package eu.ibagroup.formainframe.common.ui

import com.intellij.util.ui.ColumnInfo
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.MergedCollections
import javax.swing.SortOrder
import javax.swing.event.TableModelEvent

abstract class CrudableTableModel<Item> : ValidatingListTableModel<Item> {

  private val crudable: Crudable

  constructor(crudable: Crudable, vararg columnInfos: ColumnInfo<Item, *>?) : super(*columnInfos) {
    this.crudable = crudable
    initialize()
  }

  constructor(
    columnNames: Array<out ColumnInfo<Item, *>>,
    selectedColumn: Int,
    crudable: Crudable
  ) : super(columnNames, mutableListOf(), selectedColumn) {
    this.crudable = crudable
    initialize()
  }

  constructor(
    columnNames: Array<out ColumnInfo<Item, *>>,
    selectedColumn: Int,
    order: SortOrder,
    crudable: Crudable
  ) : super(columnNames, mutableListOf(), selectedColumn, order) {
    this.crudable = crudable
    initialize()
  }

  var replacingItems: List<Item>? = null

  private fun Item?.cloneAndWrap(): List<Item> {
    val clonedItem = this?.clone(clazz)
    return if (clonedItem != null) {
      listOf(clonedItem)
    } else listOf()
  }

  override fun removeRow(idx: Int) {
    replacingItems = this[idx].cloneAndWrap()
    super.removeRow(idx)
  }

  override operator fun set(row: Int, item: Item) {
    replacingItems = this[row].cloneAndWrap()
    super.set(row, item)
  }

  override fun setItems(items: MutableList<Item>) {
    replacingItems = getItems().cloneElements()
    super.setItems(items)
  }

  override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int, notifyListeners: Boolean) {
    if (notifyListeners) {
      replacingItems = this[rowIndex].cloneAndWrap()
    }
    super.setValueAt(aValue, rowIndex, columnIndex, notifyListeners)
  }

  private var needToUpdateCrudableOnSetItems = true

  fun reinitialize() {
    needToUpdateCrudableOnSetItems = false
    items = fetch(crudable)
    needToUpdateCrudableOnSetItems = true
  }

  private fun initialize() {
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
              && needToUpdateCrudableOnSetItems) {
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
          TableModelEvent.DELETE -> replacingItems?.forEach { deleting -> onDelete(crudable, deleting)}
        }
      }
    }

  }

  private fun List<Item>.cloneElements(): List<Item> {
    return mapNotNull { it?.clone(clazz) }
  }

  abstract fun fetch(crudable: Crudable): MutableList<Item>

  abstract fun onAdd(crudable: Crudable, value: Item): Boolean

  abstract fun onUpdate(crudable: Crudable, value: Item): Boolean

  abstract fun onDelete(crudable: Crudable, value: Item)

  abstract fun onApplyingMergedCollection(crudable: Crudable, merged: MergedCollections<Item>)

  abstract val clazz: Class<out Item>

}