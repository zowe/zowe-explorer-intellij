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
import org.zowe.explorer.config.configCrudable
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.connect.ui.ConnectionDialog
import org.zowe.explorer.config.connect.ui.ConnectionDialogState
import org.zowe.explorer.config.connect.ui.initEmptyUuids

/**
 * Action for adding Connection through UI.
 */
class AddConnectionAction : AnAction() {

  /** Shows connection dialog */
  override fun actionPerformed(e: AnActionEvent) {
    val state = ConnectionDialog.showAndTestConnection(
      crudable = configCrudable,
      project = e.project,
      initialState = ConnectionDialogState().initEmptyUuids(configCrudable)
    )
    if (state != null) {
      val connectionConfig = state.connectionConfig
      CredentialService.instance.setCredentials(connectionConfig.uuid, state.username, state.password)
      configCrudable.add(connectionConfig)
    } else {
      return
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}
