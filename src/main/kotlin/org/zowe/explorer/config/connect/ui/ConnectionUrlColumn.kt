/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.connect.ui

import com.intellij.util.ui.ColumnInfo

/**
 * Class represents a connection url column in connection table model.
 * It extends ColumnInfo abstract class and overloads getter and setter methods as values for this column.
 */
@Suppress("DialogTitleCapitalization")
class ConnectionUrlColumn : ColumnInfo<ConnectionDialogState, String>("z/OSMF URL") {

  /**
   * Overloaded getter method of ColumnInfo abstract class.
   */
  override fun valueOf(item: ConnectionDialogState): String {
    return item.connectionUrl
  }

  /**
   * Overloaded setter method of ColumnInfo abstract class.
   */
  override fun setValue(item: ConnectionDialogState, value: String) {
    item.connectionUrl = value
  }

}
