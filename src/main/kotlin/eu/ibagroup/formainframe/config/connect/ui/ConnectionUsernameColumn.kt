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

// TODO: doc
class ConnectionUsernameColumn : ColumnInfo<ConnectionDialogState, String>("Username") {

  override fun valueOf(item: ConnectionDialogState): String {
    return item.username
  }

  override fun setValue(item: ConnectionDialogState, value: String) {
    item.username = value
  }

}