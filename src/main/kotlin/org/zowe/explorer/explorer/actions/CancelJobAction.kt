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

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.jobs.*
import org.zowe.explorer.ui.build.jobs.JOBS_LOG_VIEW
import org.zowe.kotlinsdk.JobStatus

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
