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

import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.JobFilterStateWithWS
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.config.ws.JesWorkingSetConfig
import eu.ibagroup.formainframe.explorer.ui.EditJobsFilterDialog
import eu.ibagroup.formainframe.explorer.ui.JES_EXPLORER_VIEW
import eu.ibagroup.formainframe.explorer.ui.JesFilterNode
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey

/** Action to edit job filter in JES working set tree view */
class EditJobsFilterAction : JobsFilterAction() {

  /** Save changes when the dialog is fulfilled */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(JES_EXPLORER_VIEW) ?: return

    val node = view.mySelectedNodesData.getOrNull(0)?.node
    if (node is JesFilterNode) {
      val ws = getUnits(view).firstOrNull() ?: return
      val prefix = node.value.prefix
      val owner = node.value.owner
      val jobId = node.value.jobId
      val state = JobFilterStateWithWS(ws = ws, prefix = prefix, owner = owner, jobId = jobId)
      val dialog = EditJobsFilterDialog(e.project, state)
      if (dialog.showAndGet()) {
        val newJobsFilter = dialog.state.toJobsFilter()
        // Is the job filter really changed
        if (prefix != newJobsFilter.prefix || owner != newJobsFilter.owner || jobId != newJobsFilter.jobId) {
          val wsToUpdate = configCrudable.getByUniqueKey<JesWorkingSetConfig>(ws.uuid)?.clone()
          if (wsToUpdate != null) {
            val changedJobFilter: JobsFilter? =
              wsToUpdate.jobsFilters
                .filter { it.prefix == prefix && it.owner == owner && it.jobId == jobId }
                .getOrNull(0)
            changedJobFilter?.prefix = newJobsFilter.prefix
            changedJobFilter?.owner = newJobsFilter.owner
            changedJobFilter?.jobId = newJobsFilter.jobId
            configCrudable.update(wsToUpdate)
          }
        }
      }
    }
  }

}
