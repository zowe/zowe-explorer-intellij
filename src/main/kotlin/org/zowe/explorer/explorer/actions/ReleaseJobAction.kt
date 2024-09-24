/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package org.zowe.explorer.explorer.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.runBackgroundableTask
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.jobs.BasicReleaseJobParams
import org.zowe.explorer.dataops.operations.jobs.ReleaseJobOperation
import org.zowe.explorer.ui.build.jobs.JOBS_LOG_VIEW
import org.zowe.kotlinsdk.Job

/** Action to release a holding job in the Jobs Tool Window */
class ReleaseJobAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  override fun isDumbAware(): Boolean {
    return true
  }

  /**
   * Release a job on button click
   * After completion shows a notification
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(JOBS_LOG_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val jobStatus = view.getJobLogger().logFetcher.getCachedJobStatus()
    val dataOpsManager = DataOpsManager.getService()
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
          view.showNotification(
            "Error releasing ${jobStatus.jobName}: ${jobStatus.jobId}",
            "${it.message}",
            e.project,
            NotificationType.ERROR
          )
        }.onSuccess {
          view.showNotification(
            "${jobStatus.jobName}: ${jobStatus.jobId} has been released",
            "${it}",
            e.project,
            NotificationType.INFORMATION
          )
        }
      }
    }
  }

  /** A job can be released if its status is "Input" */
  override fun update(e: AnActionEvent) {
    val view = e.getData(JOBS_LOG_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val jobStatus = view.getJobLogger().logFetcher.getCachedJobStatus()?.status
    if (jobStatus == Job.Status.OUTPUT || jobStatus == Job.Status.ACTIVE || jobStatus == null) {
      e.presentation.isEnabled = false
    }
  }

}
