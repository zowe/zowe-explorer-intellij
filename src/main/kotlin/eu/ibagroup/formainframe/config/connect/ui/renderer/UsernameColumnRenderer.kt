/*
 * Copyright (c) 2024 IBA Group.
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

package eu.ibagroup.formainframe.config.connect.ui.renderer

import com.intellij.icons.AllIcons
import com.intellij.util.ui.table.IconTableCellRenderer
import eu.ibagroup.formainframe.common.message
import java.awt.Component
import javax.swing.Icon
import javax.swing.JTable

const val USERNAME_NOT_PRESENT_WARNING_TOOLTIP_TEXT =
  "Username for the connection is not found. Try fixing it by editing the connection"
const val NO_CONNECTION_ERROR_TOOLTIP_TEXT =
  "Connection is not found for the element"

/**
 * Custom renderer class for the username column in the connections table view. Provides functionality to render
 * a warning icon with a warning text about the username column
 */
class UsernameColumnRenderer : IconTableCellRenderer<String>() {

  /**
   * Function returns a warning icon if the username is empty or null otherwise
   * @param value the username cell value to check
   * @param table the table to render the icon in
   * @param row the row to render the icon in
   * @return the icon or null, depending on the check result
   */
  override fun getIcon(value: String, table: JTable, row: Int): Icon? {
    return when {
      value.isEmpty() -> {
        AllIcons.General.Warning
      }

      value == message("configurable.ws.tables.ws.username.error.empty") -> {
        AllIcons.General.Error
      }

      else -> {
        null
      }
    }
  }

  /**
   * Function returns a component which renders the username cell
   * @param table the table to render the cell in
   * @param value the username cell value
   * @param selected a parameter to check whether the cell is selected
   * @param focus a parameter to check whether the cell is focused
   * @param row the row to render the cell in
   * @param column the column to render the cell in
   * @return the actual component with a custom renderer
   */
  override fun getTableCellRendererComponent(
    table: JTable?,
    value: Any?,
    selected: Boolean,
    focus: Boolean,
    row: Int,
    column: Int
  ): Component {
    super.getTableCellRendererComponent(table, value, selected, focus, row, column)
    toolTipText = when (icon) {
      AllIcons.General.Warning -> {
        USERNAME_NOT_PRESENT_WARNING_TOOLTIP_TEXT
      }

      AllIcons.General.Error -> {
        NO_CONNECTION_ERROR_TOOLTIP_TEXT
      }

      else -> {
        message("configurable.ws.tables.ws.username.tooltip")
      }
    }
    return this
  }

}
