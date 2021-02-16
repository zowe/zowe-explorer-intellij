package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.util.Disposer
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.dataops.dataOpsManager
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.utils.crudable.eventAdaptor
import eu.ibagroup.formainframe.utils.subscribe
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import java.awt.Component
import javax.swing.event.TreeSelectionEvent
import javax.swing.tree.DefaultMutableTreeNode


fun interface NodeListener : (Any, Boolean) -> Unit {
  override operator fun invoke(node: Any, structure: Boolean)
}

class FileExplorerContent(private val explorer: Explorer, parentDisposable: Disposable) : JBScrollPane(), DataProvider,
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
//        treeModel?.accept {
//          return@accept when (it.lastPathComponent) {
//
//          }
//        }
      }
    })
    subscribe(NODE_UPDATE, NodeListener { node, structure -> this.structure?.invalidate(node, structure) }, this)
//    subscribe(FileFetchProvider.CACHE_UPDATED, object : FileCacheListener {
//      override fun <R : Any, Q : Query<R>, File : VirtualFile> onCacheUpdated(query: Q, files: Collection<File>) {
//        structure?.invalidate()
//        //invalidator(query.request, structure = true, stopIfFound = false)
//      }
//    })
  }

  private var tree: DnDAwareTree? = null

  private var treeModel: AsyncTreeModel? = null

  private var currentlySelectedNode: ExplorerTreeNodeBase<*>? = null

  private val currentlySelectedFile: MFVirtualFile?
    get() = currentlySelectedNode?.virtualFile

  private val currentlySelectedAttributes: VFileInfoAttributes?
    get() {
      return if (currentlySelectedFile != null) {
        dataOpsManager.tryToGetAttributes(currentlySelectedFile!!)
      } else {
        null
      }
    }

  private var structure: StructureTreeModel<FileExplorerTreeStructure>? =
    StructureTreeModel(FileExplorerTreeStructure(explorer), this).also { stm ->
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
          return;
        }
        val popupActionGroup = DefaultActionGroup()
        popupActionGroup.add(
          ActionManager.getInstance().getAction("eu.ibagroup.formainframe.actions.ContextMenuGroup")
        );
        val popupMenu = ActionManager.getInstance().createActionPopupMenu(FILE_EXPLORER, popupActionGroup)
        popupMenu.component.show(comp, x, y)
      }

    })
    tree.addTreeSelectionListener { treeSelectionEvent: TreeSelectionEvent ->
      val treePath = treeSelectionEvent.path
      val lastSelectedNode = treePath.lastPathComponent as DefaultMutableTreeNode
      val descriptor = lastSelectedNode.userObject as ExplorerTreeNodeBase<*>
      currentlySelectedNode = descriptor
    }

  }

  override fun dispose() {
    tree = null
    structure = null
  }


  override fun getData(dataId: String): Any? {
    return when (dataId) {
      "currentNode" -> currentlySelectedNode
      "currentFile" -> currentlySelectedFile
      "currentAttributes" -> currentlySelectedAttributes
      else -> null
    }
  }

}

val CURRENT_NODE: DataKey<ExplorerTreeNodeBase<*>> = DataKey.create("currentNode")

val CURRENT_FILE: DataKey<MFVirtualFile> = DataKey.create("currentFile")

val CURRENT_ATTRIBUTES: DataKey<VFileInfoAttributes> = DataKey.create("currentAttributes")

val FILE_EXPLORER = "File Explorer"