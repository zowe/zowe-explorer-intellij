/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.ws.ui

import org.zowe.explorer.common.ui.CrudableTableModel
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.Credentials
import org.zowe.explorer.config.ws.WorkingSetConfig
import org.zowe.explorer.utils.crudable.*
import org.zowe.explorer.utils.toMutableList

abstract class AbstractWsTableModel<WSConfig : WorkingSetConfig>(
  crudable: Crudable
) : CrudableTableModel<WSConfig>(crudable) {

  init {
    columnInfos = arrayOf(
      WSNameColumn { this.items },
      WSConnectionNameColumn<WSConfig>(crudable),
      WSUsernameColumn(
        { crudable.getByUniqueKey<Credentials>(it.connectionConfigUuid)?.username },
        {
          val username = crudable.getByUniqueKey<Credentials>(it.connectionConfigUuid)?.username
          val connectionConfig = crudable.getByUniqueKey<ConnectionConfig>(it.connectionConfigUuid)
          if (connectionConfig?.zoweConfigPath == null) username else "*".repeat(username?.length ?: 0)
        }
      ),
      UrlColumn { crudable.getByUniqueKey<ConnectionConfig>(it.connectionConfigUuid)?.url }
    )
  }

  override fun fetch(crudable: Crudable): MutableList<WSConfig> {
    return crudable.getAll(clazz).toMutableList().sortedBy { it.name }.toMutableList()
  }

  override fun onUpdate(crudable: Crudable, value: WSConfig): Boolean {
    return crudable.update(value).isPresent
  }

  override fun onDelete(crudable: Crudable, value: WSConfig) {
    crudable.delete(value)
  }

  override fun onAdd(crudable: Crudable, value: WSConfig): Boolean {
    return crudable.add(value).isPresent
  }

  override fun onApplyingMergedCollection(crudable: Crudable, merged: MergedCollections<WSConfig>) {
    crudable.applyMergedCollections(clazz, merged)
  }

}
