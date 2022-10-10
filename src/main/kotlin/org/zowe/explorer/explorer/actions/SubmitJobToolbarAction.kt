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
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.operations.jobs.*
import org.zowe.explorer.explorer.FileExplorerContentProvider
import org.zowe.explorer.ui.build.jobs.JOB_ADDED_TOPIC
import org.zowe.explorer.utils.sendTopic

/**
 * Action to submit job using the button in editor when edit JCL
 */
class SubmitJobToolbarAction: AnAction() {

  /**
   * Submit a job on button click
   * Opens job log
   */
  override fun actionPerformed(e: AnActionEvent) {
    val explorerView = e.project?.let { FileExplorerContentProvider.getInstance().getExplorerView(it) }
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
    val jclContent = editor.document.text
    if (jclContent.isNotEmpty()) {
      val dataOpsManager = service<DataOpsManager>()
      val parentAttributes = dataOpsManager.tryToGetAttributes(file.parent) as RemoteJobAttributes
      val connectionConfig = parentAttributes.requesters[0].connectionConfig
      runBackgroundableTask(
        title = "Submitting job from file ${file.name}",
        project = e.project,
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
              sendTopic(JOB_ADDED_TOPIC).submitted(project, connectionConfig, file.parent.path, result)
            }
          }
        }.onFailure {
          explorerView?.explorer?.reportThrowable(it, e.project)
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
    val dataOpsManager = service<DataOpsManager>()
    val attributes = dataOpsManager.tryToGetAttributes(file)
    val parentAttributes = dataOpsManager.tryToGetAttributes(file.parent)
    e.presentation.isEnabledAndVisible = attributes == null &&
        parentAttributes is RemoteJobAttributes &&
        file.isWritable
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}
