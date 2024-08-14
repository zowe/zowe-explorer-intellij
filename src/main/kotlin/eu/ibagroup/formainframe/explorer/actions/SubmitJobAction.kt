/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.JobAction
import eu.ibagroup.formainframe.analytics.events.JobEvent
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.content.synchronizer.*
import eu.ibagroup.formainframe.dataops.operations.jobs.SubmitFilePathOperationParams
import eu.ibagroup.formainframe.dataops.operations.jobs.SubmitJobOperation
import eu.ibagroup.formainframe.explorer.ui.FileExplorerView
import eu.ibagroup.formainframe.explorer.ui.FileLikeDatasetNode
import eu.ibagroup.formainframe.explorer.ui.UssFileNode
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import eu.ibagroup.formainframe.ui.build.jobs.JOB_ADDED_TOPIC
import eu.ibagroup.formainframe.utils.formMfPath
import eu.ibagroup.formainframe.utils.sendTopic

/**
 * Action class for executing submit job on mainframe
 */
class SubmitJobAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Called when submit option is chosen from context menu,
   * runs the submit operation
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val node = selected.getOrNull(0)?.node ?: return

    val requestData = getRequestDataForNode(node)
    if (requestData != null) {
      val file = requestData.first
      if (checkFileForSync(e.project, file)) return
      runBackgroundableTask("Preparing for job submission") {
        val dataOpsManager = service<DataOpsManager>()
        if (service<ConfigService>().isAutoSyncEnabled && dataOpsManager.isSyncSupported(file)) {
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
              request = SubmitFilePathOperationParams(submitFilePath),
              connectionConfig = requestData.second
            ), it
          ).also { result ->
            e.project?.let { project ->
              sendTopic(JOB_ADDED_TOPIC, project).submitted(project, requestData.second, submitFilePath, result)
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

  /**
   * This method is needed for interface implementation
   */
  override fun isDumbAware(): Boolean {
    return true
  }

  /**
   * Determines which objects on mainframe can be submitted
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val node = selected.getOrNull(0)?.node
    e.presentation.isVisible = selected.size == 1
        && (node is FileLikeDatasetNode || node is UssFileNode)
  }
}
