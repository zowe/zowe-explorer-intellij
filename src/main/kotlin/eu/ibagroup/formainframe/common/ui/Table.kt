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

import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.AnActionButton
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.table.TableView
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.event.TableModelEvent

/**
 * An "Add" action callback builder.
 * The callback creates a new empty row in the table and immediately focuses on it
 * @param table the table to add a new row to
 * @param createEmptyItem the function to create an empty row in the table
 * @param editingColumnIndex the current editing column index to focus on the appropriate cell when the row is created
 * @return the callback to call, that accepts an action button to be clicked to run the callback
 */
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

/**
 * A "Remove" action callback builder.
 * The callback removes the focused row from the table
 * @param table the table to remove the row from
 * @param isDeletionNeeded the function to check whether the row can be deleted
 * @return the callback to call, that accepts an action button to be clicked to run the callback
 */
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

/**
 * Toolbar builder for the table view
 * @param table the table to create a toolbar for
 * @param editingColumnIndex the column index being edited, for "Add" operation
 * @param addDefaultActions the variable to check if it is needed to initialize the toolbar with the default actions
 */
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

  /**
   * Configure the toolbar decorator for the toolbar table builder. Sets up the default actions for the toolbar if it is needed.
   * The toolbar position is bottom by the default
   * @param init the toolbar actions initializer
   */
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

/**
 * Create a table with a toolbar inside the row
 * @param table the table view to put in the view
 * @param editingColumnIndex the column index being edited, for "Add" operation
 * @param addDefaultActions the variable to check if it is needed to initialize the toolbar with the default actions
 * @param toolbarTableBuilder the toolbar table builder instance to initialize the new one with its parameters
 * @return DSL cell component builder
 */
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
    .align(AlignX.FILL.plus(AlignY.FILL))
}

val TableModelEvent.rows: IntRange
  get() = (this.firstRow..this.lastRow)

const val DEFAULT_ROW_HEIGHT = 28
