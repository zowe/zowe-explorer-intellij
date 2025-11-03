/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Dzianis Lisiankou
 */

package org.zowe.explorer.v3.ui.table

import com.intellij.util.ui.ColumnInfo
import org.zowe.explorer.common.ui.ValidatingListTableModel
import org.zowe.explorer.common.ui.rows
import javax.swing.event.TableModelEvent

abstract class TableModel<Item : Any>(
  vararg columnInfos: ColumnInfo<Item, *>?
) : ValidatingListTableModel<Item>(*columnInfos) {

  abstract val clazz: Class<out Item>

  private var itemToDelete: Item? = null

  // TODO: need it for delete?
//  private var replacingItems: List<Item>? = null

//  private var needToUpdateCache = false

//  override fun setItems(items: MutableList<Item>) {
//    replacingItems = getItems().cloneElements(clazz)
//    super.setItems(items)
//  }

//  override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int, notifyListeners: Boolean) {
//    if (notifyListeners) {
//      replacingItems = this[rowIndex].cloneAndWrap(clazz)
//    }
//    super.setValueAt(aValue, rowIndex, columnIndex, notifyListeners)
//  }

  // TODO: operator?
//  override fun set(row: Int, item: Item) {
//    replacingItems = this[row].cloneAndWrap(clazz)
//    super.set(row, item)
//  }

  override fun removeRow(idx: Int) {
//    replacingItems = this[idx].cloneAndWrap(clazz)
    itemToDelete = this[idx]
    super.removeRow(idx)
  }

  abstract fun fetch(): MutableList<Item>

  abstract fun onAdd(value: Item): Boolean

  abstract fun onUpdate(value: Item): Boolean

  abstract fun onDelete(value: Item)

//  abstract fun onApplyingMergedCollection(merged: MergedCollections<Item>)

  fun reinitialize() {
//    needToUpdateCache = false
    items = fetch()
//    needToUpdateCache = true
  }

  protected fun initialize() {
    reinitialize()
    addTableModelListener {
      if (
        it.firstRow != TableModelEvent.HEADER_ROW
        && !(it.firstRow == 0 && it.lastRow == Int.MAX_VALUE && it.column == TableModelEvent.ALL_COLUMNS)
        ) {
        when (it.type) {
          // TODO: boolean result from onAdd? onUpdate?
          TableModelEvent.INSERT -> {
//            it.rows
//              .map { rowIndex -> Pair(rowIndex, onAdd(this[rowIndex])) }
//              .filter { pair -> !pair.second }
//              .forEachIndexed { index, pair -> removeRow(pair.first - index) }
            it.rows.forEach { rowIndex -> onAdd(this[rowIndex]) }
          }

          TableModelEvent.UPDATE -> {
//            if (it.firstRow == 0 && it.lastRow == Int.MAX_VALUE && it.column == TableModelEvent.ALL_COLUMNS && needToUpdateCache) {
//              replacingItems?.let { oldItems -> onApplyingMergedCollection(mergeCollections(oldItems, items)) }
//            } else {
//              (it.firstRow until rowCount)
//                .map { rowIndex -> Pair(rowIndex, onUpdate(this[rowIndex])) }
//                .filter { pair -> !pair.second }
//                .forEachIndexed { index, pair -> removeRow(pair.first - index) }
              it.rows.forEach { rowIndex -> onUpdate(this[rowIndex]) }
//            }
          }

          TableModelEvent.DELETE -> {
//            replacingItems?.forEach { toDelete -> onDelete(toDelete) }
//            replacingItems?.forEach { item -> onDelete(item) }
            itemToDelete?.let { item -> onDelete(item) }
          }
        }
      }
    }
  }

}