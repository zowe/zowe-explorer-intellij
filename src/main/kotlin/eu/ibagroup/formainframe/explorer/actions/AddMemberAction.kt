package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.ui.Messages
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.dataops.operations.MemberAllocationOperation
import eu.ibagroup.formainframe.dataops.operations.MemberAllocationParams
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile

class AddMemberAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return
    val currentNode = view.mySelectedNodesData[0].node
    if (currentNode is ExplorerUnitTreeNodeBase<*, *>
      && currentNode.unit is WorkingSet
      && currentNode is FetchNode
    ) {
      val connectionConfig = currentNode.unit.connectionConfig
      val connectionUrl = currentNode.unit.urlConnection
      val dataOpsManager = currentNode.explorer.componentManager.service<DataOpsManager>()
      if (currentNode is LibraryNode && connectionConfig != null && connectionUrl != null) {
        val parentName = dataOpsManager
          .getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
          .getAttributes(currentNode.virtualFile)
          ?.name
        if (parentName != null) {
          val dialog = AddMemberDialog(e.project, MemberAllocationParams(datasetName = parentName))
          if (dialog.showAndGet()) {
            val state = dialog.state
            runBackgroundableTask(
              title = "Allocating member ${state.memberName}",
              project = e.project,
              cancellable = true
            ) {
              runCatching {
                dataOpsManager.performOperation(
                  operation = MemberAllocationOperation(
                    connectionConfig = connectionConfig,
                    urlConnection = connectionUrl,
                    request = state
                  ),
                  progressIndicator = it
                )
              }.onSuccess { currentNode.cleanCache() }
                .onFailure {
                  runInEdt {
                    Messages.showErrorDialog(
                      "Cannot create member ${state.memberName} ${state.datasetName} on ${connectionConfig.name}",
                      "Cannot Allocate Dataset"
                    )
                  }
                }
            }
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
    val selected = view.mySelectedNodesData.getOrNull(0)
    e.presentation.isEnabledAndVisible = selected?.node is LibraryNode || (
      selected?.node is FileLikeDatasetNode && selected.attributes is RemoteMemberAttributes
      )
  }

}