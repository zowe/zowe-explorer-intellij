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
import org.zowe.explorer.v3.apiml.ui.ApiMlConnectionDialogState
import org.zowe.explorer.v3.state.config.getUrl

class ConnectionUrlColumn : ColumnInfo<ApiMlConnectionDialogState, String>(
  "API ML URL"
) {

  override fun valueOf(item: ApiMlConnectionDialogState): String {
    return getUrl(item.apiMlConnectionConfig)
  }

}