package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.common.ui.promisePath
import eu.ibagroup.formainframe.explorer.ui.*

class RefreshNodeAction : AnAction() {

  private fun cleanInvalidateOnExpand(
    node: ExplorerTreeNode<*>,
    view: GlobalFileExplorerView
  ) {
    view.myStructure.promisePath(node, view.myTree).onSuccess {
      val lastNode = it.lastPathComponent
      if (view.myNodesToInvalidateOnExpand.contains(lastNode)) {
        synchronized(view.myNodesToInvalidateOnExpand) {
          view.myNodesToInvalidateOnExpand.remove(lastNode)
        }
      }
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return

    val selected = view.mySelectedNodesData

    selected.parallelStream().forEach { data ->
      val node = data.node
      if (node is FetchNode) {
        cleanInvalidateOnExpand(node, view)
        node.cleanCache()
        val query = node.query ?: return@forEach
        view.getNodesByQueryAndInvalidate(query)
      } else if (node is WorkingSetNode) {
        node.cachedChildren.filterIsInstance<FetchNode>()
          .forEach {
          it.cleanCache()
          cleanInvalidateOnExpand(it, view)
        }
        view.myFsTreeStructure.findByValue(node.value).forEach {
          view.myStructure.invalidate(it, true)
        }
      }
    }

  }

  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    e.presentation.isEnabledAndVisible = selected.any {
      it.node is RefreshableNode
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}