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

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.Key
import com.intellij.util.containers.isEmpty
import eu.ibagroup.formainframe.common.ui.showUntilDone
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.operations.TsoOperation
import eu.ibagroup.formainframe.dataops.operations.TsoOperationMode
import eu.ibagroup.formainframe.explorer.ui.SelectTSOSessionDialog
import eu.ibagroup.formainframe.explorer.ui.SelectTSOSessionDialogState
import eu.ibagroup.formainframe.telemetry.NotificationsService
import eu.ibagroup.formainframe.tso.SESSION_ADDED_TOPIC
import eu.ibagroup.formainframe.tso.config.TSOConfigWrapper
import eu.ibagroup.formainframe.tso.config.TSOSessionConfig
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.runTask
import eu.ibagroup.formainframe.utils.sendTopic
import javax.swing.JComponent

/**
 * Class which represents TSO console creation action
 */
class TsoConsoleCreateAction : AnAction() {
  private val presentationText = "TSO Console"

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Method to perform an action which is called when OK button is pressed
   */
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    val dialog = SelectTSOSessionDialog(
      project,
      ConfigService.getService().crudable,
      SelectTSOSessionDialogState(null)
    )
    showUntilDone(
      initialState = dialog.state,
      factory = { dialog },
      test = { state ->
        val throwable: Throwable?
        val tsoSessionConfig = state.tsoSessionConfig
        if (tsoSessionConfig != null) {
          val connectionConfig = ConfigService.getService().crudable
            .getByUniqueKey<ConnectionConfig>(tsoSessionConfig.connectionConfigUuid)
          if (connectionConfig != null) {
            throwable = runTask(title = "Testing TSO Connection to ${connectionConfig.url}", project = project) {
              return@runTask try {
                val tsoResponse = DataOpsManager.getService().performOperation(
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
          NotificationsService
            .getService()
            .notifyError(throwable, project = project, custTitle = "Error creating TSO Console")
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
    if (ConfigService.getService().crudable.getAll<ConnectionConfig>().isEmpty()) {
      e.presentation.isEnabled = false
      e.presentation.putClientProperty(Key(JComponent.TOOL_TIP_TEXT_KEY), "Create connection first")
      return
    }
    if (ConfigService.getService().crudable.getAll<TSOSessionConfig>().isEmpty()) {
      e.presentation.isEnabled = false
      e.presentation.putClientProperty(Key(JComponent.TOOL_TIP_TEXT_KEY), "Create TSO session first")
      return
    }
  }
}
