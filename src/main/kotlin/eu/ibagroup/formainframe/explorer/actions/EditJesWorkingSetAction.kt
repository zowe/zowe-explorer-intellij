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
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.IdeFocusManager
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.JesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.jes.JesWsDialog
import eu.ibagroup.formainframe.config.ws.ui.jes.toDialogState
import eu.ibagroup.formainframe.explorer.ui.JES_EXPLORER_VIEW
import eu.ibagroup.formainframe.explorer.ui.JesWsNode
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey

/**
 * Action class for edit JES working set act
 */
class EditJesWorkingSetAction: AnAction() {

  /**
   * Called when edit JES working set option is chosen from context menu,
   * runs the edit JES working set operation
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(JES_EXPLORER_VIEW) ?: return
    val node = view.mySelectedNodesData[0].node
    if (node is JesWsNode) {
      val workingSetConfig =
        configCrudable.getByUniqueKey<JesWorkingSetConfig>(node.value.uuid)?.clone() as JesWorkingSetConfig
      service<IdeFocusManager>().runOnOwnContext(
        DataContext.EMPTY_CONTEXT
      ) {
        val dialog = JesWsDialog(configCrudable, workingSetConfig.toDialogState().apply { mode = DialogMode.UPDATE })
        if (dialog.showAndGet()) {
          val state = dialog.state
          configCrudable.update(state.workingSetConfig)
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
    val view = e.getData(JES_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    e.presentation.isEnabledAndVisible = selected.size == 1 && (selected[0].node is JesWsNode)
  }
}