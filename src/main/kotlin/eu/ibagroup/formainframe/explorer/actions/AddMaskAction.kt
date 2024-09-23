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
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.MaskStateWithWS
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.explorer.ui.AddOrEditMaskDialog
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeView
import eu.ibagroup.formainframe.explorer.ui.FileExplorerView
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import eu.ibagroup.formainframe.utils.MaskType
import eu.ibagroup.formainframe.utils.getSelectedNodesWorkingSets

/** Action to add USS or z/OS mask */
class AddMaskAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /** Add new mask to the working set, where the action is triggered */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: return

    val workingSets = getSelectedNodesWorkingSets<FilesWorkingSet>(view as ExplorerTreeView<*, *, *>)
    val ws = workingSets.firstOrNull() ?: return
    val initialState = MaskStateWithWS(ws = ws)
    val dialog = AddOrEditMaskDialog(e.project, "Create Mask", ws.connectionConfig, initialState)
    if (dialog.showAndGet()) {
      val state = dialog.state
      when (state.type) {
        MaskType.ZOS -> ws.addMask(DSMask(state.mask.uppercase(), mutableListOf(), ""))
        MaskType.USS -> ws.addUssPath(UssPath(state.mask))
      }
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }

  /**
   * Decides to show the add mask action or not.
   * Shows the action if:
   * 1. Explorer view is not null
   * 2. Item(s) from them same files working set is(are) selected
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val workingSets = getSelectedNodesWorkingSets<FilesWorkingSet>(view as ExplorerTreeView<*, *, *>)
    e.presentation.isEnabledAndVisible = workingSets.size == 1
  }

}
