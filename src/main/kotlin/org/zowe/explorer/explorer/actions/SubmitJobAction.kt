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
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.jobs.SubmitJobOperation
import org.zowe.explorer.dataops.operations.jobs.SubmitOperationParams
import org.zowe.explorer.explorer.ui.*

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
      runBackgroundableTask("Job Submission") {
        runCatching {
          service<DataOpsManager>().performOperation(
            operation = SubmitJobOperation(
              request = SubmitOperationParams(requestData.first),
              connectionConfig = requestData.second
            ), it
          )
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
