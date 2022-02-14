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
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

@Suppress("DialogTitleCapitalization")
class UrlColumn<WSConfig : WorkingSetConfig>(
  private val getUrl: (WSConfig) -> String?
) : ColumnInfo<WSConfig, String>(message("configurable.ws.tables.ws.url.name")) {

  override fun valueOf(item: WSConfig): String {
    return getUrl(item) ?: message("configurable.ws.tables.ws.url.error.empty")
  }

  override fun isCellEditable(item: WSConfig): Boolean {
    return false
  }

  override fun getWidth(table: JTable?): Int {
    return 270
  }

  override fun getRenderer(item: WSConfig): TableCellRenderer {
    return ErrorableTableCellRenderer(
      errorMessage = message("configurable.ws.tables.ws.url.error.empty.tooltip")
    ) {
      getUrl(item) == null
    }
  }

}
