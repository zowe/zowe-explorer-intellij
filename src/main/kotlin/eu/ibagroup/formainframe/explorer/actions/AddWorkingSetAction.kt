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

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.util.containers.isEmpty
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.connect.ui.ConnectionDialog
import eu.ibagroup.formainframe.config.connect.ui.ConnectionDialogState
import eu.ibagroup.formainframe.config.connect.ui.initEmptyUuids
import eu.ibagroup.formainframe.config.ws.ui.WorkingSetDialog
import eu.ibagroup.formainframe.config.ws.ui.WorkingSetDialogState
import eu.ibagroup.formainframe.config.ws.ui.initEmptyUuids
import eu.ibagroup.formainframe.explorer.ui.FILE_EXPLORER_VIEW
import eu.ibagroup.formainframe.utils.crudable.getAll

class AddWorkingSetAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    if (configCrudable.getAll<ConnectionConfig>().isEmpty()) {
      val state = ConnectionDialog.showAndTestConnection(
        crudable = configCrudable,
        project = e.project,
        initialState = ConnectionDialogState().initEmptyUuids(configCrudable)
      )
      if (state != null) {
        val urlConnection = state.urlConnection
        val connectionConfig = state.connectionConfig
        CredentialService.instance.setCredentials(connectionConfig.uuid, state.username, state.password)
        configCrudable.add(urlConnection)
        configCrudable.add(connectionConfig)
      } else {
        return
      }
    }
    val dialog = WorkingSetDialog(configCrudable, WorkingSetDialogState().initEmptyUuids(configCrudable))
    if (dialog.showAndGet()) {
      val state = dialog.state
      val workingSetConfig = state.workingSetConfig
      configCrudable.add(workingSetConfig)
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }

  override fun update(e: AnActionEvent) {
    if (e.getData(FILE_EXPLORER_VIEW) != null) {
      e.presentation.text = "Working Set"
      e.presentation.icon = AllIcons.Nodes.Project
    } else {
      e.presentation.text = "Create Working Set"
      e.presentation.icon = AllIcons.General.Add
    }
  }

}