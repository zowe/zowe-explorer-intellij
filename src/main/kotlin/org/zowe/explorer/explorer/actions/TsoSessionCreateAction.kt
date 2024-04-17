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

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.ui.Messages
import com.intellij.util.containers.isEmpty
import org.zowe.explorer.common.message
import org.zowe.explorer.common.ui.showUntilDone
import org.zowe.explorer.config.configCrudable
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.TsoOperation
import org.zowe.explorer.dataops.operations.TsoOperationMode
import org.zowe.explorer.ui.build.tso.SESSION_ADDED_TOPIC
import org.zowe.explorer.ui.build.tso.config.TSOConfigWrapper
import org.zowe.explorer.ui.build.tso.ui.TSOSessionDialog
import org.zowe.explorer.ui.build.tso.ui.TSOSessionParams
import org.zowe.explorer.utils.crudable.getAll
import org.zowe.explorer.utils.runTask
import org.zowe.explorer.utils.sendTopic

/**
 * Class which represents TSO session creation action
 */
class TsoSessionCreateAction : AnAction() {
  private val presentationText = "TSO Console"

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Method to perform an action which is called when OK button is pressed
   */
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    var dialog = TSOSessionDialog(project, TSOSessionParams())
    showUntilDone(
      initialState = dialog.state,
      factory = { dialog },
      test = { state ->
        val throwable = runTask(title = "Testing TSO Connection to ${state.connectionConfig.url}", project = project) {
          return@runTask try {
            val tsoResponse = service<DataOpsManager>().performOperation(
              TsoOperation(dialog.state, TsoOperationMode.START), it
            )
            if (tsoResponse.servletKey?.isNotEmpty() == true && project != null) {
              val config = TSOConfigWrapper(dialog.state, tsoResponse)
              sendTopic(SESSION_ADDED_TOPIC, project).create(project, config)
            }
            null
          } catch (t: Throwable) {
            t
          }
        }
        if (throwable != null) {
          val tMessage = if (throwable is ProcessCanceledException) {
            message("explorer.cancel.by.user.error")
          } else {
            "${throwable.message}"
          }
          val errorTemplate = "An error occurred. See details below:\n $tMessage"
          Messages.showErrorDialog(
            project,
            errorTemplate,
            "Error"
          )
        }
        dialog = TSOSessionDialog(project, dialog.state)
        throwable == null
      }
    )
  }

  /**
   * Determines if an action is dumb aware or not
   */
  override fun isDumbAware(): Boolean {
    return true
  }

  /**
   * Method to update the UI when any mouse events are performed
   */
  override fun update(e: AnActionEvent) {
    e.presentation.text = presentationText
    if (configCrudable.getAll<ConnectionConfig>().isEmpty()) {
      e.presentation.isEnabled = false
    }
  }
}
