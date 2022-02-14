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
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.explorer.JesWorkingSet
import org.zowe.explorer.explorer.ui.*

class AddJobsFilerAction: AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(JES_EXPLORER_VIEW) ?: return

    val ws = getUnits(view).firstOrNull() ?: return
    val owner = ws.connectionConfig?.let { CredentialService.instance.getUsernameByKey(it.uuid ) } ?: ""
    val initialState = JobsFilterState(ws, "*", owner)
    val dialog = AddJobsFilterDialog(e.project, initialState)
    if (dialog.showAndGet()) {
      ws.addMask(dialog.state.toJobsFilter())
    }
  }

  override fun update(e: AnActionEvent) {
    val view = e.getData(JES_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    e.presentation.isEnabledAndVisible = getUnits(view).size == 1
  }

  private fun getUnits(view: JesExplorerView): List<JesWorkingSet> {
    return view.mySelectedNodesData.map { it.node }
      .filterIsInstance<ExplorerUnitTreeNodeBase<*, JesWorkingSet>>()
      .map { it.unit }
      .distinct()
  }

}
