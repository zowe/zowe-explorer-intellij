package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
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
import eu.ibagroup.formainframe.common.ui.DoubleClickTreeMouseListener
import eu.ibagroup.formainframe.common.ui.makeNodeDataFromTreePath
import eu.ibagroup.formainframe.common.ui.promisePath
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.attributesListener
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.dataops.content.synchronizer.SaveStrategy
import eu.ibagroup.formainframe.dataops.fetch.FileCacheListener
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import eu.ibagroup.formainframe.explorer.*
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.EntityWithUuid
import eu.ibagroup.formainframe.vfs.MFBulkFileListener
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem
import org.jetbrains.concurrency.AsyncPromise
import java.awt.Component
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

val EXPLORER_VIEW = DataKey.create<ExplorerTreeView<*, *, *>>("explorerView")

fun <ExplorerView: ExplorerTreeView<*, *, *>> AnActionEvent.getExplorerView(clazz: Class<out ExplorerView>): ExplorerView? {
  return getData(EXPLORER_VIEW).castOrNull(clazz)
}

inline fun <reified ExplorerView: ExplorerTreeView<*, *, *>> AnActionEvent.getExplorerView(): ExplorerView? {
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
abstract class ExplorerTreeView<Connection: ConnectionConfigBase, U : WorkingSet<Connection, *>, UnitConfig : EntityWithUuid>
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
   * Get node by provided query and invalidate them. The nodes will be either collapsed or invalidated on this action, basing on the provided parameters
   * @param query the query to search nodes by
   * @param [collapse] collapse the nodes if the parameter is true. False by default
   * @param [invalidate] invalidate the nodes if the parameter is true. True by default
   * @return the nodes found by the query
   */
  fun getNodesByQueryAndInvalidate(
    query: Query<*, *>,
    collapse: Boolean = false,
    invalidate: Boolean = true
  ): Collection<ExplorerTreeNode<*, *>> {
    return myFsTreeStructure
      .findByPredicate {
        if (it is FetchNode) {
          it.query == query
        } else false
      }
      .onEach { foundNode ->

        fun collapseIfNeeded(tp: TreePath) {
          if (collapse) {
            treeModel.onValidThread {
              myTree.collapsePath(tp)
              synchronized(myNodesToInvalidateOnExpand) {
                val node = tp.lastPathComponent
                myNodesToInvalidateOnExpand.add(node)
              }
            }
          }
        }

        myStructure
          .promisePath(foundNode, myTree)
          .thenAsync { nodePath ->
            if (myTree.isVisible(nodePath) && myTree.isCollapsed(nodePath) && mySelectedNodesData.any { it.node == nodePath }) {
              myTree.expandPath(nodePath)
            }
            if (invalidate) {
              myStructure
                .invalidate(foundNode, true)
                .onSuccess { tp ->
                  collapseIfNeeded(tp)
                }
            } else {
              collapseIfNeeded(nodePath)
              AsyncPromise<TreePath>()
            }
          }
      }
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


        private fun <Connection: ConnectionConfigBase> onAddDelete(explorer: Explorer<Connection, *>) {
          if (explorer == this@ExplorerTreeView.explorer) {
            myFsTreeStructure.findByValue(explorer).forEach {
              myStructure.invalidate(it, true)
            }
          }
        }

        override fun <Connection: ConnectionConfigBase> onAdded(explorer: Explorer<Connection, *>,unit: ExplorerUnit<Connection>) {
          onAddDelete(explorer)
        }


        override fun <Connection: ConnectionConfigBase> onChanged(explorer: Explorer<Connection, *>, unit: ExplorerUnit<Connection>) {
          if (explorer == this@ExplorerTreeView.explorer) {
            myFsTreeStructure.findByValue(unit).forEach {
              myStructure.invalidate(it, true)
            }
          }
        }

        override fun <Connection: ConnectionConfigBase> onDeleted(explorer: Explorer<Connection, *>, unit: ExplorerUnit<Connection>) {
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

                it is VFileContentChangeEvent || it is VFilePropertyChangeEvent -> {
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
          getNodesByQueryAndInvalidate(query)
        }

        override fun <R : Any, Q : Query<R, Unit>> onCacheCleaned(query: Q) {
          getNodesByQueryAndInvalidate(query)
        }

        override fun <R : Any, Q : Query<R, Unit>> onFetchCancelled(query: Q) {
          getNodesByQueryAndInvalidate(query, collapse = true, invalidate = false)
        }

        override fun <R : Any, Q : Query<R, Unit>> onFetchFailure(query: Q, throwable: Throwable) {
          getNodesByQueryAndInvalidate(query)
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
      mySelectedNodesData = tree.selectionPaths?.mapNotNull { makeNodeDataFromTreePath<Connection>(explorer, it) } ?: listOf()
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
