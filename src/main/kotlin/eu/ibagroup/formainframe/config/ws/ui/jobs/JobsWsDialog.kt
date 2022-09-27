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

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.ValidationInfoBuilder
import eu.ibagroup.formainframe.common.ui.*
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.ws.JobsWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.AbstractWsDialog
import eu.ibagroup.formainframe.config.ws.ui.JobsWorkingSetDialogState
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.validateJobFilter
import javax.swing.JComponent
import javax.swing.JTextField

/**
 * Dialog of Jobs Working Set configurations.
 * @param crudable Crudable instance to change data in after dialog applied.
 * @param state state of Jobs Working Set configuration data.
 */
class JobsWsDialog(
  crudable: Crudable,
  state: JobsWorkingSetDialogState
) : AbstractWsDialog<JobsWorkingSetConfig, JobsWorkingSetDialogState.TableRow, JobsWorkingSetDialogState>(
  crudable,
  JobsWorkingSetDialogState::class.java,
  state
) {
  override val wsConfigClass = JobsWorkingSetConfig::class.java

  /**
   * TableView with Job Prefix, Owner, JobId columns for representation of jobs filters.
   * @see PrefixColumn
   * @see OwnerColumn
   * @see JobIdColumn
   */
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

  private fun fixBlankFieldsInState(
    state: JobsWorkingSetDialogState.TableRow
  ): JobsWorkingSetDialogState.TableRow {
    state.prefix = state.prefix.ifBlank { "" }
    state.owner = state.owner.ifBlank { "" }
    state.jobId = state.jobId.ifBlank { "" }
    return state
  }

  /**
   * Fills empty columns by default values. Removes duplicates from filters table.
   * @param state state modified through dialog.
   * @return applied state.
   */
  override fun onWSApplied(state: JobsWorkingSetDialogState): JobsWorkingSetDialogState {
    state.maskRow = state.maskRow.map { fixBlankFieldsInState(it) }
      .distinctBy { "pre:" + it.prefix + "owr:" + it.owner + "jid:" + it.jobId } as MutableList<JobsWorkingSetDialogState.TableRow>
    return super.onWSApplied(state)
  }

  override fun emptyTableRow(): JobsWorkingSetDialogState.TableRow = JobsWorkingSetDialogState.TableRow(
    prefix = "*",
    owner = CredentialService.instance.getUsernameByKey(state.connectionUuid) ?: ""
  )

  /**
   * Validates data in Jobs Working Set dialog table.
   * @param validationBuilder Builder that passed through Intellij Platform to build ValidationInfo.
   * @param component requestor component.
   * @return info with validation warnings and errors to display inside.
   */
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
    return tableElements.size != tableElements.map {
      "pre:" + it.prefix.ifBlank { "" } + "owr:" + it.owner.ifBlank { "" } + "jid:" + it.jobId.ifBlank { "" }
    }.distinct().size
  }

  /**
   * Class for representation Job Prefix column in Job Working Set table.
   * @see ValidatingColumnInfo
   */
  object PrefixColumn : ValidatingColumnInfo<JobsWorkingSetDialogState.TableRow>("Prefix") {

    override fun valueOf(item: JobsWorkingSetDialogState.TableRow?): String? = item?.prefix

    override fun validateOnInput(
      oldItem: JobsWorkingSetDialogState.TableRow,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      return validateJobFilter(newValue, oldItem.owner, oldItem.jobId, JTextField(newValue), false)
    }

    override fun validateEntered(item: JobsWorkingSetDialogState.TableRow, component: JComponent): ValidationInfo? {
      return validateJobFilter(item.prefix, item.owner, item.jobId, JTextField(item.prefix), false)
    }

    override fun isCellEditable(item: JobsWorkingSetDialogState.TableRow?): Boolean = true

    override fun setValue(item: JobsWorkingSetDialogState.TableRow, value: String) {
      item.prefix = value.uppercase()
    }
  }

  /**
   * Class for representation Owner column in Job Working Set table.
   * @see ValidatingColumnInfo
   */
  object OwnerColumn : ValidatingColumnInfo<JobsWorkingSetDialogState.TableRow>("Owner") {
    override fun valueOf(item: JobsWorkingSetDialogState.TableRow?): String? = item?.owner
    override fun validateOnInput(
      oldItem: JobsWorkingSetDialogState.TableRow,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      return validateJobFilter(newValue, oldItem.owner, oldItem.jobId, JTextField(newValue), false)
    }

    override fun validateEntered(item: JobsWorkingSetDialogState.TableRow, component: JComponent): ValidationInfo? {
      return validateJobFilter(item.prefix, item.owner, item.jobId, JTextField(item.owner), false)
    }

    override fun isCellEditable(item: JobsWorkingSetDialogState.TableRow?): Boolean = true

    override fun setValue(item: JobsWorkingSetDialogState.TableRow, value: String) {
      item.owner = value.uppercase()
    }
  }

  /**
   * Class for representation Job Id column in Job Working Set table.
   * @see ValidatingColumnInfo
   */
  object JobIdColumn : ValidatingColumnInfo<JobsWorkingSetDialogState.TableRow>("Job ID") {
    override fun valueOf(item: JobsWorkingSetDialogState.TableRow?): String? = item?.jobId
    override fun validateOnInput(
      oldItem: JobsWorkingSetDialogState.TableRow,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      return validateJobFilter(newValue, oldItem.owner, oldItem.jobId, JTextField(newValue), true)
    }

    override fun validateEntered(item: JobsWorkingSetDialogState.TableRow, component: JComponent): ValidationInfo? {
      return validateJobFilter(item.prefix, item.owner, item.jobId, JTextField(item.jobId), true)
    }

    override fun isCellEditable(item: JobsWorkingSetDialogState.TableRow?): Boolean = true

    override fun setValue(item: JobsWorkingSetDialogState.TableRow, value: String) {
      item.jobId = value.uppercase()
    }
  }
}
