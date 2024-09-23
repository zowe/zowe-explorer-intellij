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

package eu.ibagroup.formainframe.config.ws.ui.jes

import com.intellij.openapi.components.service
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.ValidationInfoBuilder
import eu.ibagroup.formainframe.common.ui.DEFAULT_ROW_HEIGHT
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.ValidatingColumnInfo
import eu.ibagroup.formainframe.common.ui.ValidatingListTableModel
import eu.ibagroup.formainframe.common.ui.ValidatingTableView
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.ws.JesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.JobFilterState
import eu.ibagroup.formainframe.config.ws.ui.AbstractWsDialog
import eu.ibagroup.formainframe.config.ws.ui.JesWorkingSetDialogState
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.validateJobFilter
import javax.swing.JComponent
import javax.swing.JTextField

/**
 * Dialog of JES Working Set configurations.
 * @param crudable Crudable instance to change data in after dialog applied.
 * @param state state of JES Working Set configuration data.
 * @property isSingleConnectionOnlyAllowed overrides default property assignment in super class, allows creating specific binding for panel
 * @property connectionConfigToSelect overrides default property assignment in super class
 */
class JesWsDialog(
  crudable: Crudable,
  state: JesWorkingSetDialogState,
  override val isSingleConnectionOnlyAllowed: Boolean = false,
  override val connectionConfigToSelect: ConnectionConfig? = null,
) : AbstractWsDialog<ConnectionConfig, JesWorkingSetConfig, JobFilterState, JesWorkingSetDialogState>(
  crudable,
  JesWorkingSetDialogState::class.java,
  state
) {
  override val wsConfigClass = JesWorkingSetConfig::class.java
  override val connectionClass = ConnectionConfig::class.java

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

  override val wsNameLabel = "JES Working Set Name"

  override fun init() {
    title = when (state.mode) {
      DialogMode.CREATE -> "Add JES Working Set"
      else -> "Edit JES Working Set"
    }
    super.init()
  }

  init {
    init()
  }

  private fun fixBlankFieldsInState(
    state: JobFilterState
  ): JobFilterState {
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
  override fun onWSApplied(state: JesWorkingSetDialogState): JesWorkingSetDialogState {
    state.maskRow = state.maskRow.map { fixBlankFieldsInState(it) }
      .distinctBy { "pre:" + it.prefix + "owr:" + it.owner + "jid:" + it.jobId } as MutableList<JobFilterState>
    return super.onWSApplied(state)
  }

  override fun emptyTableRow(): JobFilterState = JobFilterState(
    prefix = "*",
    owner = CredentialService.getService().getUsernameByKey(state.connectionUuid) ?: ""
  )

  /**
   * Validates data in JES Working Set dialog table.
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

  private fun hasDuplicatesInTable(tableElements: MutableList<JobFilterState>): Boolean {
    return tableElements.size != tableElements.map {
      "pre:" + it.prefix.ifBlank { "" } + "owr:" + it.owner.ifBlank { "" } + "jid:" + it.jobId.ifBlank { "" }
    }.distinct().size
  }

  /**
   * Class for representation Job Prefix column in JES Working Set table.
   * @see ValidatingColumnInfo
   */
  object PrefixColumn : ValidatingColumnInfo<JobFilterState>("Prefix") {

    override fun valueOf(item: JobFilterState?): String? = item?.prefix

    override fun validateOnInput(
      oldItem: JobFilterState,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      return validateJobFilter(newValue, oldItem.owner, oldItem.jobId, JTextField(newValue), false)
    }

    override fun validateEntered(item: JobFilterState, component: JComponent): ValidationInfo? {
      return validateJobFilter(item.prefix, item.owner, item.jobId, JTextField(item.prefix), false)
    }

    override fun isCellEditable(item: JobFilterState?): Boolean = true

    override fun setValue(item: JobFilterState, value: String) {
      item.prefix = value.uppercase()
    }
  }

  /**
   * Class for representation Owner column in JES Working Set table.
   * @see ValidatingColumnInfo
   */
  object OwnerColumn : ValidatingColumnInfo<JobFilterState>("Owner") {
    override fun valueOf(item: JobFilterState?): String? = item?.owner
    override fun validateOnInput(
      oldItem: JobFilterState,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      return validateJobFilter(newValue, oldItem.owner, oldItem.jobId, JTextField(newValue), false)
    }

    override fun validateEntered(item: JobFilterState, component: JComponent): ValidationInfo? {
      return validateJobFilter(item.prefix, item.owner, item.jobId, JTextField(item.owner), false)
    }

    override fun isCellEditable(item: JobFilterState?): Boolean = true

    override fun setValue(item: JobFilterState, value: String) {
      item.owner = value.uppercase()
    }
  }

  /**
   * Class for representation Job Id column in JES Working Set table.
   * @see ValidatingColumnInfo
   */
  object JobIdColumn : ValidatingColumnInfo<JobFilterState>("Job ID") {
    override fun valueOf(item: JobFilterState?): String? = item?.jobId
    override fun validateOnInput(
      oldItem: JobFilterState,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      return validateJobFilter(newValue, oldItem.owner, oldItem.jobId, JTextField(newValue), true)
    }

    override fun validateEntered(item: JobFilterState, component: JComponent): ValidationInfo? {
      return validateJobFilter(item.prefix, item.owner, item.jobId, JTextField(item.jobId), true)
    }

    override fun isCellEditable(item: JobFilterState?): Boolean = true

    override fun setValue(item: JobFilterState, value: String) {
      item.jobId = value.uppercase()
    }
  }
}
