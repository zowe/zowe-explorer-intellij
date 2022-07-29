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

import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.AnActionButton
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.table.TableView
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.event.TableModelEvent

// TODO: doc
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

  fun configureDecorator(): ToolbarTableBuilder<Item> {
    return configureDecorator { }
  }

  fun configureDecorator(init: ToolbarDecorator.() -> Unit): ToolbarTableBuilder<Item> {
    toolbarDecorator = ToolbarDecorator
      .createDecorator(table)
      .apply(init)
      .apply decorator@{
        if (addDefaultActions && this@ToolbarTableBuilder::itemProducer.isInitialized) {
          this.setAddAction(addAction(table, itemProducer, editingColumnIndex))
            .setRemoveAction(removeAction(table, deletionPredicate))
            .setRemoveActionUpdater(removeActionUpdater(table))
        }
      }
      .setToolbarPosition(ActionToolbarPosition.BOTTOM)
    return this
  }

  internal fun createPanel(): JPanel {
    if (toolbarDecorator == null) {
      configureDecorator()
    }
    return toolbarDecorator!!.createPanel()
  }

}

fun <Item> Row.tableWithToolbar(
  table: TableView<Item>,
  editingColumnIndex: Int = 0,
  addDefaultActions: Boolean = false,
  toolbarTableBuilder: ToolbarTableBuilder<Item>.() -> Unit
): com.intellij.ui.dsl.builder.Cell<JPanel> {
  val tableComponent = ToolbarTableBuilder(table, editingColumnIndex, addDefaultActions)
    .apply { toolbarTableBuilder() }
    .createPanel()
  return cell(tableComponent)
    .horizontalAlign(HorizontalAlign.FILL)
    .verticalAlign(VerticalAlign.FILL)
}

val TableModelEvent.rows: IntRange
  get() = (this.firstRow..this.lastRow)

const val DEFAULT_ROW_HEIGHT = 28
