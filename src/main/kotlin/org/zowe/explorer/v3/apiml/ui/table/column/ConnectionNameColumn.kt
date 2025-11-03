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

class ConnectionNameColumn : ColumnInfo<ApiMlConnectionDialogState, String>(
  message("configurable.connection.table.name")
) {

  override fun valueOf(item: ApiMlConnectionDialogState): String {
    return item.name
  }

  override fun setValue(item: ApiMlConnectionDialogState, value: String) {
    item.name = value
  }

  override fun getTooltipText(): String {
    return message("configurable.connection.table.name.tooltip")
  }

}