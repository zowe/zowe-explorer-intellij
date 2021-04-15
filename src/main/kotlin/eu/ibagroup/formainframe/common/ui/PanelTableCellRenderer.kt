package eu.ibagroup.formainframe.common.ui

import java.awt.Component
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

class PanelTableCellRenderer<Panel : JPanel>(
  private val componentRenderer: (params: CellRendererParams, label: JLabel) -> Panel
) : JPanel(), TableCellRenderer {

  private val delegate = DefaultTableCellRenderer()

  override fun getTableCellRendererComponent(
    table: JTable?,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val delegatedComponent = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
    return componentRenderer(CellRendererParams(table, value, isSelected, hasFocus, row, column), delegatedComponent)
  }



}