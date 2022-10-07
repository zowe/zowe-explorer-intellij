/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import org.zowe.explorer.analytics.AnalyticsService
import org.zowe.explorer.analytics.events.JobAction
import org.zowe.explorer.analytics.events.JobEvent
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.content.synchronizer.DocumentedSyncProvider
import org.zowe.explorer.dataops.content.synchronizer.SaveStrategy
import org.zowe.explorer.dataops.operations.jobs.SubmitFilePathOperationParams
import org.zowe.explorer.dataops.operations.jobs.SubmitJobOperation
import org.zowe.explorer.explorer.ui.FILE_EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.FileLikeDatasetNode
import org.zowe.explorer.explorer.ui.UssFileNode
import org.zowe.explorer.ui.build.jobs.JOB_ADDED_TOPIC
import org.zowe.explorer.utils.formMfPath
import org.zowe.explorer.utils.sendTopic

/**
 * Action class for executing submit job on mainframe
 */
class SubmitJobAction : AnAction() {

  /**
   * Called when submit option is chosen from context menu,
   * runs the submit operation
   */
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
        if (service<ConfigService>().isAutoSyncEnabled && dataOpsManager.isSyncSupported(file)) {
          val contentSynchronizer = dataOpsManager.getContentSynchronizer(file)
          contentSynchronizer?.synchronizeWithRemote(DocumentedSyncProvider(file, SaveStrategy.default(e.project)), it)
        }
        it.text = "Submitting job from file ${file.name}"

        runCatching {

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
