package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.sendTopic

class RefreshNode : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {

    val node = e.getData(CURRENT_NODE)

    if (node is FileCacheNode<*,*,*,*,*>) {
      node.cleanCache()
    } else if (node is WorkingSetNode) {
      node.children.filterIsInstance<DSMaskNode>().forEach {
        it.cleanCache()
      }
    }

    if (node != null) {
      sendTopic(FileExplorerContent.NODE_UPDATE)(node, true)
    }


  }

  override fun update(e: AnActionEvent) {
    val node = e.getData(CURRENT_NODE)
    e.presentation.isVisible = node is WorkingSetNode || node is DSMaskNode || node is UssDirNode || node is LibraryNode
  }

}