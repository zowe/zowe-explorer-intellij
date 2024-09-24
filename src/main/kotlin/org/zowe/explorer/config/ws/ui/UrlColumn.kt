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

package org.zowe.explorer.config.ws.ui

import com.intellij.util.ui.ColumnInfo
import org.zowe.explorer.common.message
import org.zowe.explorer.common.ui.ErrorableTableCellRenderer
import org.zowe.explorer.config.ws.WorkingSetConfig
import javax.swing.table.TableCellRenderer

/**
 * Class which represents column of url in GUI
 */
class UrlColumn<WSConfig : WorkingSetConfig>(
  connectionColumnName: String,
  private val getUrl: (WSConfig) -> String?
) : ColumnInfo<WSConfig, String>(connectionColumnName) {

  /**
   * Returns value of url of particular config
   * @param item instance of working set config
   * @return url stored in instance of working set config
   */
  override fun valueOf(item: WSConfig): String {
    return getUrl(item) ?: message("configurable.ws.tables.ws.url.error.empty")
  }

  /**
   * Checks if cell can be edited
   * @param item instance of working set config
   * @return is cell can be edited
   */
  override fun isCellEditable(item: WSConfig): Boolean {
    return false
  }

  /**
   * Returns instance of renderer object
   * @param item instance of working set config
   * @return instance of table cell renderer
   */
  override fun getRenderer(item: WSConfig): TableCellRenderer {
    return ErrorableTableCellRenderer(
      errorMessage = message("configurable.ws.tables.ws.url.error.empty.tooltip")
    ) {
      getUrl(item) == null
    }
  }

}
