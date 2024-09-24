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

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.IdeFocusManager
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.config.ws.ui.jes.JesWsDialog
import org.zowe.explorer.config.ws.ui.jes.toDialogState
import org.zowe.explorer.explorer.ui.JesExplorerView
import org.zowe.explorer.explorer.ui.JesWsNode
import org.zowe.explorer.explorer.ui.getExplorerView
import org.zowe.explorer.utils.clone
import org.zowe.explorer.utils.crudable.getByUniqueKey

/**
 * Action class for edit JES working set act
 */
class EditJesWorkingSetAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Called when edit JES working set option is chosen from context menu,
   * runs the edit JES working set operation
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: return
    val node = view.mySelectedNodesData[0].node
    if (node is JesWsNode) {
      val workingSetConfig =
        ConfigService.getService().crudable
          .getByUniqueKey<JesWorkingSetConfig>(node.value.uuid)
          ?.clone() as JesWorkingSetConfig
      service<IdeFocusManager>().runOnOwnContext(
        DataContext.EMPTY_CONTEXT
      ) {
        val dialog = JesWsDialog(
          ConfigService.getService().crudable,
          workingSetConfig.toDialogState().apply { mode = DialogMode.UPDATE }
        )
        if (dialog.showAndGet()) {
          val state = dialog.state
          ConfigService.getService().crudable.update(state.workingSetConfig)
        }
      }
    }
  }

  /**
   * This method is needed for interface implementation
   */
  override fun isDumbAware(): Boolean {
    return true
  }

  /**
   * Determines which objects are JES working sets and therefore can be edited
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    e.presentation.isEnabledAndVisible = selected.size == 1 && (selected[0].node is JesWsNode)
  }
}
