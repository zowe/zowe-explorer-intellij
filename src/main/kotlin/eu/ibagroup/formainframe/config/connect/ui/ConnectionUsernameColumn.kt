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

package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.util.ui.ColumnInfo

/**
 * Class which represents column of username in GUI
 */
class ConnectionUsernameColumn<ConnectionState : ConnectionDialogStateBase<*>>
  : ColumnInfo<ConnectionState, String>("Username") {

  /**
   * Returns name of particular user
   * @param item all info about particular connection to mainframe
   * @return name of particular user
   */
  override fun valueOf(item: ConnectionState): String {
    return item.username
  }

  /**
   * Sets username to particular user
   * @param item all info about particular connection to mainframe
   * @param value new name of user
   */
  override fun setValue(item: ConnectionState, value: String) {
    item.username = value
  }

}
