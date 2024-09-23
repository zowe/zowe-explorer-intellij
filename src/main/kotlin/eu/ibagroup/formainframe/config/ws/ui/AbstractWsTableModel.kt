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

package eu.ibagroup.formainframe.config.ws.ui

import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.common.ui.CrudableTableModel
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.MergedCollections
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.nullable
import eu.ibagroup.formainframe.utils.toMutableList

/**
 * Abstract table model for table in configurations
 * for Working Sets (e.g. JES Working Set, Files Working Set).
 * @param Connection The system (such as zosmf, cics etc.) connection class to work with (see [ConnectionConfigBase]).
 * @param WSConfig Implementation class of [WorkingSetConfig].
 * @param crudable Crudable instance to change data.
 * @author Valiantsin Krus
 */
abstract class AbstractWsTableModel<Connection : ConnectionConfigBase, WSConfig : WorkingSetConfig>(
  crudable: Crudable,
  connectionClass: Class<out Connection>,
  connectionColumnName: String = message("configurable.ws.tables.ws.url.name")
) : CrudableTableModel<WSConfig>(crudable) {

  init {
    columnInfos = arrayOf(
      WSNameColumn { this.items },
      WSConnectionNameColumn<Connection, WSConfig>(crudable, connectionClass),
      WSUsernameColumn { crudable.getByUniqueKey<Credentials>(it.connectionConfigUuid)?.username },
      UrlColumn(connectionColumnName) {
        crudable.getByUniqueKey(
          connectionClass,
          it.connectionConfigUuid
        ).nullable?.url
      }
    )
  }

  override fun fetch(crudable: Crudable): MutableList<WSConfig> {
    return crudable.getAll(clazz).toMutableList().sortedBy { it.name }.toMutableList()
  }

  override fun onUpdate(crudable: Crudable, value: WSConfig): Boolean {
    return crudable.update(value)?.isPresent ?: false
  }

  override fun onDelete(crudable: Crudable, value: WSConfig) {
    crudable.delete(value)
  }

  override fun onAdd(crudable: Crudable, value: WSConfig): Boolean {
    return crudable.add(value)?.isPresent ?: false
  }

  override fun onApplyingMergedCollection(crudable: Crudable, merged: MergedCollections<WSConfig>) {
    crudable.applyMergedCollections(clazz, merged)
  }

}
