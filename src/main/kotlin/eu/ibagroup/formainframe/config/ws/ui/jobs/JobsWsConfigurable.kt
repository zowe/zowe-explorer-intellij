/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.ws.ui.jobs

import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.config.sandboxCrudable
import eu.ibagroup.formainframe.config.ws.JobFilterState
import eu.ibagroup.formainframe.config.ws.JobsWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.AbstractWsConfigurable
import eu.ibagroup.formainframe.config.ws.ui.JobsWorkingSetDialogState
import eu.ibagroup.formainframe.utils.crudable.Crudable

/**
 * Implementation of AbstractWsConfigurable class for modifying Jobs Working Set configurations.
 * @see AbstractWsConfigurable
 */
class JobsWsConfigurable
  : AbstractWsConfigurable<JobsWorkingSetConfig, JobsWsTableModel, JobsWorkingSetDialogState>("Jobs Working Sets") {
  override val wsConfigClass = JobsWorkingSetConfig::class.java
  override val wsTableModel = JobsWsTableModel(sandboxCrudable)

  override fun emptyConfig() = JobsWorkingSetConfig()

  /**
   * Creates JobsWorkingSetDialogState based on data of JobsWorkingSetConfig.
   */
  override fun JobsWorkingSetConfig.toDialogStateAbstract() = this.toDialogState()

  /**
   * Creates and shows dialog for adding Jobs Working Set.
   * @param crudable crudable to modify after applying dialog.
   * @param state state of dialog.
   */
  override fun createAddDialog(crudable: Crudable, state: JobsWorkingSetDialogState) {
    JobsWsDialog(sandboxCrudable, state)
      .apply {
        if (showAndGet()) {
          wsTableModel.addRow(state.workingSetConfig)
          wsTableModel.reinitialize()
        }
      }
  }

  /**
   * Creates and shows dialog for editing Jobs Working Set.
   * @param selected selected working set dialog state
   */
  override fun createEditDialog(selected: JobsWorkingSetDialogState) {
    JobsWsDialog(
      sandboxCrudable,
      selected.apply { mode = DialogMode.UPDATE }).apply {
      if (showAndGet()) {
        val idx = wsTable.selectedRow
        val res = state.workingSetConfig
        wsTableModel[idx] = res
        wsTableModel.reinitialize()
      }
    }
  }


}

/**
 * Creates JobsWorkingSetDialogState based on data of JobsWorkingSetConfig.
 */
fun JobsWorkingSetConfig.toDialogState(): JobsWorkingSetDialogState {
  return JobsWorkingSetDialogState(
    uuid = this.uuid,
    connectionUuid = this.connectionConfigUuid,
    workingSetName = this.name,
    maskRow = this.jobsFilters.map { JobFilterState(it.prefix, it.owner, it.jobId) }
      .toMutableSmartList()
  )
}
