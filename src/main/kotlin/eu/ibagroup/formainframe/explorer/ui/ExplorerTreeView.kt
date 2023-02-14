package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
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
import eu.ibagroup.formainframe.common.ui.DoubleClickTreeMouseListener
import eu.ibagroup.formainframe.common.ui.makeNodeDataFromTreePath
import eu.ibagroup.formainframe.common.ui.promisePath
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
import eu.ibagroup.formainframe.utils.crudable.EntityWithUuid
import eu.ibagroup.formainframe.utils.getAncestorNodes
import eu.ibagroup.formainframe.utils.rwLocked
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.utils.subscribe
import org.jetbrains.concurrency.AsyncPromise
import java.awt.Component
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

/**
 * Explorer tree view base implementation
 * @param explorer the explorer to provide the tree view for
 * @param project the project where the view will be shown
 * @param parentDisposable the parent disposable object to register the disposer for the explorer view
 * @param contextMenu the context menu for the explorer
 * @param rootNodeProvider the root node provider for the root node of the explorer
 * @param cutProviderUpdater the cut provider updater to store the information about the cut elements
 */
abstract class ExplorerTreeView<U : WorkingSet<*>, UnitConfig : EntityWithUuid>
  (
  val explorer: Explorer<U>,
  project: Project,
  parentDisposable: Disposable,
  private val contextMenu: ActionGroup,
  rootNodeProvider: (explorer: Explorer<U>, project: Project, treeStructure: ExplorerTreeStructureBase) -> ExplorerTreeNode<*>,
  internal val cutProviderUpdater: (List<VirtualFile>) -> Unit
) : JBScrollPane(), DataProvider, Disposable {

  internal var mySelectedNodesData: List<NodeData> by rwLocked(listOf())
  internal val myFsTreeStructure: CommonExplorerTreeStructure<Explorer<U>>
  internal val myStructure: StructureTreeModel<CommonExplorerTreeStructure<Explorer<U>>>
  internal val myTree: Tree
  internal val myNodesToInvalidateOnExpand = hashSetOf<Any>()
  internal val ignoreVFileDeleteEvents = AtomicBoolean(false)

  protected val dataOpsManager = explorer.componentManager.service<DataOpsManager>()

  private var treeModel: AsyncTreeModel

  /**
   * Get node by provided query and invalidate them. The nodes will be either collapsed or invalidated on this action, basing on the provided parameters
   * @param query the query to search nodes by
   * @param [collapse] collapse the nodes if the parameter is true. False by default
   * @param [invalidate] invalidate the nodes if the parameter is true. True by default
   * @return the nodes found by the query
   */
  internal fun getNodesByQueryAndInvalidate(
    query: Query<*, *>,
    collapse: Boolean = false,
    invalidate: Boolean = true
  ): Collection<ExplorerTreeNode<*>> {
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


        private fun onAddDelete(explorer: Explorer<*>) {
          if (explorer == this@ExplorerTreeView.explorer) {
            myFsTreeStructure.findByValue(explorer).forEach {
              myStructure.invalidate(it, true)
            }
          }
        }


        override fun onAdded(explorer: Explorer<*>, unit: ExplorerUnit) {
          onAddDelete(explorer)
        }

        override fun onChanged(explorer: Explorer<*>, unit: ExplorerUnit) {
          if (explorer == this@ExplorerTreeView.explorer) {
            myFsTreeStructure.findByValue(unit).forEach {
              myStructure.invalidate(it, true)
            }
          }
        }

        override fun onDeleted(explorer: Explorer<*>, unit: ExplorerUnit) {
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
          events
            .mapNotNull {
              val nodes = myFsTreeStructure.findByVirtualFile(it.file ?: return@mapNotNull null)
              when {
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
          explorer.reportThrowable(throwable, project)
        }
      },
      disposable = this
    )
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

  /**
   * Register the tree events listeners. These are both mouse listeners, and the other tree listeners
   * @param tree the tree where the listeners will be registered
   */
  private fun registerTreeListeners(tree: DnDAwareTree) {
    val contextMenuPlace: String = when (this) {
      is FileExplorerView -> FILE_EXPLORER_CONTEXT_MENU
      is JesExplorerView -> JES_EXPLORER_CONTEXT_MENU
      else -> {
        "Unrecognized"
      }
    }

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
      mySelectedNodesData = tree.selectionPaths?.map { makeNodeDataFromTreePath(explorer, it) } ?: listOf()
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


}
