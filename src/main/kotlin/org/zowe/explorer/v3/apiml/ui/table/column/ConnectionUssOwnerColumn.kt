/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Dzianis Lisiankou
 */

package org.zowe.explorer.v3.apiml.ui.table.column

import com.intellij.util.ui.ColumnInfo
import org.zowe.explorer.common.message
import org.zowe.explorer.v3.apiml.ui.ApiMlConnectionDialogState
import javax.swing.table.TableCellRenderer

class ConnectionUssOwnerColumn : ColumnInfo<ApiMlConnectionDialogState, String>(
  "Uss Owner"
) {

  override fun valueOf(item: ApiMlConnectionDialogState): String {
    return item.ussOwner
  }

  override fun setValue(item: ApiMlConnectionDialogState, value: String) {
    item.ussOwner = value
  }

  override fun getRenderer(item: ApiMlConnectionDialogState): TableCellRenderer? {
    return super.getRenderer(item)
  }

  override fun getTooltipText(): String {
    return message("configurable.connection.table.owner.tooltip")
  }

}