package eu.ibagroup.formainframe.explorer.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.dataops.operations.jobs.BasicPurgeJobParams
import eu.ibagroup.formainframe.dataops.operations.jobs.PurgeJobOperation
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.ui.build.jobs.JOBS_LOG_VIEW
import eu.ibagroup.formainframe.ui.build.jobs.JobBuildTreeView
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.r2z.JobStatus

class PurgeJobAction : AnAction() {

  override fun isDumbAware(): Boolean {
    return true
  }

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(JES_EXPLORER_VIEW) ?: e.getData(JOBS_LOG_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    var jobStatus: JobStatus? = null
    var connectionConfig: ConnectionConfig? = null
    if (view is JesExplorerView) {
      val node = view.mySelectedNodesData.getOrNull(0)?.node
      if (node is ExplorerTreeNode<*>) {
        val virtualFile = node.virtualFile
        if (virtualFile != null) {
          val dataOpsManager = node.explorer.componentManager.service<DataOpsManager>()
          val attributes: RemoteJobAttributes = dataOpsManager.tryToGetAttributes(virtualFile)?.clone() as RemoteJobAttributes
          jobStatus = attributes.jobInfo
          connectionConfig = attributes.requesters[0].connectionConfig
        }
      }
    } else if (view is JobBuildTreeView) {
      jobStatus = view.getJobLogger().logFetcher.getCachedJobStatus()
      connectionConfig = view.getConnectionConfig()
    }
    val dataOpsManager = service<DataOpsManager>()
    if (jobStatus != null && connectionConfig != null) {
      runBackgroundableTask(
        title = "Purging ${jobStatus.jobName}: ${jobStatus.jobId}",
        project = e.project,
        cancellable = true
      ) {
        runCatching {
          dataOpsManager.performOperation(
            operation = PurgeJobOperation(
              request = BasicPurgeJobParams(jobStatus.jobName, jobStatus.jobId),
              connectionConfig = connectionConfig
            ),
            progressIndicator = it
          )
        }.onFailure {
          if (view is JesExplorerView) {
            view.explorer.showNotification(
              "Error purging ${jobStatus.jobName}: ${jobStatus.jobId}",
              "${it.message}",
              NotificationType.ERROR,
              e.project
            )
          } else if (view is JobBuildTreeView) {
            view.showNotification(
              "Error purging ${jobStatus.jobName}: ${jobStatus.jobId}",
              "${it.message}",
              e.project,
              NotificationType.ERROR
            )
          }
        }.onSuccess {
          if (view is JesExplorerView) {
            view.explorer.showNotification(
              "${jobStatus.jobName}: ${jobStatus.jobId} has been purged",
              "${it}",
              NotificationType.INFORMATION,
              e.project
            )
            val jobFilterNode = view.mySelectedNodesData.getOrNull(0)?.node?.parent
            if (jobFilterNode is FetchNode) {
              jobFilterNode.cleanCache()
              jobFilterNode.query?.let { query -> view.getNodesByQueryAndInvalidate(query) }
            }
          } else if (view is JobBuildTreeView) {
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
  }

  override fun update(e: AnActionEvent) {
    val view = e.getData(JES_EXPLORER_VIEW) ?: e.getData(JOBS_LOG_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    if (view is JesExplorerView) {
      val selected = view.mySelectedNodesData
      val node = selected.getOrNull(0)?.node
      e.presentation.isVisible = selected.size == 1
              && node is JobNode
    } else if (view is JobBuildTreeView) {
      val jobStatus = view.getJobLogger().logFetcher.getCachedJobStatus()?.status
      if (jobStatus == null) {
        e.presentation.isEnabled = false
      }
    }
  }
}