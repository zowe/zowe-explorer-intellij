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

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.operations.jobs.BasicHoldJobParams
import eu.ibagroup.formainframe.dataops.operations.jobs.HoldJobOperation
import eu.ibagroup.formainframe.ui.build.jobs.JOBS_LOG_VIEW
import eu.ibagroup.r2z.JobStatus

// TODO: doc Nikita
class HoldJobAction : AnAction() {

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
        title = "Holding ${jobStatus.jobName}: ${jobStatus.jobId}",
        project = e.project,
        cancellable = true
      ) {
        runCatching {
          dataOpsManager.performOperation(
            operation = HoldJobOperation(
              request = BasicHoldJobParams(jobStatus.jobName, jobStatus.jobId),
              connectionConfig = view.getConnectionConfig()
            ),
            progressIndicator = it
          )
        }.onFailure {
          view.showNotification(
            "Error holding ${jobStatus.jobName}: ${jobStatus.jobId}",
            "${it.message}",
            e.project,
            NotificationType.ERROR
          )
        }.onSuccess {
          view.showNotification(
            "${jobStatus.jobName}: ${jobStatus.jobId} has been held",
            "${it}",
            e.project,
            NotificationType.INFORMATION
          )
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
    if (jobStatus == JobStatus.Status.OUTPUT || jobStatus == JobStatus.Status.ACTIVE || jobStatus == null) {
      e.presentation.isEnabled = false
    }
  }

}