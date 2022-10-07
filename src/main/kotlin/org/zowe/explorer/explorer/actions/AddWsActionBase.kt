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

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.util.containers.isEmpty
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.configCrudable
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.connect.ui.ConnectionDialog
import org.zowe.explorer.config.connect.ui.ConnectionDialogState
import org.zowe.explorer.config.connect.ui.initEmptyUuids
import org.zowe.explorer.config.ws.WorkingSetConfig
import org.zowe.explorer.config.ws.ui.AbstractWsDialog
import org.zowe.explorer.config.ws.ui.AbstractWsDialogState
import org.zowe.explorer.explorer.ui.ExplorerTreeView
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.getAll

/**
 * Abstract action for adding Working Set (for files or for jobs) through UI.
 */
abstract class AddWsActionBase : AnAction() {

  abstract val explorerView: DataKey<out ExplorerTreeView<*, *>>

  /** Shows dialog if connections list is empty and shows WS dialog after connection creation. */
  override fun actionPerformed(e: AnActionEvent) {
    if (configCrudable.getAll<ConnectionConfig>().isEmpty()) {
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
    val dialog = createDialog(configCrudable)
    if (dialog.showAndGet()) {
      val state = dialog.state
      val workingSetConfig = state.workingSetConfig
      configCrudable.add(workingSetConfig)
    }
  }

  /** Presentation text in explorer context menu */
  abstract val presentationTextInExplorer: String

  /** Default presentation text. */
  abstract val defaultPresentationText: String

  /**
   * Implementation should create working set dialog for specific working set type.
   * @param configCrudable crudable from ConfigService.
   * @see Crudable
   * @see ConfigService
   */
  abstract fun createDialog(configCrudable: Crudable): AbstractWsDialog<*, *, out AbstractWsDialogState<out WorkingSetConfig, *>>

  override fun isDumbAware(): Boolean {
    return true
  }

  /** Updates text and icon regarding the context from which action should be triggered. */
  override fun update(e: AnActionEvent) {
    if (e.getData(explorerView) != null) {
      e.presentation.text = presentationTextInExplorer
      e.presentation.icon = AllIcons.Nodes.Project
    } else {
      e.presentation.text = defaultPresentationText
      e.presentation.icon = AllIcons.General.Add
    }
  }
}
