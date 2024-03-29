/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.ws.ui

import com.intellij.util.ui.ColumnInfo
import org.zowe.explorer.common.message
import org.zowe.explorer.common.ui.ErrorableTableCellRenderer
import org.zowe.explorer.config.ws.WorkingSetConfig
import java.awt.Component
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

private val NO_USERNAME_MESSAGE = message("configurable.ws.tables.ws.username.error.empty")

/**
 * Class which represents working set username column in working set table model
 */
class WSUsernameColumn<WSConfig : WorkingSetConfig>(
  private val getUsername: (WSConfig) -> String?,
  private val renderUsername: (WSConfig) -> String? = { getUsername(it) }
) : ColumnInfo<WSConfig, String>(message("configurable.ws.tables.ws.username.name")) {

  /**
   * Overloaded getter method. Gets the username from crudable by connection config uuid
   */
  override fun valueOf(item: WSConfig): String {
    return getUsername(item) ?: NO_USERNAME_MESSAGE
  }

  /**
   * Overloaded getter method. Gets the renderer
   */
  override fun getRenderer(item: WSConfig): TableCellRenderer {
    return ErrorableTableCellRenderer(
      errorMessage = message("configurable.ws.tables.ws.url.error.empty.tooltip"),
      renderer = object: DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
          table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
        ): Component {
          val formedUsername = renderUsername(item)
          return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column).apply {
            text = formedUsername
          }
        }
      }
    ) {
      getUsername(item) == null
    }
  }

  /**
   * Determines if table cell is editable
   */
  override fun isCellEditable(item: WSConfig?): Boolean {
    return false
  }

  /**
   * Gets the UI tooltip of the username column when mouse is hovered
   */
  override fun getTooltipText(): String {
    return message("configurable.ws.tables.ws.username.tooltip")
  }

}
