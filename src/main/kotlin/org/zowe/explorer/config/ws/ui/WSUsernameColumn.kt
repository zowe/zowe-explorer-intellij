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
import javax.swing.table.TableCellRenderer

private val NO_USERNAME_MESSAGE = message("configurable.ws.tables.ws.username.error.empty")

class WSUsernameColumn<WSConfig : WorkingSetConfig>(
  private val getUsername: (WSConfig) -> String?
) : ColumnInfo<WSConfig, String>(message("configurable.ws.tables.ws.username.name")) {

  override fun valueOf(item: WSConfig): String {
    return getUsername(item) ?: NO_USERNAME_MESSAGE
  }

  override fun getRenderer(item: WSConfig): TableCellRenderer {
    return ErrorableTableCellRenderer(
      errorMessage = message("configurable.ws.tables.ws.url.error.empty.tooltip")
    ) {
      getUsername(item) == null
    }
  }

  override fun isCellEditable(item: WSConfig?): Boolean {
    return false
  }

  override fun getTooltipText(): String {
    return message("configurable.ws.tables.ws.username.tooltip")
  }

}
