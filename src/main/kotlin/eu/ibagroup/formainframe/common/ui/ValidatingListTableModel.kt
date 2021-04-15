package eu.ibagroup.formainframe.common.ui

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import eu.ibagroup.formainframe.utils.SparseMatrix
import javax.swing.SortOrder

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

  fun getValidationInfoByColumnName(row: Int, columnName: String): ValidationInfo? {
    val index = this.columnInfos.indexOfFirst { it.name == columnName }
    return if (index >= 0) {
      validationInfos[row, index]
    } else {
      null
    }
  }

  operator fun get(row: Int): Item {
    return this.getItem(row)
  }

  open operator fun set(row: Int, item: Item) {
    (0 until columnCount).forEach { columnIndex ->
      val info = columnInfos[columnIndex]
      setValueAt(info.valueOf(item), row, columnIndex, false)
    }
    fireTableRowsUpdated(row, row)
  }

//  override fun fireTableChanged(e: TableModelEvent?) {
//    listenerList.listenerList
//      .filterIsInstance<TableModelListener>()
//      .forEach { it.tableChanged(e) }
//  }

}