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

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.runBackgroundableTask
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.operations.jobs.SubmitJobJclOperationParams
import org.zowe.explorer.dataops.operations.jobs.SubmitJobOperation
import org.zowe.explorer.explorer.FileExplorerContentProvider
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.ui.build.jobs.JOB_ADDED_TOPIC
import org.zowe.explorer.utils.sendTopic

/**
 * Action to submit job using the button in editor when edit JCL
 */
class SubmitJobToolbarAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Submit a job on button click
   * Opens job log
   */
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
    val jclContent = editor.document.text
    if (jclContent.isNotEmpty()) {
      val dataOpsManager = DataOpsManager.getService()
      val parentAttributes = dataOpsManager.tryToGetAttributes(file.parent) as RemoteJobAttributes
      val connectionConfig = parentAttributes.requesters[0].connectionConfig
      runBackgroundableTask(
        title = "Submitting job from file ${file.name}",
        project = project,
        cancellable = true
      ) {
        runCatching {
          dataOpsManager.performOperation(
            operation = SubmitJobOperation(
              request = SubmitJobJclOperationParams(jclContent),
              connectionConfig = connectionConfig
            ),
            progressIndicator = it
          ).also { result ->
            e.project?.let { project ->
              sendTopic(JOB_ADDED_TOPIC, project).submitted(project, connectionConfig, file.parent.path, result)
            }
          }
        }.onFailure {
          NotificationsService.getService().notifyError(it, project)
        }
      }
    }
  }

  /**
   * Makes action visible only in editor when edit JCL
   */
  override fun update(e: AnActionEvent) {
    val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val parentFile = file.parent ?: return
    val dataOpsManager = DataOpsManager.getService()
    val attributes = dataOpsManager.tryToGetAttributes(file)
    val parentAttributes = dataOpsManager.tryToGetAttributes(parentFile)
    e.presentation.isEnabledAndVisible = attributes == null &&
      parentAttributes is RemoteJobAttributes &&
      file.isWritable
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}
