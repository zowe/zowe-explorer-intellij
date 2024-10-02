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

package eu.ibagroup.formainframe.config.ws.ui

import com.intellij.util.ui.ColumnInfo
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.config.connect.ui.renderer.UsernameColumnRenderer
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import javax.swing.table.TableCellRenderer


/**
 * Class which represents working set username column in working set table model
 */
class WSUsernameColumn<WSConfig : WorkingSetConfig>(
  private val getUsername: (WSConfig) -> String
) : ColumnInfo<WSConfig, String>(message("configurable.ws.tables.ws.username.name")) {

  /**
   * Overloaded getter method. Gets the username from crudable by connection config uuid
   */
  override fun valueOf(item: WSConfig): String {
    return getUsername(item)
  }

  /**
   * Overloaded getter method. Gets the renderer
   */
  override fun getRenderer(item: WSConfig): TableCellRenderer {
    return UsernameColumnRenderer()
  }

  /**
   * Determines if table cell is editable
   */
  override fun isCellEditable(item: WSConfig?): Boolean {
    return false
  }

}
