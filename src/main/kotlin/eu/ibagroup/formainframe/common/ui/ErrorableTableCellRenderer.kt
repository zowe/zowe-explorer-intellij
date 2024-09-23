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

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.JBColor
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.runIfTrue
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

/**
 * Error checking class for table cell renderer
 * @param errorMessage error message to send
 * @param renderer [DefaultTableCellRenderer] instance describes cell renderer
 * @param hasError boolean parameter for check error in cell
 */
class ErrorableTableCellRenderer(
  private val errorMessage: String,
  private val renderer: DefaultTableCellRenderer = DefaultTableCellRenderer(),
  changeForegroundOnError: Boolean = true,
  showErrorIcon: Boolean = false,
  private val hasError: () -> Boolean
) : TableCellRenderer by renderer {
  init {
    if (hasError()) {
      if (changeForegroundOnError) {
        renderer.foreground = JBColor.namedColor("Label.errorForeground", 0xff0000)
      }
      renderer.toolTipText = errorMessage
      if (showErrorIcon) {
        renderer.icon = AllIcons.General.ExclMark
      }
    } else {
      if (changeForegroundOnError) {
        renderer.foreground = JBColor.namedColor("Label.foreground", 0x000000, 0xffffff)
      }
      renderer.toolTipText = null
      if (showErrorIcon) {
        renderer.icon = null
      }
    }
  }

  /**
   * Get table cell renderer component and check if a field of component has error.
   * If so, it creates a validation error message associated with the component
   * @param table the table to assign validators on columns of it
   * @param row the row number to find the cell to assign the validator to
   * @param column the column number to find the cell to assign the validator to
   * @return the updated renderer component
   */
  override fun getTableCellRendererComponent(
    table: JTable?,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    return renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column).apply {
      table?.castOrNull<ValidatingTableView<FilesWorkingSetConfig>>()?.let { t ->
        t.listTableModel.validationInfos[row, column] = hasError().runIfTrue {
          ValidationInfo(errorMessage, this as JComponent)
        }
      }
    }
  }
}
