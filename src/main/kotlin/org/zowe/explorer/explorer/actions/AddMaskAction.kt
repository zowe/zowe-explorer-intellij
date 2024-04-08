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
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.config.ws.MaskStateWithWS
import org.zowe.explorer.config.ws.UssPath
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.AddOrEditMaskDialog
import org.zowe.explorer.explorer.ui.ExplorerTreeView
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.getExplorerView
import org.zowe.explorer.utils.MaskType
import org.zowe.explorer.utils.getSelectedNodesWorkingSets

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
