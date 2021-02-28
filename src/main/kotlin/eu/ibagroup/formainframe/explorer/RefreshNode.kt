package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.sendTopic

class RefreshNode : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {

    val selected = e.getData(SELECTED_NODES) ?: return

    for (data in selected) {
      val node = data.node
      var sendTopic = false
      if (node is FileCacheNode<*, *, *, *, *>) {
        node.cleanCache()
        sendTopic = true
      } else if (node is WorkingSetNode) {
        node.children.filterIsInstance<DSMaskNode>().forEach {
          it.cleanCache()
        }
        sendTopic = true
      }
      if (sendTopic) sendTopic(FileExplorerContent.NODE_UPDATE)(node, true)
    }

  }

  override fun update(e: AnActionEvent) {
    val selected = e.getData(SELECTED_NODES)
    e.presentation.isVisible = selected?.any {
      val node = it.node
      node is WorkingSetNode || node is DSMaskNode || node is UssDirNode || node is LibraryNode
    } == true
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}