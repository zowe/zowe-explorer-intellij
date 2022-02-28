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
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.config.ws.UssPath
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.WorkingSet
import org.zowe.explorer.explorer.ui.*

class AddMaskAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return

    val ws = getUnits(view).firstOrNull() ?: return
    val initialState = MaskState(ws)
    val dialog = AddMaskDialog(e.project, initialState)
    if (dialog.showAndGet()) {
      val state = dialog.state
      when (state.type) {
        MaskState.ZOS -> ws.addMask(DSMask(state.mask, mutableListOf(), "", state.isSingle))
        MaskState.USS -> ws.addUssPath(UssPath(state.mask))
      }
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }

  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    e.presentation.isEnabledAndVisible = getUnits(view).size == 1
  }

  private fun getUnits(view: GlobalFileExplorerView): List<FilesWorkingSet> {
    return view.mySelectedNodesData.map { it.node }
      .filterIsInstance<ExplorerUnitTreeNodeBase<*, FilesWorkingSet>>()
      .map { it.unit }
      .distinct()
  }

}
