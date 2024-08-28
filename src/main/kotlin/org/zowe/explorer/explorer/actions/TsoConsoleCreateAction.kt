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
import com.intellij.openapi.util.Key
import com.intellij.util.containers.isEmpty
import org.zowe.explorer.common.message
import org.zowe.explorer.common.ui.showUntilDone
import org.zowe.explorer.config.configCrudable
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.tso.config.TSOSessionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.TsoOperation
import org.zowe.explorer.dataops.operations.TsoOperationMode
import org.zowe.explorer.explorer.ui.SelectTSOSessionDialog
import org.zowe.explorer.explorer.ui.SelectTSOSessionDialogState
import org.zowe.explorer.tso.SESSION_ADDED_TOPIC
import org.zowe.explorer.tso.TSOWindowFactory
import org.zowe.explorer.tso.config.TSOConfigWrapper
import org.zowe.explorer.utils.crudable.getAll
import org.zowe.explorer.utils.crudable.getByUniqueKey
import org.zowe.explorer.utils.runTask
import org.zowe.explorer.utils.sendTopic
import javax.swing.JComponent

/**
 * Class which represents TSO console creation action
 */
class TsoConsoleCreateAction : AnAction() {
  private val presentationText = "Zowe TSO Console"

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Method to perform an action which is called when OK button is pressed
   */
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    val dialog = SelectTSOSessionDialog(project, configCrudable, SelectTSOSessionDialogState(null))
    showUntilDone(
      initialState = dialog.state,
      factory = { dialog },
      test = { state ->
        val throwable: Throwable?
        val tsoSessionConfig = state.tsoSessionConfig
        if (tsoSessionConfig != null) {
          val connectionConfig = configCrudable.getByUniqueKey<ConnectionConfig>(tsoSessionConfig.connectionConfigUuid)
          if (connectionConfig != null) {
            throwable = runTask(title = "Testing TSO Connection to ${connectionConfig.url}", project = project) {
              return@runTask try {
                val tsoResponse = service<DataOpsManager>().performOperation(
                  TsoOperation(
                    TSOConfigWrapper(tsoSessionConfig, connectionConfig),
                    TsoOperationMode.START
                  ),
                  it
                )
                if (tsoResponse.servletKey?.isNotEmpty() == true && project != null) {
                  val configWrapper = TSOConfigWrapper(tsoSessionConfig, connectionConfig, tsoResponse)
                  sendTopic(SESSION_ADDED_TOPIC, project).create(project, configWrapper)
                }
                null
              } catch (t: Throwable) {
                t
              }
            }
          } else {
            throwable = Exception("Cannot get connection config from Crudable")
          }
        } else {
          throwable = Exception("TSO session config is not specified")
        }
        if (throwable != null) {
          val tMessage = if (throwable is ProcessCanceledException) {
            message("explorer.cancel.by.user.error")
          } else {
            "${throwable.message}"
          }
          TSOWindowFactory.showSessionFailureNotification(
            "Error Creating TSO Console",
            tMessage,
            project
          )
        }
        true
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
      e.presentation.putClientProperty(Key(JComponent.TOOL_TIP_TEXT_KEY), "Create connection first")
      return
    }
    if (configCrudable.getAll<TSOSessionConfig>().isEmpty()) {
      e.presentation.isEnabled = false
      e.presentation.putClientProperty(Key(JComponent.TOOL_TIP_TEXT_KEY), "Create TSO session first")
      return
    }
  }
}
