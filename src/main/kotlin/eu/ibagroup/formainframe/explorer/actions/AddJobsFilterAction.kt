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
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.ws.JobFilterStateWithMultipleWS
import eu.ibagroup.formainframe.explorer.JesWorkingSet
import eu.ibagroup.formainframe.explorer.ui.AddJobsFilterDialog
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeView
import eu.ibagroup.formainframe.explorer.ui.JesExplorerView
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import eu.ibagroup.formainframe.utils.getSelectedNodesWorkingSets

/**
 * Action for adding Job Filter from UI.
 * @author Valiantsin Krus
 */
class AddJobsFilterAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /** Opens AddJobsFilterDialog and saves result. */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: return
    val workingSets = getSelectedNodesWorkingSets<JesWorkingSet>(view as ExplorerTreeView<*, *, *>)
    val ws = workingSets.firstOrNull() ?: return
    val owner = ws.connectionConfig?.let { CredentialService.getService().getUsernameByKey(it.uuid) } ?: ""
    val initialState = JobFilterStateWithMultipleWS(wsList = mutableListOf(ws), owner = owner)
    val dialog = AddJobsFilterDialog(e.project, initialState)
    if (dialog.showAndGet()) {
      ws.addMask(dialog.state.toJobsFilter())
    }
  }

  /**
   * Decides to show the add jobs filter action or not.
   * Shows the action if:
   * 1. Explorer view is not null
   * 2. Item(s) from them same JES working set is(are) selected
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val workingSets = getSelectedNodesWorkingSets<JesWorkingSet>(view as ExplorerTreeView<*, *, *>)
    e.presentation.isEnabledAndVisible = workingSets.size == 1
  }

}
