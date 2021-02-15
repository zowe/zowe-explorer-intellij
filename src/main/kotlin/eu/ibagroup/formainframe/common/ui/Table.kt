package eu.ibagroup.formainframe.common.ui

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.AnActionButton
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ListTableModel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.event.TableModelEvent

inline fun <Item> addAction(
  table: TableView<Item>,
  crossinline createEmptyItem: (TableView<Item>) -> Item,
  editingColumnIndex: Int = 0
): (AnActionButton) -> Unit {
  return {
    table.cellEditor?.stopCellEditing()
    table.listTableModel.addRow(createEmptyItem(table))
    with(table.listTableModel.rowCount - 1) {
      if (this >= 0) {
        table.editCellAt(this, editingColumnIndex)
      }
    }
    table.selectionModel.clearSelection()
  }
}

inline fun <Item> removeAction(
  table: TableView<Item>,
  crossinline isDeletionNeeded: (List<Item>) -> Boolean
): (AnActionButton) -> Unit {
  return {
    table.cellEditor?.stopCellEditing()
    table.selectedRows?.let { indices ->
      indices.map { table.listTableModel.getItem(it) }.toList().apply {
        if (isDeletionNeeded(this)) {
          indices.reversed().forEach { table.listTableModel.removeRow(it) }
        }
      }
    }
    table.clearSelection()
    table.updateUI()
  }
}

fun removeActionUpdater(
  table: JTable
): (AnActionEvent) -> Boolean {
  return { table.selectedRowCount > 0 }
}

fun <Item> Cell.toolbarTable(
  title: String,
  table: TableView<Item>,
  hasSeparator: Boolean = true,
  editingColumnIndex: Int = 0,
  addDefaultActions: Boolean = false,
  toolbarTableBuilder: ToolbarTableBuilder<Item>.() -> Unit
): CellBuilder<JPanel> {
  return panel(
    title,
    ToolbarTableBuilder(table, editingColumnIndex, addDefaultActions)
      .apply {
        toolbarTableBuilder()
      }.createPanel(),
    hasSeparator
  )
}

class ToolbarTableBuilder<Item> @PublishedApi internal constructor(
  private val table: TableView<Item>,
  private val editingColumnIndex: Int = 0,
  private val addDefaultActions: Boolean = true
) {

  private lateinit var itemProducer: (TableView<Item>) -> Item

  private var toolbarDecorator: ToolbarDecorator? = null

  private var deletionPredicate: (List<Item>) -> Boolean = { true }

  fun addNewItemProducer(producer: (TableView<Item>) -> Item): ToolbarTableBuilder<Item> {
    itemProducer = producer
    return this
  }

  fun addDeletionPredicate(predicate: (List<Item>) -> Boolean): ToolbarTableBuilder<Item> {
    deletionPredicate = predicate
    return this
  }

  fun configureDecorator(): ToolbarTableBuilder<Item> {
    return configureDecorator { }
  }

  fun configureDecorator(init: ToolbarDecorator.() -> Unit): ToolbarTableBuilder<Item> {
    toolbarDecorator = ToolbarDecorator.createDecorator(table).apply(init).apply decorator@{
      if (addDefaultActions
        && this@ToolbarTableBuilder::itemProducer.isInitialized
      ) {
        this.setAddAction(addAction(table, itemProducer, editingColumnIndex))
          .setRemoveAction(removeAction(table, deletionPredicate))
          .setRemoveActionUpdater(removeActionUpdater(table))
      }
    }
    return this
  }

  internal fun createPanel(): JPanel {
    if (toolbarDecorator == null) {
      configureDecorator()
    }
    return toolbarDecorator!!.createPanel()
  }

}

fun <Item> ListTableModel<Item>.getColumnIndexByName(name: String): Int {
  return this.columnInfos.indexOfFirst { it.name == name }
}

val TableModelEvent.rows: IntRange
  get() = (this.firstRow..this.lastRow)

const val DEFAULT_ROW_HEIGHT = 28