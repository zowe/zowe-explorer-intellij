/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
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

class ValidatingCellEditor<Item> : DefaultCellEditor(JBTextField()) {

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
                    (editingRow in 0 until columnCount).runIfTrue {
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
