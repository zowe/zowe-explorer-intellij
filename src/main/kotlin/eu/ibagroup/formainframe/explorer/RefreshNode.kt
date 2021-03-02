package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.explorer.ui.*

class RefreshNode : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {

    val selected = e.getData(SELECTED_NODES) ?: return

    selected.parallelStream().forEach { data ->
      val node = data.node
      if (node is FileCacheNode<*, *, *, *, *>) {
        node.cleanCache()
      } else if (node is WorkingSetNode) {
        node.children.filterIsInstance<DSMaskNode>().forEach {
          it.cleanCache()
        }
      }
    }

  }

  override fun update(e: AnActionEvent) {
    val selected = e.getData(SELECTED_NODES)
    e.presentation.isEnabledAndVisible = selected?.any {
      val node = it.node
      node is DSMaskNode || node is UssDirNode || node is LibraryNode
    } == true
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}