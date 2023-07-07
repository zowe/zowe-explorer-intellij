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
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.config.ws.MaskStateWithWS
import org.zowe.explorer.config.ws.UssPath
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.AddOrEditMaskDialog
import org.zowe.explorer.explorer.ui.ExplorerUnitTreeNodeBase
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.getExplorerView
import org.zowe.explorer.utils.MaskType

/** Action to add USS or z/OS mask */
class AddMaskAction : AnAction() {

  /** Add mask when the dialog is fulfilled    */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: return

    val ws = getUnits(view).firstOrNull() ?: return
    val initialState = MaskStateWithWS(ws = ws)
    val dialog = AddOrEditMaskDialog(e.project, "Create Mask", initialState)
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

  /** Decides to show action or not */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    e.presentation.isEnabledAndVisible = getUnits(view).size == 1
  }

  /** Finds files working set units for selected nodes in explorer */
  private fun getUnits(view: FileExplorerView): List<FilesWorkingSet> {
    return view.mySelectedNodesData
      .map { it.node }
      .filterIsInstance<ExplorerUnitTreeNodeBase<ConnectionConfig, *, FilesWorkingSet>>()
      .map { it.unit }
      .distinct()
  }

}
