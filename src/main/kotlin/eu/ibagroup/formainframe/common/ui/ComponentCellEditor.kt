package eu.ibagroup.formainframe.common.ui

import javax.swing.AbstractCellEditor
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.table.TableCellEditor

class ComponentCellEditor<Component : JComponent, Value>(
  componentBuilder: () -> Component,
  private val panelConfigurator: (Component, CellEditorParams) -> Unit,
  private val valueCollector: (Component) -> Value
) : AbstractCellEditor(), TableCellEditor {

  private val component = componentBuilder()

  override fun getCellEditorValue(): Value = valueCollector(component)

  override fun getTableCellEditorComponent(
    table: JTable?,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ): Component {
    return component.apply { panelConfigurator(this, CellEditorParams(table, value, isSelected, row, column)) }
  }
}

class ComponentCellEditorBuilder<Component : JComponent>(
  private val componentBuilder: () -> Component,
) {
  private var panelConfigurator: (Component, CellEditorParams) -> Unit = { _, _ -> }

  fun addConfigurator(panelConfigurator: (Component, CellEditorParams) -> Unit): ComponentCellEditorBuilder<Component> {
    this.panelConfigurator = panelConfigurator
    return this
  }

  fun <Value> addValueCollector(valueCollector: (Component) -> Value): ComponentCellEditor<Component, Value> {
    return ComponentCellEditor(componentBuilder, panelConfigurator, valueCollector)
  }
}