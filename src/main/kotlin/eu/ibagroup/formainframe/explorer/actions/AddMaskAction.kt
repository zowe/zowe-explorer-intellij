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
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.MaskStateWithWS
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.explorer.ui.AddMaskDialog
import eu.ibagroup.formainframe.explorer.ui.ExplorerUnitTreeNodeBase
import eu.ibagroup.formainframe.explorer.ui.FILE_EXPLORER_VIEW
import eu.ibagroup.formainframe.explorer.ui.FileExplorerView
import eu.ibagroup.formainframe.utils.MaskType

/** Action to add USS or z/OS mask */
class AddMaskAction : AnAction() {

  /** Add mask when the dialog is fulfilled    */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return

    val ws = getUnits(view).firstOrNull() ?: return
    val initialState = MaskStateWithWS(ws)
    val dialog = AddMaskDialog(e.project, initialState)
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
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    e.presentation.isEnabledAndVisible = getUnits(view).size == 1
  }

  /** Finds files working set units for selected nodes in explorer */
  private fun getUnits(view: FileExplorerView): List<FilesWorkingSet> {
    return view.mySelectedNodesData
      .map { it.node }
      .filterIsInstance<ExplorerUnitTreeNodeBase<*, FilesWorkingSet>>()
      .map { it.unit }
      .distinct()
  }

}
