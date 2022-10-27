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
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.explorer.JesWorkingSet
import eu.ibagroup.formainframe.explorer.ui.*

/**
 * Action for adding Job Filter from UI.
 * @author Valiantsin Krus
 */
class AddJobsFilerAction : AnAction() {

  /** Opens AddJobsFilterDialog and saves result. */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(JES_EXPLORER_VIEW) ?: return

    val ws = getUnits(view).firstOrNull() ?: return
    val owner = ws.connectionConfig?.let { CredentialService.instance.getUsernameByKey(it.uuid) } ?: ""
    val initialState = JobsFilterState(ws, "*", owner)
    val dialog = AddJobsFilterDialog(e.project, initialState)
    if (dialog.showAndGet()) {
      ws.addMask(dialog.state.toJobsFilter())
    }
  }

  /** Decides to show action or not. */
  override fun update(e: AnActionEvent) {
    val view = e.getData(JES_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    e.presentation.isEnabledAndVisible = getUnits(view).size == 1
  }

  /** Finds units for selected nodes in explorer. */
  private fun getUnits(view: JesExplorerView): List<JesWorkingSet> {
    return view.mySelectedNodesData.map { it.node }
      .filterIsInstance<ExplorerUnitTreeNodeBase<*, JesWorkingSet>>()
      .map { it.unit }
      .distinct()
  }

}
