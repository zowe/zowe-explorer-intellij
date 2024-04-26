/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.connect.ui.zosmf

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.ui.ConnectionsTableModelBase
import eu.ibagroup.formainframe.utils.crudable.Crudable

/** Table model for z/OSMF connections. Provides operations on connection entries of the table */
class ConnectionsTableModel(
  crudable: Crudable
) : ConnectionsTableModelBase<ConnectionConfig, ConnectionDialogState>(crudable) {

  override val clazz = ConnectionDialogState::class.java
  override val connectionConfigClass = ConnectionConfig::class.java

  override fun connectionToDialogState(connectionConfig: ConnectionConfig, crudable: Crudable): ConnectionDialogState {
    return connectionConfig.toDialogState(crudable)
  }

  /**
   * Set the item for the row
   * @param row the row number to update
   * @param item the item to set to the row
   */
  override fun set(row: Int, item: ConnectionDialogState) {
    get(row).isAllowSsl = item.isAllowSsl
    get(row).password = item.password
    get(row).zVersion = item.zVersion
    get(row).owner = item.owner
    super.set(row, item)
  }


  init {
    initialize()
  }
}
