package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.operations.jobs.*
import eu.ibagroup.formainframe.ui.build.jobs.JOBS_LOG_VIEW

class ReleaseJobAction : AnAction() {

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
        title = "Release ${jobStatus.jobName}: ${jobStatus.jobId}",
        project = e.project,
        cancellable = true
      ) {
        runCatching {
          dataOpsManager.performOperation(
            operation = ReleaseJobOperation(
              request = BasicReleaseJobParams(jobStatus.jobName, jobStatus.jobId),
              connectionConfig = view.getConnectionConfig()
            ),
            progressIndicator = it
          )
        }.onFailure {
          view.showErrorNotification("Error releasing ${jobStatus.jobName}: ${jobStatus.jobId}", "${it.message}", e.project)
        }
      }
    }

  }

}