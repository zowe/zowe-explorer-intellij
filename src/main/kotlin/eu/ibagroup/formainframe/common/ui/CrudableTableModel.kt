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

import com.intellij.util.ui.ColumnInfo
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.MergedCollections
import javax.swing.event.TableModelEvent

/**
 * Abstract crudable class for table model. Provides functions to work with tables
 * @param crudable Crudable instance to change data
 * @param columnInfos column info
 */
abstract class CrudableTableModel<Item : Any>(
  private val crudable: Crudable,
  vararg columnInfos: ColumnInfo<Item, *>?
) :
  ValidatingListTableModel<Item>(*columnInfos) {

  var replacingItems: List<Item>? = null

  private fun Item?.cloneAndWrap(): List<Item> {
    val clonedItem = this?.clone(clazz)
    return if (clonedItem != null) {
      listOf(clonedItem)
    } else listOf()
  }

  /**
   * Method is called when user deletes selected row from table
   * @param idx row index to delete
   */
  override fun removeRow(idx: Int) {
    replacingItems = this[idx].cloneAndWrap()
    super.removeRow(idx)
  }

  /**
   * Setter method to set an Item to specified row number
   * @param row row number to set an Item
   * @param item Item object to set
   */
  override operator fun set(row: Int, item: Item) {
    replacingItems = this[row].cloneAndWrap()
    super.set(row, item)
  }

  /**
   * Setter method to set a list of Item
   * @param items list of Item object to set
   */
  override fun setItems(items: MutableList<Item>) {
    replacingItems = getItems().cloneElements()
    super.setItems(items)
  }

  /**
   * Sets the value in the cell at columnIndex and rowIndex to aValue.
   * This method allows to choose will the model listeners notified or not.
   * @param aValue the new value
   * @param rowIndex the row whose value is to be changed
   * @param columnIndex the column whose value is to be changed
   * @param notifyListeners indicates whether the model listeners are notified
   */
  override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int, notifyListeners: Boolean) {
    if (notifyListeners) {
      replacingItems = this[rowIndex].cloneAndWrap()
    }
    super.setValueAt(aValue, rowIndex, columnIndex, notifyListeners)
  }

  private var needToUpdateCrudableOnSetItems = true

  /**
   * Fetch list of items
   */
  fun reinitialize() {
    needToUpdateCrudableOnSetItems = false
    items = fetch(crudable)
    needToUpdateCrudableOnSetItems = true
  }

  /**
   * Fetch list of items. Contains listener for an insert, update and delete events
   * @see reinitialize
   */
  protected fun initialize() {
    reinitialize()
    addTableModelListener {
      if (it.firstRow != TableModelEvent.HEADER_ROW) {
        when (it.type) {
          TableModelEvent.INSERT -> it.rows.map { rowIndex ->
            Pair(rowIndex, onAdd(crudable, this[rowIndex]))
          }.filter { pair ->
            !pair.second
          }.forEachIndexed { index, pair ->
            removeRow(pair.first - index)
          }
          TableModelEvent.UPDATE -> {
            if (it.firstRow == 0
              && it.lastRow == Int.MAX_VALUE
              && it.column == TableModelEvent.ALL_COLUMNS
              && needToUpdateCrudableOnSetItems
            ) {
              replacingItems?.let { oldItems ->
                onApplyingMergedCollection(crudable, Crudable.mergeCollections(oldItems, items))
              }
            } else {
              (it.firstRow until rowCount).map { rowIndex ->
                Pair(rowIndex, onUpdate(crudable, this[rowIndex]))
              }.filter { pair ->
                !pair.second
              }.forEachIndexed { index, pair ->
                removeRow(pair.first - index)
              }
            }
          }
          TableModelEvent.DELETE -> replacingItems?.forEach { deleting -> onDelete(crudable, deleting) }
        }
      }
    }

  }

  private fun List<Item>.cloneElements(): List<Item> {
    return mapNotNull { it.clone(clazz) }
  }

  abstract fun fetch(crudable: Crudable): MutableList<Item>

  abstract fun onAdd(crudable: Crudable, value: Item): Boolean

  abstract fun onUpdate(crudable: Crudable, value: Item): Boolean

  abstract fun onDelete(crudable: Crudable, value: Item)

  abstract fun onApplyingMergedCollection(crudable: Crudable, merged: MergedCollections<Item>)

  abstract val clazz: Class<out Item>

}
