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

import org.zowe.explorer.common.ui.CrudableTableModel
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.Credentials
import org.zowe.explorer.utils.crudable.*
import org.zowe.explorer.utils.toMutableList

class ConnectionsTableModel(
  crudable: Crudable
) : CrudableTableModel<ConnectionDialogState>(
  crudable,
  ConnectionNameColumn(),
  ConnectionUrlColumn(),
  ConnectionUsernameColumn()
) {

  override fun fetch(crudable: Crudable): MutableList<ConnectionDialogState> {
    return crudable.getAll<ConnectionConfig>().map {
      it.toDialogState(crudable)
    }.toMutableList()
  }

  override fun onUpdate(crudable: Crudable, value: ConnectionDialogState): Boolean {
    return with(crudable) {
        listOf(
          update(value.credentials),
          update(value.connectionConfig)
        ).all { it.isPresent }
    }
  }

  override fun onDelete(crudable: Crudable, value: ConnectionDialogState) {
    with(crudable) {
      delete(value.credentials)
      delete(value.connectionConfig)
    }
  }

  override fun onAdd(crudable: Crudable, value: ConnectionDialogState): Boolean {
    return with(crudable) {
      value.connectionUuid = crudable.nextUniqueValue<ConnectionConfig, String>()
      listOf(
        add(value.credentials),
        add(value.connectionConfig)
      ).all { it.isPresent }
    }
  }

  override fun onApplyingMergedCollection(crudable: Crudable, merged: MergedCollections<ConnectionDialogState>) {
    listOf(
      Pair(Credentials::class.java, ConnectionDialogState::credentials),
      Pair(ConnectionConfig::class.java, ConnectionDialogState::connectionConfig)
    ).forEach { pair ->
      crudable.applyMergedCollections(
        pair.first, MergedCollections(
          toAdd = merged.toAdd.map { pair.second.get(it) },
          toUpdate = merged.toUpdate.map { pair.second.get(it) },
          toDelete = merged.toDelete.map { pair.second.get(it) }
        )
      )
    }
  }

  override fun set(row: Int, item: ConnectionDialogState) {
    get(row).isAllowSsl = item.isAllowSsl
    get(row).password = item.password
    get(row).zVersion = item.zVersion
    get(row).codePage = item.codePage
    super.set(row, item)
  }

  override val clazz = ConnectionDialogState::class.java
  init {
    initialize()
  }
}
