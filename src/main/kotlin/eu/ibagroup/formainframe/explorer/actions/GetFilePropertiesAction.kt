package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.service

class GetFilePropertiesAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return
    val node = view.mySelectedNodesData.getOrNull(0)?.node
    if (node is ExplorerTreeNode<*>) {
      val virtualFile = node.virtualFile
      if (virtualFile != null) {
        val dataOpsManager = node.explorer.componentManager.service<DataOpsManager>()
        when (val attributes = dataOpsManager.tryToGetAttributes(virtualFile)?.clone()) {
          is RemoteDatasetAttributes -> {
            val dialog = DatasetPropertiesDialog(e.project, DatasetState(attributes))
            dialog.showAndGet()
          }
          is RemoteUssAttributes -> {
            val dialog = UssFilePropertiesDialog(e.project, UssFileState(attributes))
            dialog.showAndGet()
          }
          is RemoteMemberAttributes -> {
            val dialog = MemberPropertiesDialog(e.project, MemberState(attributes))
            dialog.showAndGet()
          }
        }
      }
    }

  }

  override fun isDumbAware(): Boolean {
    return true
  }

  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val node = selected.getOrNull(0)?.node
    e.presentation.isVisible = selected.size == 1
      && (node is UssFileNode
      || node is FileLikeDatasetNode
      || node is LibraryNode
      || node is UssDirNode)
  }
}