package eu.ibagroup.formainframe.explorer.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.operations.jobs.BasicPurgeJobParams
import eu.ibagroup.formainframe.dataops.operations.jobs.PurgeJobOperation
import eu.ibagroup.formainframe.ui.build.jobs.JOBS_LOG_VIEW
import eu.ibagroup.r2z.JobStatus
import java.awt.event.MouseEvent

class PurgeJobAction : AnAction() {

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
        title = "Purging ${jobStatus.jobName}: ${jobStatus.jobId}",
        project = e.project,
        cancellable = true
      ) {
        runCatching {
          dataOpsManager.performOperation(
            operation = PurgeJobOperation(
              request = BasicPurgeJobParams(jobStatus.jobName, jobStatus.jobId),
              connectionConfig = view.getConnectionConfig()
            ),
            progressIndicator = it
          )
        }.onFailure {
          view.showNotification(
            "Error purging ${jobStatus.jobName}: ${jobStatus.jobId}",
            "${it.message}",
            e.project,
            NotificationType.ERROR
          )
        }.onSuccess {
          view.showNotification(
            "${jobStatus.jobName}: ${jobStatus.jobId} has been purged",
            "${it}",
            e.project,
            NotificationType.INFORMATION
          )
        }
      }
    }
  }

  override fun update(e: AnActionEvent) {
    //TODO
  }
}