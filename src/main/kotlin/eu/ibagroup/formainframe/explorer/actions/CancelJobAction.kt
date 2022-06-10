package eu.ibagroup.formainframe.explorer.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.operations.jobs.*
import eu.ibagroup.formainframe.ui.build.jobs.JOBS_LOG_VIEW
import eu.ibagroup.r2z.JobStatus

class CancelJobAction : AnAction() {

  override fun isDumbAware(): Boolean {
    return true
  }

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(JOBS_LOG_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val jobStatus = view.getJobLogger().logFetcher.getCachedJobStatus()
    val dataOpsManager = service<DataOpsManager>()
    if (jobStatus != null) {
      runBackgroundableTask(
        title = "Canceling ${jobStatus.jobName}: ${jobStatus.jobId}",
        project = e.project,
        cancellable = true
      ) {
        runCatching {
          dataOpsManager.performOperation(
            operation = CancelJobOperation(
              request = BasicCancelJobParams(jobStatus.jobName, jobStatus.jobId),
              connectionConfig = view.getConnectionConfig()
            ),
            progressIndicator = it
          )
        }.onFailure {
          view.showNotification("Error cancelling ${jobStatus.jobName}: ${jobStatus.jobId}", "${it.message}", e.project, NotificationType.ERROR)
        }.onSuccess {
          view.showNotification("${jobStatus.jobName}: ${jobStatus.jobId} has been cancelled", "${it}", e.project, NotificationType.INFORMATION)
        }
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val view = e.getData(JOBS_LOG_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val jobStatus = view.getJobLogger().logFetcher.getCachedJobStatus()?.status
    if(jobStatus == JobStatus.Status.OUTPUT) {
      e.presentation.isEnabled = false
    }
  }

}