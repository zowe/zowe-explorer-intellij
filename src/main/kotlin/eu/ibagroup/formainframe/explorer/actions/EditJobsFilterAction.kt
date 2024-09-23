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
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.ws.JesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.JobFilterStateWithMultipleWS
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.explorer.JesWorkingSet
import eu.ibagroup.formainframe.explorer.ui.EditJobsFilterDialog
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeView
import eu.ibagroup.formainframe.explorer.ui.JesExplorerView
import eu.ibagroup.formainframe.explorer.ui.JesFilterNode
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.getSelectedNodesWorkingSets

/** Action to edit job filter in JES working set tree view */
class EditJobsFilterAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /** Save changes when the dialog is fulfilled */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: return

    val node = view.mySelectedNodesData.getOrNull(0)?.node
    if (node is JesFilterNode) {
      val workingSets = getSelectedNodesWorkingSets<JesWorkingSet>(view as ExplorerTreeView<*, *, *>)
      val ws = workingSets.firstOrNull() ?: return
      val prefix = node.value.prefix
      val owner = node.value.owner
      val jobId = node.value.jobId
      val state =
        JobFilterStateWithMultipleWS(wsList = mutableListOf(ws), prefix = prefix, owner = owner, jobId = jobId)
      val dialog = EditJobsFilterDialog(e.project, state)
      if (dialog.showAndGet()) {
        val newJobsFilter = dialog.state.toJobsFilter()
        // Is the job filter really changed
        if (prefix != newJobsFilter.prefix || owner != newJobsFilter.owner || jobId != newJobsFilter.jobId) {
          val wsToUpdate = ConfigService.getService().crudable
            .getByUniqueKey<JesWorkingSetConfig>(ws.uuid)
            ?.clone()
          if (wsToUpdate != null) {
            val changedJobFilter: JobsFilter? =
              wsToUpdate.jobsFilters
                .filter { it.prefix == prefix && it.owner == owner && it.jobId == jobId }
                .getOrNull(0)
            changedJobFilter?.prefix = newJobsFilter.prefix
            changedJobFilter?.owner = newJobsFilter.owner
            changedJobFilter?.jobId = newJobsFilter.jobId
            ConfigService.getService().crudable.update(wsToUpdate)
          }
        }
      }
    }
  }

  /**
   * Decides to show the edit jobs filter action or not.
   * Shows the action if:
   * 1. Explorer view is not null
   * 2. Only one item selected
   * 3. The selected item is a [JesFilterNode]
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    if (selected.size != 1) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val node = selected[0].node
    e.presentation.isEnabledAndVisible = node is JesFilterNode
  }

}
