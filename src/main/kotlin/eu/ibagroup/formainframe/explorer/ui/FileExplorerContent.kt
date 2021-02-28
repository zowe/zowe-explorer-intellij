package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.ide.util.treeView.PresentableNodeDescriptor
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.containers.toArray
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.common.ui.getPath
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.utils.crudable.eventAdaptor
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.utils.subscribe
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import java.awt.Component
import javax.swing.event.TreeSelectionEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeSelectionModel


fun interface NodeListener : (Any, Boolean) -> Unit {
  override operator fun invoke(node: Any, structure: Boolean)
}

class FileExplorerContent(
  private val explorer: Explorer,
  parentDisposable: Disposable, project: Project
) : JBScrollPane(),
  DataProvider,
  Disposable {

  companion object {
    @JvmStatic
    val NODE_UPDATE = Topic.create("nodeUpdate", NodeListener::class.java)
  }

  init {
    Disposer.register(parentDisposable, this)
    subscribe(ConfigService.CONFIGS_CHANGED, eventAdaptor<WorkingSetConfig> {
      onAdd { structure?.invalidate() }
      onDelete { structure?.invalidate() }
      onUpdate { _, _ ->
        structure?.invalidate()
      }
    })
    subscribe(
      componentManager = explorer.componentManager,
      topic = NODE_UPDATE,
      handler = NodeListener { node, structure -> this.structure?.invalidate(node, structure) },
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

val SELECTED_NODES: DataKey<List<NodeData>> = DataKey.create("currentNode")

val FILE_EXPLORER_PLACE = "File Explorer"