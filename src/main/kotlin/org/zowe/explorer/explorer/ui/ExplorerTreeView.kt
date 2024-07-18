package org.zowe.explorer.explorer.ui

import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.EditorHighlighterUpdater
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.FileContentUtilCore
import org.zowe.explorer.common.ui.DoubleClickTreeMouseListener
import org.zowe.explorer.common.ui.makeNodeDataFromTreePath
import org.zowe.explorer.common.ui.promisePath
import org.zowe.explorer.config.connect.ConnectionConfigBase
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Query
import org.zowe.explorer.dataops.attributes.AttributesService
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.attributes.attributesListener
import org.zowe.explorer.dataops.content.synchronizer.DocumentedSyncProvider
import org.zowe.explorer.dataops.content.synchronizer.SaveStrategy
import org.zowe.explorer.dataops.fetch.FileCacheListener
import org.zowe.explorer.dataops.fetch.FileFetchProvider
import org.zowe.explorer.explorer.CutBufferListener
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.ExplorerListener
import org.zowe.explorer.explorer.ExplorerUnit
import org.zowe.explorer.explorer.UNITS_CHANGED
import org.zowe.explorer.explorer.WorkingSet
import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.utils.crudable.EntityWithUuid
import org.zowe.explorer.utils.getAncestorNodes
import org.zowe.explorer.utils.runWriteActionInEdtAndWait
import org.zowe.explorer.utils.rwLocked
import org.zowe.explorer.utils.subscribe
import org.zowe.explorer.vfs.MFBulkFileListener
import org.zowe.explorer.vfs.MFVFilePropertyChangeEvent
import org.zowe.explorer.vfs.MFVirtualFileSystem
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.collectResults
import java.awt.Component
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.TreeSelectionModel

val EXPLORER_VIEW = DataKey.create<ExplorerTreeView<*, *, *>>("explorerView")

fun <ExplorerView : ExplorerTreeView<*, *, *>> AnActionEvent.getExplorerView(clazz: Class<out ExplorerView>): ExplorerView? {
  return getData(EXPLORER_VIEW).castOrNull(clazz)
}

inline fun <reified ExplorerView : ExplorerTreeView<*, *, *>> AnActionEvent.getExplorerView(): ExplorerView? {
  return getExplorerView(ExplorerView::class.java)
}

/**
 * Explorer tree view base implementation
 * @param explorer the explorer to provide the tree view for
 * @param project the project where the view will be shown
 * @param parentDisposable the parent disposable object to register the disposer for the explorer view
 * @param contextMenu the context menu for the explorer
 * @param rootNodeProvider the root node provider for the root node of the explorer
 * @param cutProviderUpdater the cut provider updater to store the information about the cut elements
 */
abstract class ExplorerTreeView<Connection : ConnectionConfigBase, U : WorkingSet<Connection, *>, UnitConfig : EntityWithUuid>
  (
  val explorer: Explorer<Connection, U>,
  private val project: Project,
  parentDisposable: Disposable,
  private val contextMenu: ActionGroup,
  rootNodeProvider: (explorer: Explorer<Connection, U>, project: Project, treeStructure: ExplorerTreeStructureBase) -> ExplorerTreeNode<Connection, *>,
  internal val cutProviderUpdater: (List<VirtualFile>) -> Unit
) : JBScrollPane(), DataProvider, Disposable {

  var mySelectedNodesData: List<NodeData<Connection>> by rwLocked(listOf())
  internal val myFsTreeStructure: CommonExplorerTreeStructure<Explorer<Connection, U>>
  internal val myStructure: StructureTreeModel<CommonExplorerTreeStructure<Explorer<Connection, U>>>
  val myTree: Tree
  internal val myNodesToInvalidateOnExpand = hashSetOf<Any>()
  internal val ignoreVFileDeleteEvents = AtomicBoolean(false)
  internal val ignoreVFSChangeEvents = AtomicBoolean(false)

  protected val dataOpsManager = explorer.componentManager.service<DataOpsManager>()

  private var treeModel: AsyncTreeModel

  /**
   * Get nodes by the provided query
   * @param query the query to search nodes by
   * @return collection of the nodes found by the provided query
   */
  fun getNodesByQuery(query: Query<*, *>): Collection<ExplorerTreeNode<*, *>> {
    return myFsTreeStructure
      .findByPredicate {
        it is FetchNode && it.query == query
      }
  }

  /**
   * Invalidate the provided nodes. It will expand a selected node's path if it is collapsed
   * @param nodes the nodes to invalidate
   * @return a promise with the list of the invalidated nodes
   */
  fun invalidateNodes(nodes: Collection<ExplorerTreeNode<*, *>>): Promise<List<ExplorerTreeNode<*, *>>> {
    return nodes
      .map { foundNode ->
        myStructure
          .promisePath(foundNode, myTree)
          .thenAsync { nodePath ->
            if (myTree.isVisible(nodePath) && myTree.isCollapsed(nodePath) && mySelectedNodesData.any { it.node == nodePath }) {
              myTree.expandPath(nodePath)
            }
            myStructure.invalidate(foundNode, true)
          }
          .then { foundNode }
      }
      .collectResults()
  }

  /**
   * Collapse the provided nodes
   * @param nodes the nodes to collapse
   * @return a promise with the list of the collapsed nodes
   */
  fun collapseNodes(nodes: Collection<ExplorerTreeNode<*, *>>): Promise<List<ExplorerTreeNode<*, *>>> {
    return nodes
      .map { foundNode ->
        myStructure
          .promisePath(foundNode, myTree)
          .then { nodePath ->
            treeModel.onValidThread {
              myTree.collapsePath(nodePath)
              synchronized(myNodesToInvalidateOnExpand) {
                val node = nodePath.lastPathComponent
                myNodesToInvalidateOnExpand.add(node)
              }
            }
            foundNode
          }
      }
      .collectResults()
  }

  /**
   * Register the view disposer, subscribe on UNITS_CHANGED, VFS_CHANGES, CUT_BUFFER_CHANGES, CACHE_CHANGES and FILE_CONTENT_CHANGED topics
   */
  init {
    Disposer.register(parentDisposable, this)
    myStructure = StructureTreeModel(
      CommonExplorerTreeStructure(explorer, project, rootNodeProvider).also { myFsTreeStructure = it },
      { o1, o2 ->
        if (o1 is FilesWorkingSetNode && o2 is FilesWorkingSetNode) {
          o1.unit.name.compareTo(o2.unit.name)
        } else {
          0
        }
      },
      this
    ).also { stm ->
      treeModel = AsyncTreeModel(stm, false, this).also {
        myTree = DnDAwareTree(it).apply { isRootVisible = false }.also { t ->
          setViewportView(t)
          registerTreeListeners(t)
        }
        myTree.putClientProperty(AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true)
      }
    }
    subscribe(
      componentManager = explorer.componentManager,
      topic = UNITS_CHANGED,
      handler = object : ExplorerListener {


        private fun <Connection : ConnectionConfigBase> onAddDelete(explorer: Explorer<Connection, *>) {
          if (explorer == this@ExplorerTreeView.explorer) {
            myFsTreeStructure.findByValue(explorer).forEach {
              myStructure.invalidate(it, true)
            }
          }
        }

        override fun <Connection : ConnectionConfigBase> onAdded(
          explorer: Explorer<Connection, *>,
          unit: ExplorerUnit<Connection>
        ) {
          onAddDelete(explorer)
        }


        override fun <Connection : ConnectionConfigBase> onChanged(
          explorer: Explorer<Connection, *>,
          unit: ExplorerUnit<Connection>
        ) {
          if (explorer == this@ExplorerTreeView.explorer) {
            myFsTreeStructure.findByValue(unit).forEach {
              myStructure.invalidate(it, true)
            }
          }
        }

        override fun <Connection : ConnectionConfigBase> onDeleted(
          explorer: Explorer<Connection, *>,
          unit: ExplorerUnit<Connection>
        ) {
          onAddDelete(explorer)
        }
      },
      disposable = this
    )
    subscribe(
      componentManager = ApplicationManager.getApplication(),
      topic = VirtualFileManager.VFS_CHANGES,
      handler = object : BulkFileListener {
        override fun after(events: MutableList<out VFileEvent>) {
          events.forEach {
            if (
              it is VFilePropertyChangeEvent &&
              VirtualFile.PROP_NAME == it.propertyName &&
              (FileContentUtilCore.FORCE_RELOAD_REQUESTOR == it.requestor || it.oldValue != it.newValue)
            ) {
              val editorEx =
                EditorUtil.getEditorEx(FileEditorManager.getInstance(project).getSelectedEditor(it.file))
              editorEx?.let { editor ->
                val editorHighlighterUpdater =
                  EditorHighlighterUpdater(project, parentDisposable, editor, it.file)
                editorHighlighterUpdater.updateHighlighters()
              }
            }
          }
        }
      },
      disposable = this
    )
    subscribe(
      componentManager = ApplicationManager.getApplication(),
      topic = MFVirtualFileSystem.MF_VFS_CHANGES_TOPIC,
      handler = object : MFBulkFileListener {
        override fun before(events: List<VFileEvent>) {
          // listens for virtual file delete events and
          // closes files opened in editor if file to be deleted is an ancestor of these files
          events.filterIsInstance<VFileDeleteEvent>().forEach {
            closeChildrenInEditor(it.file)
          }
        }

        override fun after(events: List<VFileEvent>) {
          events
            .mapNotNull {
              val nodes = myFsTreeStructure.findByVirtualFile(it.file ?: return@mapNotNull null)
              when {
                this@ExplorerTreeView
                  .ignoreVFSChangeEvents
                  .compareAndSet(true, true) -> {
                  null
                }

                it is VFileContentChangeEvent || it is VFilePropertyChangeEvent || it is MFVFilePropertyChangeEvent -> {
                  nodes
                }

                it is VFileDeleteEvent &&
                  this@ExplorerTreeView
                    .ignoreVFileDeleteEvents
                    .compareAndSet(true, true) -> {
                  null
                }

                else -> {
                  nodes.mapNotNull { n -> n.parent }
                }
              }
            }
            .flatten()
            .forEach { fileNode ->
              fileNode.cleanCacheIfPossible(cleanBatchedQuery = false)
              myStructure.invalidate(fileNode, true)
            }
        }
      },
      disposable = this
    )
    subscribe(
      componentManager = explorer.componentManager,
      topic = CutBufferListener.CUT_BUFFER_CHANGES,
      handler = CutBufferListener { previousBufferState, currentBufferState ->
        previousBufferState
          .asSequence()
          .plus(currentBufferState)
          .distinct()
          .map { it.getAncestorNodes() }
          .flatten()
          .distinct()
          .map {
            myFsTreeStructure.findByVirtualFile(it).reversed()
          }.flatten()
          .distinct()
          .forEach {
            myStructure.invalidate(it, true)
          }
      }
    )
    subscribe(
      componentManager = explorer.componentManager,
      topic = FileFetchProvider.CACHE_CHANGES,
      handler = object : FileCacheListener {

        override fun <R : Any, Q : Query<R, Unit>, File : VirtualFile> onCacheUpdated(
          query: Q,
          files: Collection<File>
        ) {
          val nodes = getNodesByQuery(query)
          invalidateNodes(nodes)
        }

        override fun <R : Any, Q : Query<R, Unit>> onCacheCleaned(query: Q) {
          val nodes = getNodesByQuery(query)
          invalidateNodes(nodes)
        }

        override fun <R : Any, Q : Query<R, Unit>> onFetchCancelled(query: Q) {
          val nodes = getNodesByQuery(query)
          collapseNodes(nodes)
        }

        override fun <R : Any, Q : Query<R, Unit>> onFetchFailure(query: Q, throwable: Throwable) {
          val nodes = getNodesByQuery(query)
          invalidateNodes(nodes)
          //The ability to show exceptions for JES Explorer has been disabled.
          //All messages about exceptions that occur in TreeView components will be displayed using File Explorer.
          //This was done to avoid duplication of exception messages, since both explorers have a common EventBus and,
          // accordingly, both receive a message about an exception that occurred in one of them.
          if (this@ExplorerTreeView is FileExplorerView) {
            explorer.reportThrowable(throwable, project)
          }
        }
      },
      disposable = this
    )
    //TODO: is the functionality needed?
    subscribe(
      componentManager = dataOpsManager.componentManager,
      topic = AttributesService.FILE_CONTENT_CHANGED,
      handler = attributesListener<FileAttributes, VirtualFile> {
        onUpdate { _, _, file ->
          val syncProvider = DocumentedSyncProvider(file, SaveStrategy.default(project))
          dataOpsManager.getContentSynchronizer(file)?.synchronizeWithRemote(syncProvider, progressIndicator = null)
        }
      }
    )
  }

  abstract val contextMenuPlace: String

  /**
   * Register the tree events listeners. These are both mouse listeners, and the other tree listeners
   * @param tree the tree where listeners will be registered
   */
  private fun registerTreeListeners(tree: DnDAwareTree) {

    tree.addMouseListener(object : PopupHandler() {
      override fun invokePopup(comp: Component, x: Int, y: Int) {
        val popupActionGroup = DefaultActionGroup()
        popupActionGroup.add(
          contextMenu
        )
        val popupMenu = ActionManager.getInstance().createActionPopupMenu(contextMenuPlace, popupActionGroup)
        popupMenu.component.show(comp, x, y)
      }
    })

    tree.selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
    tree.addTreeSelectionListener {
      mySelectedNodesData =
        tree.selectionPaths?.mapNotNull { makeNodeDataFromTreePath<Connection>(explorer, it) } ?: listOf()
    }

    tree.addTreeWillExpandListener(object : TreeWillExpandListener {
      override fun treeWillExpand(event: TreeExpansionEvent) {
        val node = event.path.lastPathComponent
        if (myNodesToInvalidateOnExpand.contains(node)) {
          synchronized(myNodesToInvalidateOnExpand) {
            if (myNodesToInvalidateOnExpand.contains(node)) {
              myNodesToInvalidateOnExpand.remove(node)
              myStructure.invalidate(event.path, true)
            }
          }
        }
      }

      override fun treeWillCollapse(event: TreeExpansionEvent) {
      }
    })

    DoubleClickTreeMouseListener(tree) {
      if (isExpanded(it)) {
        collapsePath(it)
      } else {
        expandPath(it)
      }
    }.installOn(tree)

  }

  /** Close files opened in editor if selected file is an ancestor of these files */
  fun closeChildrenInEditor(selectedFile: VirtualFile) {
    val fileEditorManager = FileEditorManager.getInstance(project)
    val openFiles = fileEditorManager.openFiles
    openFiles.forEach { openFile ->
      if (VfsUtilCore.isAncestor(selectedFile, openFile, false)) {
        val contentSynchronizer = service<DataOpsManager>().getContentSynchronizer(openFile)
        val syncProvider = DocumentedSyncProvider(openFile)
        contentSynchronizer?.markAsNotNeededForSync(syncProvider)
        runWriteActionInEdtAndWait {
          fileEditorManager.closeFile(openFile)
        }
      }
    }
  }

}
