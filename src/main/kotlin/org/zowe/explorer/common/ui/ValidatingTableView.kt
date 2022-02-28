/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.common.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.table.TableView
import org.zowe.explorer.utils.castOrNull
import java.awt.Dimension
import javax.swing.DefaultCellEditor
import javax.swing.JComponent
import javax.swing.table.TableCellRenderer

class ValidatingTableView<Item>(
  model: ValidatingListTableModel<Item>,
  val disposable: Disposable
) : TableView<Item>(model) {

  override fun getCellRenderer(row: Int, column: Int): TableCellRenderer? {
    return super.getCellRenderer(row, column)?.apply {
        val editor = getCellEditor(row, column)
        if (editor is DefaultCellEditor) {
          preferredSize = with(preferredSize) {
            Dimension(this.width, this.height.coerceAtLeast(editor.component.preferredSize.height))
          }
        }
      }
  }

  @Suppress("UNCHECKED_CAST")
  override fun getListTableModel(): ValidatingListTableModel<Item> {
    return super.getModel() as ValidatingListTableModel<Item>
  }

  private val validationInfos
    get() = listTableModel.validationInfos

  fun <Component : JComponent> getValidationCallback(): ValidationInfoBuilder.(Component) -> ValidationInfo? {
    return { component ->
      val validationInfoComponentPair = validationInfos
        .asMap
        .entries
        .minByOrNull { if (it.value.warning) 1 else 0 }
      if (validationInfoComponentPair != null) {
        val validationInfo = validationInfoComponentPair.value
        val cell = validationInfoComponentPair.key
        editCellAt(cell.first, cell.second)
        if (validationInfo.warning) {
          ValidationInfoBuilder(validationInfo.component ?: component).warning(validationInfo.message)
        } else {
          ValidationInfoBuilder(validationInfo.component ?: component).error(validationInfo.message)
        }
      } else null
    }
  }

  fun getCellValidator(column: Int): ValidatingColumnInfo<Item>? {
    return listTableModel.columnInfos[convertColumnIndexToModel(column)].castOrNull()
  }

}
