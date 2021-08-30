package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.JobAction
import eu.ibagroup.formainframe.analytics.events.JobEvent
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.operations.jobs.SubmitJobOperation
import eu.ibagroup.formainframe.dataops.operations.jobs.SubmitOperationParams
import eu.ibagroup.formainframe.explorer.ui.*

class SubmitJobAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val node = selected.getOrNull(0)?.node ?: return

    val requestData = getRequestDataForNode(node)
    if (requestData != null) {
      runBackgroundableTask("Job Submission") {
        runCatching {
          service<AnalyticsService>().trackAnalyticsEvent(JobEvent(JobAction.SUBMIT))

          service<DataOpsManager>().performOperation(
            operation = SubmitJobOperation(
              request = SubmitOperationParams(requestData.first),
              connectionConfig = requestData.second
            ), it
          )
        }.onSuccess {
          view.explorer.showNotification("Job ${it.jobname} has been submitted", "$it", project = e.project)
        }.onFailure {
          view.explorer.reportThrowable(it, e.project)
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
        && (node is FileLikeDatasetNode || node is UssFileNode)
  }
}