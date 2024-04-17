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
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFileManager
import org.zowe.explorer.config.configCrudable
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.connect.ui.zosmf.ConnectionDialogState
import org.zowe.explorer.config.connect.ui.zosmf.ZoweTeamConfigDialog
import org.zowe.explorer.config.connect.ui.zosmf.initEmptyUuids
import org.zowe.explorer.zowe.ZOWE_CONFIG_NAME
import org.zowe.explorer.zowe.service.ZoweConfigService
import java.nio.file.Path


/**
 * Action for adding zowe team config file through UI.
 */
class AddZoweTeamConfigAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Updates the presentation of the AddZoweTeamConfig action.
   * @see com.intellij.openapi.actionSystem.AnAction.update
   */
  override fun update(e: AnActionEvent) {
    val project = e.project ?: let {
      e.presentation.isEnabled = false
      e.presentation.description = "Configuration file can only be created in the project"
      return
    }
    val zoweConfigLocation = "${project.basePath}/$ZOWE_CONFIG_NAME"

    runReadAction {
      VirtualFileManager.getInstance().findFileByNioPath(Path.of(zoweConfigLocation))
    }?.let {
      e.presentation.isEnabled = false
      e.presentation.description = "$ZOWE_CONFIG_NAME already exists in the project"
    } ?: return
  }

  /**
   * Shows Create Zowe Team Config dialog
   * @see com.intellij.openapi.actionSystem.AnAction.actionPerformed
   */
  override fun actionPerformed(e: AnActionEvent) {
    val state = ZoweTeamConfigDialog.showAndTestConnection(
      crudable = configCrudable,
      project = e.project,
      initialState = ConnectionDialogState().initEmptyUuids(configCrudable)
    )
    if (state != null) {
      val connectionConfig = state.connectionConfig
      val project = e.project ?: let {
        e.presentation.isEnabled = false
        e.presentation.description = "$ZOWE_CONFIG_NAME already exists in the project"
        return
      }
      val zoweConfigService = project.service<ZoweConfigService>()
      zoweConfigService.addZoweConfigFile(state)

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
