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
