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
 * Class represents a connection name column in connection table model.
 * It extends ColumnInfo abstract class and overloads getter and setter methods as values for this column
 */
class ConnectionNameColumn<ConnectionState : ConnectionDialogStateBase<*>>
  : ColumnInfo<ConnectionState, String>("Name") {

  /**
   * overloaded getter method of ColumnInfo abstract class
   */
  override fun valueOf(item: ConnectionState): String {
    return item.connectionName
  }

  /**
   * overloaded setter method of ColumnInfo abstract class
   */
  override fun setValue(item: ConnectionState, value: String) {
    item.connectionName = value
  }

}
