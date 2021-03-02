package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.EditSourceOnDoubleClickHandler
import eu.ibagroup.formainframe.common.ui.findCommonParentPath
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.dataops.fetch.FileCacheListener
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.ExplorerListener
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.utils.subscribe
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import java.awt.Component
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeSelectionModel

class FileExplorerContent(
  private val explorer: Explorer,
  parentDisposable: Disposable, project: Project
) : JBScrollPane(),
  DataProvider,
  Disposable {

  init {
    Disposer.register(parentDisposable, this)
    subscribe(
      componentManager = explorer.componentManager,
      topic = Explorer.UNITS_CHANGED,
      handler = object : ExplorerListener {
        override fun onAdded(explorer: Explorer, unit: ExplorerUnit) {
          onAddDelete(explorer)
        }

        private fun onAddDelete(explorer: Explorer) {
          if (explorer == this@FileExplorerContent.explorer) {
            fsTreeStructure?.findByValue(explorer)?.forEach {
              structure?.invalidate(it, true)
            }
          }
        }

        override fun onChanged(explorer: Explorer, unit: ExplorerUnit) {
          if (explorer == this@FileExplorerContent.explorer) {
            fsTreeStructure?.findByValue(unit)?.forEach {
              structure?.invalidate(it, true)
            }
          }
        }

        override fun onDeleted(explorer: Explorer, unit: ExplorerUnit) {
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
          events.mapNotNull {
            val nodes = fsTreeStructure?.findByVirtualFile(it.file ?: return@mapNotNull null)
            if (it is VFileContentChangeEvent || it is VFilePropertyChangeEvent) {
              nodes
            } else {
              nodes?.mapNotNull { n -> n.parent }
            }
          }.flatten().forEach { fileNode ->
            fileNode.cleanCacheIfPossible()
            structure?.invalidate(fileNode, true)
          }
        }
      },
      disposable = this
    )
    subscribe(
      componentManager = explorer.componentManager,
      topic = FileFetchProvider.CACHE_UPDATED,
      handler = object : FileCacheListener {
        override fun <R : Any, Q : Query<R>, File : VirtualFile> onCacheUpdated(query: Q, files: Collection<File>) {
          fsTreeStructure?.findByPredicate {
            if (it is FileCacheNode<*, *, *, *, *>) {
              it.query == query
            } else false
          }?.forEach {
            structure?.invalidate(it, true)
          }
        }
      },
      disposable = this
    )
  }

  private var tree: DnDAwareTree? = null

  private var treeModel: AsyncTreeModel? = null

  private var selectedNodesData: List<NodeData>? = null

  private var fsTreeStructure: FileExplorerTreeStructure? = null

  private var structure: StructureTreeModel<FileExplorerTreeStructure>? =
    StructureTreeModel(
      FileExplorerTreeStructure(explorer, project).also { fsTreeStructure = it },
      this
    ).also { stm ->
      treeModel = AsyncTreeModel(stm, false, this).also {
        tree = DnDAwareTree(it).apply { isRootVisible = false }.also { t ->
          setViewportView(t)
          registerTreeListeners(t, stm)
        }
      }
    }

  private fun registerTreeListeners(tree: Tree, treeModel: StructureTreeModel<*>) {
    tree.addMouseListener(object : PopupHandler() {
      override fun invokePopup(comp: Component, x: Int, y: Int) {
        if (tree.getRowForLocation(x, y) == -1) {
          return
        }
        val popupActionGroup = DefaultActionGroup()
        popupActionGroup.add(
          ActionManager.getInstance().getAction("eu.ibagroup.formainframe.actions.ContextMenuGroup")
        )
        val popupMenu = ActionManager.getInstance().createActionPopupMenu(FILE_EXPLORER_PLACE, popupActionGroup)
        popupMenu.component.show(comp, x, y)
      }
    })
    tree.selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
    tree.addTreeSelectionListener {
      selectedNodesData = tree.selectionPaths?.map {
        val descriptor = (it.lastPathComponent as DefaultMutableTreeNode).userObject as ExplorerTreeNodeBase<*>
        val file = descriptor.virtualFile
        val attributes = if (file != null) {
          service<DataOpsManager>(explorer.componentManager).tryToGetAttributes(file)
        } else null
        NodeData(descriptor, file, attributes)
      }
    }

    EditSourceOnDoubleClickHandler.TreeMouseListener(tree).installOn(tree)

  }

  override fun dispose() {
    tree = null
    structure = null
    treeModel = null
    fsTreeStructure = null
  }


  override fun getData(dataId: String): Any? {
    return when (dataId) {
      "currentNode" -> selectedNodesData
      CommonDataKeys.NAVIGATABLE_ARRAY.name -> selectedNodesData?.map { it.node }?.toTypedArray()
      else -> null
    }
  }

}

data class NodeData(
  val node: ExplorerTreeNodeBase<*>,
  val file: MFVirtualFile?,
  val attributes: VFileInfoAttributes?
)

fun Collection<NodeData>.filterNodesBeneath(): Collection<NodeData> {
  val withPathLength = map { Pair(it, it.node.path.pathCount) }
  return withPathLength.filter { pair ->
    withPathLength
      .filter { pair.second > it.second }
      .none { it.first.node.path.findCommonParentPath(pair.first.node.path) != pair.first.node.path }
  }.map { it.first }
}

val SELECTED_NODES: DataKey<List<NodeData>> = DataKey.create("currentNode")

val FILE_EXPLORER_PLACE = "File Explorer"