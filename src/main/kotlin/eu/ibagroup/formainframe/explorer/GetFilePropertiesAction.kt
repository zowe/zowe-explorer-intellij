package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.dataOpsManager
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.r2z.UssFile

class GetFilePropertiesAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val node = e.getData(CURRENT_NODE)
    if (node is ExplorerTreeNodeBase<*>) {
      val virtualFile = node.virtualFile
      if (virtualFile != null) {
        val attributes = dataOpsManager.tryToGetAttributes(virtualFile)?.clone()
        when (attributes) {
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
    val node = e.getData(CURRENT_NODE)
    e.presentation.isVisible = node is UssFileNode
        || node is FileLikeDatasetNode
        || node is LibraryNode
        || node is UssDirNode
  }
}