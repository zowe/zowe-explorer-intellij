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

package eu.ibagroup.formainframe.config.ws.ui.files

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ComboBoxCellEditor
import eu.ibagroup.formainframe.common.ui.*
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.MaskState
import eu.ibagroup.formainframe.config.ws.ui.AbstractWsDialog
import eu.ibagroup.formainframe.config.ws.ui.FilesWorkingSetDialogState
import eu.ibagroup.formainframe.dataops.exceptions.CredentialsNotFoundForConnectionException
import eu.ibagroup.formainframe.utils.MaskType
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.validateDatasetMask
import eu.ibagroup.formainframe.utils.validateForBlank
import eu.ibagroup.formainframe.utils.validateUssMask
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

/**
 * Dialog of Files Working Set configurations.
 * @param crudable Crudable instance to change data in after dialog applied.
 * @param state state of Files Working Set configuration data.
 * @author Valiantsin Krus
 * @author Viktar Mushtsin
 */
class FilesWorkingSetDialog(
  private val crudable: Crudable,
  state: FilesWorkingSetDialogState
) : AbstractWsDialog<ConnectionConfig, FilesWorkingSetConfig, MaskState, FilesWorkingSetDialogState>(
  crudable,
  FilesWorkingSetDialogState::class.java,
  state
) {

  override val wsConfigClass = FilesWorkingSetConfig::class.java
  override val tableTitle = "DS Masks included in Working Set"
  override val wsNameLabel = "Working Set Name"
  private val comboBoxMap = HashMap<MaskState, ComboBoxCellEditor>()

  override val connectionClass = ConnectionConfig::class.java

  /**
   * Class for representation File Mask column in Files Working Set table.
   * @see ValidatingColumnInfo
   */
  inner class MaskColumn : ValidatingColumnInfo<MaskState>("Mask") {

    override fun valueOf(item: MaskState): String {
      return item.mask
    }

    /**
     * Set the mask value. If it is the z/OS mask, then the value will be transformed to uppercase.
     * If the value has the trailing slash, it will be deleted
     * @param item the mask state item to save mask value in
     * @param value the new mask value to save
     */
    override fun setValue(item: MaskState, value: String) {
      val editedCaseValue = if (item.type == MaskType.ZOS) value.uppercase() else value
      item.mask =
        if (editedCaseValue.length > 1 && editedCaseValue.endsWith("/")) editedCaseValue.substringBeforeLast("/") else editedCaseValue
    }

    override fun isCellEditable(item: MaskState?): Boolean {
      return true
    }

    /**
     * Validate mask value on input. It will change the mask type to USS in case if the value includes slash.
     * The z/OS type will be selected in other case. In case if the user already proceeded with the manual type selection, the input will not affect the type anymore
     * @param oldItem the mask state to check if the type is not selected manually yet and to change the type in positive case
     * @param newValue the new input string
     * @param component the component where the input is proceeded
     */
    override fun validateOnInput(
      oldItem: MaskState,
      newValue: String,
      component: JComponent
    ): ValidationInfo? {
      if (!oldItem.isTypeSelectedManually) {
        oldItem.type = if (newValue.contains("/")) MaskType.USS else MaskType.ZOS
        oldItem.isTypeSelectedAutomatically = true
        masksTable.repaint()
      }
      oldItem.mask = newValue
      return null
    }

    /**
     * Validate entered mask name
     * @param item the mask state to validate the mask name
     * @param component the component to show the error message on in case if the validation is not succeeded
     */
    override fun validateEntered(item: MaskState, component: JComponent): ValidationInfo? {
      return validateForBlank(item.mask, component)
        ?: if (item.type == MaskType.ZOS) validateDatasetMask(item.mask, component)
        else validateUssMask(item.mask, component)
    }

  }

  /**
   * ComboBox Editor for System Type in masks table.
   * If the mask type is selected by user, then the input of the mask name should not take any effect on the type anymore, so the manual selection flag is set to true
   * @param item the mask state to update the type selection flags
   */
  class ComboBoxCellEditorImpl(val item: MaskState) : ComboBoxCellEditor() {

    override fun getComboBoxItems(): MutableList<String> {
      return mutableListOf(MaskType.ZOS.stringType, MaskType.USS.stringType)
    }

    init {
      component.addPropertyChangeListener {
        if (!item.isTypeSelectedAutomatically) {
          item.isTypeSelectedManually = true
        } else {
          item.isTypeSelectedAutomatically = false
        }
      }
    }

  }

  /**
   * Class for representation System Type (USS, z/OS) column in Files Working Set table.
   * @see ValidatingColumnInfo
   */
  inner class TypeColumn : ColumnInfo<MaskState, String>("Type") {

    /**
     * Get type cell editor. Creates the combo box component with the mask state provided to set up the selection listener
     * @param item the mask state to track and change the flags
     */
    override fun getEditor(item: MaskState): TableCellEditor {
      val comboBoxItem = comboBoxMap[item] ?: ComboBoxCellEditorImpl(item)
      comboBoxMap[item] = comboBoxItem
      comboBoxMap[item]?.clickCountToStart = 1
      return comboBoxItem
    }

    override fun getRenderer(item: MaskState): TableCellRenderer {
      return ValidatingCellRenderer<MaskState>()
    }

    override fun setValue(item: MaskState, value: String) {
      item.type = MaskType.values().find { it.stringType == value } ?: item.type
    }

    override fun isCellEditable(item: MaskState): Boolean {
      return true
    }

    override fun valueOf(item: MaskState): String {
      return item.type.stringType
    }

    override fun getWidth(table: JTable): Int {
      return 100
    }

  }

  /**
   * TableView with Mask, System Type columns for representation of files masks.
   * @see MaskColumn
   * @see TypeColumn
   */
  override val masksTable = ValidatingTableView(
    ValidatingListTableModel(MaskColumn(), TypeColumn())
      .apply { items = state.maskRow },
    disposable
  )
    .apply { rowHeight = DEFAULT_ROW_HEIGHT }

  override fun init() {
    title = when (state.mode) {
      DialogMode.CREATE -> "Add Working Set"
      else -> "Edit Working Set"
    }
    super.init()
  }

  init {
    init()
  }

  /**
   * Checks if masks table has duplicated rows.
   * @param tableElements the table elements to check masks duplication in
   */
  private fun hasDuplicatesInTable(tableElements: List<MaskState>): Boolean {
    return tableElements.size != tableElements.map { it.mask }.distinct().size
  }

  /**
   * Validates data in Files Working Set dialog table.
   * @param validationBuilder Builder that passed through Intellij Platform to build ValidationInfo.
   * @param component requester component.
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
        ValidationInfo("You cannot add several identical masks to table")
      }

      else -> null
    }
  }

  /**
   * Returns mask state with predefined mask name as "<HLQ>.*"
   */
  override fun emptyTableRow(): MaskState {
    val config = crudable.getByUniqueKey<ConnectionConfig>(state.connectionUuid)
    var initialMask = ""
    try {
      if (config != null) {
        val username = CredentialService.getUsername(config)
        initialMask = "$username.*"
      }
    } catch (_: CredentialsNotFoundForConnectionException) {
    }
    return MaskState(initialMask)
  }

}
