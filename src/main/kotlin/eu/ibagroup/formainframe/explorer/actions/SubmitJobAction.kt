package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.JobAction
import eu.ibagroup.formainframe.analytics.events.JobEvent
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.dataops.content.synchronizer.SaveStrategy
import eu.ibagroup.formainframe.dataops.operations.jobs.SubmitJobOperation
import eu.ibagroup.formainframe.dataops.operations.jobs.SubmitOperationParams
import eu.ibagroup.formainframe.explorer.ui.FILE_EXPLORER_VIEW
import eu.ibagroup.formainframe.explorer.ui.FileLikeDatasetNode
import eu.ibagroup.formainframe.explorer.ui.UssFileNode
import eu.ibagroup.formainframe.ui.build.jobs.JOB_ADDED_TOPIC
import eu.ibagroup.formainframe.utils.formMfPath
import eu.ibagroup.formainframe.utils.sendTopic

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
      runBackgroundableTask("Preparing for job submission") {
        val dataOpsManager = service<DataOpsManager>()
        val file = requestData.first
        if (service<ConfigService>().isAutoSyncEnabled.get() && dataOpsManager.isSyncSupported(file)) {
          val contentSynchronizer = dataOpsManager.getContentSynchronizer(file)
          contentSynchronizer?.synchronizeWithRemote(DocumentedSyncProvider(file, SaveStrategy.default(e.project)), it)
        }
        it.text = "Submitting job from file ${file.name}"

        runCatching {
          service<AnalyticsService>().trackAnalyticsEvent(JobEvent(JobAction.SUBMIT))

          val attributes = service<DataOpsManager>().tryToGetAttributes(requestData.first)
            ?: throw IllegalArgumentException("Cannot find attributes for specified file.")

          val submitFilePath = attributes.formMfPath()
          service<DataOpsManager>().performOperation(
            operation = SubmitJobOperation(
              request = SubmitOperationParams(submitFilePath),
              connectionConfig = requestData.second
            ), it
          ).also {result ->
            e.project?.let {project ->
              sendTopic(JOB_ADDED_TOPIC).submitted(project, requestData.second, submitFilePath, result)
            }
          }
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
