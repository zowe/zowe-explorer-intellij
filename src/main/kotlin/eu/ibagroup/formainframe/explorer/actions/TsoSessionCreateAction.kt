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

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.util.containers.isEmpty
import eu.ibagroup.formainframe.common.ui.showUntilDone
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.operations.TsoOperation
import eu.ibagroup.formainframe.dataops.operations.TsoOperationMode
import eu.ibagroup.formainframe.ui.build.tso.SESSION_ADDED_TOPIC
import eu.ibagroup.formainframe.ui.build.tso.config.TSOConfigWrapper
import eu.ibagroup.formainframe.ui.build.tso.ui.TSOSessionDialog
import eu.ibagroup.formainframe.ui.build.tso.ui.TSOSessionParams
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.runTask
import eu.ibagroup.formainframe.utils.sendTopic

/**
 * Class which represents TSO session creation action
 */
class TsoSessionCreateAction : AnAction() {
  private val presentationText = "TSO Console"

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
            val tsoResponse = service<DataOpsManager>().performOperation(TsoOperation(dialog.state, TsoOperationMode.START))
            if (tsoResponse.servletKey?.isNotEmpty() == true && project != null) {
              val config = TSOConfigWrapper(dialog.state, tsoResponse)
              sendTopic(SESSION_ADDED_TOPIC).create(project, config)
            }
            null
          } catch (t: Throwable) {
            t
          }
        }
        if (throwable != null) {
          val errorTemplate = "An error occurred. See details below:\n $throwable"
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