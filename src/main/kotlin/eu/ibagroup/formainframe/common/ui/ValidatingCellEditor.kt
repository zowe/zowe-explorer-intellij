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

package eu.ibagroup.formainframe.common.ui

import com.intellij.ui.components.JBTextField
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.runIfTrue
import java.awt.Component
import javax.swing.DefaultCellEditor
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.JTextField

/**
 * Validating cell editor. Needed for cells with validation
 */
class ValidatingCellEditor<Item> : DefaultCellEditor(JBTextField()) {

  /**
   * Get table cell editor component with the custom validator on each cell
   * @param table the table to assign validators on columns of it
   * @param row the row number to find the cell to assign the validator to
   * @param column the column number to find the cell to assign the validator to
   * @return the updated editor component
   */
  @Suppress("UNCHECKED_CAST")
  override fun getTableCellEditorComponent(
    table: JTable?,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ): Component? {
    return super.getTableCellEditorComponent(table, value, isSelected, row, column)?.apply component@{
      if (table is ValidatingTableView<*> && this is JComponent) {
        table.castOrNull<ValidatingTableView<Item>>()?.apply {
          getCellValidator(column)?.let { validator ->
            ComponentValidatorBuilder(this@component, table.disposable)
              .setup {
                withValidator { ->
                  with(table as ValidatingTableView<Item>) {
                    (editingRow in 0 until rowCount).runIfTrue {
                      getRow(editingRow)?.let { value ->
                        validator.validateOnInput(
                          value,
                          this@ValidatingCellEditor.component.castOrNull<JTextField>()!!.text,
                          component
                        )
                      }
                    }.also {
                      listTableModel.validationInfos[editingRow, editingColumn] = it
                    }
                  }
                }
              }.finish { }
          }
        }
      }
    }
  }
}
