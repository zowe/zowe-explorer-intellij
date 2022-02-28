/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.ws.ui.jobs

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.ValidationInfoBuilder
import org.zowe.explorer.common.ui.*
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.ws.JobsWorkingSetConfig
import org.zowe.explorer.config.ws.ui.AbstractWsDialog
import org.zowe.explorer.config.ws.ui.JobsWorkingSetDialogState
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.validateJobFilter
import javax.swing.JComponent

class JobsWsDialog(
  crudable: Crudable,
  state: JobsWorkingSetDialogState
) : AbstractWsDialog<JobsWorkingSetConfig, JobsWorkingSetDialogState.TableRow, JobsWorkingSetDialogState>(
  crudable,
  JobsWorkingSetDialogState::class.java,
  state
) {
  override val wsConfigClass = JobsWorkingSetConfig::class.java

  override val masksTable = ValidatingTableView(
    ValidatingListTableModel(PrefixColumn, OwnerColumn, JobIdColumn).apply {
      items = state.maskRow
    },
    disposable
  ).apply { rowHeight = DEFAULT_ROW_HEIGHT }

  override val tableTitle = "Job Filters included in Working Set"

  override val wsNameLabel = "Jobs Working Set Name"

  override fun init() {
    title = when (state.mode) {
      DialogMode.CREATE -> "Add Jobs Working Set"
      else -> "Edit Jobs Working Set"
    }
    super.init()
  }

  init {
    init()
  }

  private fun fixEmptyFieldsInState(
    state: JobsWorkingSetDialogState.TableRow,
    connectionUuid: String
  ): JobsWorkingSetDialogState.TableRow {
    if (state.jobId.isEmpty()) {
      if (state.owner.isEmpty()) {
        state.owner = CredentialService.instance.getUsernameByKey(connectionUuid) ?: ""
      }
      if (state.prefix.isEmpty()) {
        state.prefix = "*"
      }
    }
    return state
  }

  override fun onWSApplyed(state: JobsWorkingSetDialogState): JobsWorkingSetDialogState {
    state.maskRow = state.maskRow.map { fixEmptyFieldsInState(it, state.connectionUuid) }
      .distinctBy { "pre:" + it.prefix + "owr:" + it.owner + "jid:" + it.jobId } as MutableList<JobsWorkingSetDialogState.TableRow>
    return super.onWSApplyed(state)
  }

  override fun emptyTableRow(): JobsWorkingSetDialogState.TableRow = JobsWorkingSetDialogState.TableRow()

  override fun validateOnApply(validationBuilder: ValidationInfoBuilder, component: JComponent): ValidationInfo? {
    return when {
      masksTable.listTableModel.validationInfos.asMap.isNotEmpty() -> {
        ValidationInfo("Fix errors in the table and try again", component)
      }
      masksTable.listTableModel.rowCount == 0 -> {
        validationBuilder.warning("You are going to create a Working Set that doesn't fetch anything")
      }
      hasDuplicatesInTable(masksTable.items) -> {
        ValidationInfo("You cannot add several identical job filters to table")
      }
      else -> null
    }
  }

  private fun hasDuplicatesInTable(tableElements: MutableList<JobsWorkingSetDialogState.TableRow>): Boolean {
    return tableElements.size != tableElements.map { "pre:" + it.prefix + "owr:" + it.owner + "jid:" + it.jobId }
      .distinct().size
  }

  object PrefixColumn : ValidatingColumnInfo<JobsWorkingSetDialogState.TableRow>("Prefix") {
    override fun valueOf(item: JobsWorkingSetDialogState.TableRow?): String? = item?.prefix
    override fun validateOnInput(
      oldItem: JobsWorkingSetDialogState.TableRow,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      return validateJobFilter(newValue, oldItem.owner, oldItem.jobId, component)
    }

    override fun validateEntered(item: JobsWorkingSetDialogState.TableRow, component: JComponent): ValidationInfo? {
      return validateJobFilter(item.prefix, item.owner, item.jobId, component)
    }

    override fun isCellEditable(item: JobsWorkingSetDialogState.TableRow?): Boolean = true

    override fun setValue(item: JobsWorkingSetDialogState.TableRow, value: String) {
      item.prefix = value
    }
  }

  object OwnerColumn : ValidatingColumnInfo<JobsWorkingSetDialogState.TableRow>("Owner") {
    override fun valueOf(item: JobsWorkingSetDialogState.TableRow?): String? = item?.owner
    override fun validateOnInput(
      oldItem: JobsWorkingSetDialogState.TableRow,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      return validateJobFilter(oldItem.prefix, newValue, oldItem.jobId, component)
    }

    override fun validateEntered(item: JobsWorkingSetDialogState.TableRow, component: JComponent): ValidationInfo? {
      return validateJobFilter(item.prefix, item.owner, item.jobId, component)
    }

    override fun isCellEditable(item: JobsWorkingSetDialogState.TableRow?): Boolean = true

    override fun setValue(item: JobsWorkingSetDialogState.TableRow, value: String) {
      item.owner = value
    }
  }

  object JobIdColumn : ValidatingColumnInfo<JobsWorkingSetDialogState.TableRow>("Job ID") {
    override fun valueOf(item: JobsWorkingSetDialogState.TableRow?): String? = item?.jobId
    override fun validateOnInput(
      oldItem: JobsWorkingSetDialogState.TableRow,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      return validateJobFilter(oldItem.prefix, oldItem.owner, newValue, component)
    }

    override fun validateEntered(item: JobsWorkingSetDialogState.TableRow, component: JComponent): ValidationInfo? {
      return validateJobFilter(item.prefix, item.owner, item.jobId, component)
    }

    override fun isCellEditable(item: JobsWorkingSetDialogState.TableRow?): Boolean = true

    override fun setValue(item: JobsWorkingSetDialogState.TableRow, value: String) {
      item.jobId = value
    }
  }
}
