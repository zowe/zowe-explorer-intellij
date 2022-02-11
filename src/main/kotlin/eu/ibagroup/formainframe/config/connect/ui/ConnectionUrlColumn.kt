/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.util.ui.ColumnInfo

@Suppress("DialogTitleCapitalization")
class ConnectionUrlColumn : ColumnInfo<ConnectionDialogState, String>("z/OSMF URL") {

  override fun valueOf(item: ConnectionDialogState): String {
    return item.connectionUrl
  }

  override fun setValue(item: ConnectionDialogState, value: String) {
    item.connectionUrl = value
  }

}
