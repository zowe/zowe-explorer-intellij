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
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.common.ui.getPath
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

class FileExplorerContent(explorer: Explorer, parentDisposable: Disposable, project: Project) : JBScrollPane(),
  DataProvider,
  Disposable {

  companion object {
    @JvmStatic
    val NODE_UPDATE = Topic.create("nodeUpdate", NodeListener::class.java)
  }

//  private fun invalidateRoot(str: Boolean) {
//    val s = structure
//    val root = fsTreeStructure?.rootElement
//    if (s != null && root != null) {
//      invalidateAndExpand(root, str)
//    }
//  }
//
//  private fun <T : Any> findByPredicate(
//    str: Boolean,
//    clazz: Class<out T>,
//    rootNode: ExplorerTreeNodeBase<*>? = null,
//    predicate: (T) -> Boolean
//  ): ExplorerTreeNodeBase<T>? {
//    val s = structure
//    val root = rootNode ?: fsTreeStructure?.rootElement
//
//    @Suppress("UNCHECKED_CAST")
//    fun <E : Any> findByPredicateRecursively(
//      structure: StructureTreeModel<*>,
//      startNode: Any?,
//      str: Boolean,
//      predicate: (E) -> Boolean
//    ): ExplorerTreeNodeBase<E>? {
//      if (startNode is ExplorerTreeNodeBase<*>) {
//        val nodeValue = startNode.value
//        if (clazz.isAssignableFrom(nodeValue::class.java) && predicate(nodeValue as E)) {
//          return startNode as ExplorerTreeNodeBase<E>
//        }
//      }
//      return structure.getChildren(startNode).stream().mapNotNull {
//        findByPredicateRecursively(structure, it as ExplorerTreeNodeBase<*>, str, predicate)
//      }.findAnyNullable()
//    }
//    return if (s != null) {
//      findByPredicateRecursively(s, root, str, predicate)
//    } else null
//  }
//
//  private inline fun <reified T : Any> findByPredicate(
//    str: Boolean,
//    rootNode: ExplorerTreeNodeBase<*>? = null,
//    noinline predicate: (T) -> Boolean
//  ): ExplorerTreeNodeBase<T>? {
//    return findByPredicate(str, T::class.java, rootNode, predicate)
//  }

  private fun invalidateAndExpand(
    node: Any,
    str: Boolean,
  ) {
    val s = structure
    val t = tree
    val tm = treeModel
    val ts = fsTreeStructure
    if (s != null && t != null && tm != null && ts != null) {
      val oldChildren = ts.getChildElements(node).filterIsInstance<PresentableNodeDescriptor<*>>()
      val oldExpanded = oldChildren
        .mapNotNull { tm.getPath(it) }
        .filter { t.isExpanded(it) }
      s.invalidate(node, str).onSuccess { path ->
        val newChildren = ts.getChildElements(path.lastPathComponent).filterIsInstance<PresentableNodeDescriptor<*>>()
        oldExpanded
          .filter { newChildren.contains(it.lastPathComponent) }
          .map { tm.getPath(it.lastPathComponent) }
          .forEach { t.expandPath(it) }
      }
    }
  }

  init {
    Disposer.register(parentDisposable, this)
    subscribe(ConfigService.CONFIGS_CHANGED, eventAdaptor<WorkingSetConfig> {
      onAdd { structure?.invalidate() }
      onDelete { structure?.invalidate() }
      onUpdate { oldWsConfig, newWsConfig ->
        structure?.invalidate()
//        if (oldWsConfig.name != newWsConfig.name) {
//          invalidateRoot(true)
//        }
//        val wsNode = findByPredicate<WorkingSet>(true) {
//          it.name == newWsConfig.name
//        } ?: return@onUpdate
//        val mergedDsMasks = Crudable.mergeCollections(oldWsConfig.dsMasks, newWsConfig.dsMasks)
//        val mergedUssPath = Crudable.mergeCollections(oldWsConfig.ussPaths, newWsConfig.ussPaths)
//        if (mergedDsMasks.toAdd.isNotEmpty()
//          || mergedDsMasks.toDelete.isNotEmpty()
//          || mergedUssPath.toAdd.isNotEmpty()
//          || mergedUssPath.toDelete.isNotEmpty()
//        ) {
//          invalidateAndExpand(wsNode, false)
//        }
//        mergedDsMasks.toUpdate.mapNotNull { dsMask ->
//          findByPredicate<DSMask>(true, wsNode) { it.mask == dsMask.mask }
//        }.forEach { invalidateAndExpand(it, true) }
//        mergedUssPath.toUpdate.mapNotNull { ussPath ->
//          findByPredicate<UssPath>(true, wsNode) { it.path == ussPath.path }
//        }.forEach { invalidateAndExpand(it, true) }
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
    tree.addTreeSelectionListener { treeSelectionEvent: TreeSelectionEvent ->
      val treePath = treeSelectionEvent.path
      val lastSelectedNode = treePath.lastPathComponent as DefaultMutableTreeNode
      val descriptor = lastSelectedNode.userObject as ExplorerTreeNodeBase<*>
      currentlySelectedNode = descriptor
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
      "currentNode" -> currentlySelectedNode
      "currentFile" -> currentlySelectedFile
      "currentAttributes" -> currentlySelectedAttributes
      CommonDataKeys.NAVIGATABLE_ARRAY.name -> arrayOf(currentlySelectedNode)
      else -> null
    }
  }

}

val CURRENT_NODE: DataKey<ExplorerTreeNodeBase<*>> = DataKey.create("currentNode")

val CURRENT_FILE: DataKey<MFVirtualFile> = DataKey.create("currentFile")

val CURRENT_ATTRIBUTES: DataKey<VFileInfoAttributes> = DataKey.create("currentAttributes")

val FILE_EXPLORER_PLACE = "File Explorer"