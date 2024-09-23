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

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import eu.ibagroup.formainframe.utils.SparseMatrix
import javax.swing.SortOrder

/**
 * Class which serves as validator for sparse matrix items in Job Filters and WS Masks tables to add
 */
open class ValidatingListTableModel<Item> : ListTableModel<Item> {

  constructor(vararg columnInfos: ColumnInfo<Item, *>?) : super(*columnInfos)

  constructor(
    columnNames: Array<out ColumnInfo<Item, *>>,
    items: MutableList<Item>,
    selectedColumn: Int
  ) : super(columnNames, items, selectedColumn)

  constructor(columnNames: Array<out ColumnInfo<Item, *>>, items: MutableList<Item>) : super(
    columnNames,
    items
  )

  constructor(
    columnNames: Array<out ColumnInfo<Item, *>>,
    items: MutableList<Item>,
    selectedColumn: Int,
    order: SortOrder
  ) : super(columnNames, items, selectedColumn, order)

  val validationInfos = SparseMatrix<ValidationInfo>()

  /**
   * Method which is always called when new entry is added to WS Mask or Job Filter
   * @param row - row to validate
   * @param columnName - column name by which validation is going to be performed
   */
  fun getValidationInfoByColumnName(row: Int, columnName: String): ValidationInfo? {
    val index = this.columnInfos.indexOfFirst { it.name == columnName }
    return if (index >= 0) {
      validationInfos[row, index]
    } else {
      null
    }
  }

  /**
   * Getter method to get a full Item by row number
   * @param row - row number to retrieve an Item
   */
  operator fun get(row: Int): Item {
    return this.getItem(row)
  }

  /**
   * Setter method to set an Item to specified row number
   * @param row - row number to set an Item
   * @param item - Item object to set
   */
  open operator fun set(row: Int, item: Item) {
    (0 until columnCount).forEach { columnIndex ->
      val info = columnInfos[columnIndex]
      setValueAt(info.valueOf(item), row, columnIndex, false)
    }
    fireTableRowsUpdated(row, row)
  }

  /**
   * Method is called when user deletes selected row from table
   * @param idx - row index to delete
   */
  override fun removeRow(idx: Int) {
    validationInfos.clear()
    super.removeRow(idx)
  }

}
