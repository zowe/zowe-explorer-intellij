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

import com.intellij.openapi.Disposable
import com.intellij.ui.table.TableView
import eu.ibagroup.formainframe.utils.castOrNull
import java.awt.Dimension
import javax.swing.DefaultCellEditor
import javax.swing.table.TableCellRenderer

// TODO: doc
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

  fun getCellValidator(column: Int): ValidatingColumnInfo<Item>? {
    return listTableModel.columnInfos[convertColumnIndexToModel(column)].castOrNull()
  }

}