/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.ws.ui

import eu.ibagroup.formainframe.common.ui.CrudableTableModel
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.*
import eu.ibagroup.formainframe.utils.toMutableList

class WSTableModel(
  crudable: Crudable,
) : CrudableTableModel<WorkingSetConfig>(crudable) {

  init {
    columnInfos = arrayOf(
      WSNameColumn { this.items },
      WSConnectionNameColumn(crudable),
      WSUsernameColumn { crudable.getByUniqueKey<Credentials>(it.connectionConfigUuid)?.username },
      UrlColumn { crudable.getByForeignKeyDeeply<WorkingSetConfig, UrlConnection>(it)?.url }
    )
  }

  override fun fetch(crudable: Crudable): MutableList<WorkingSetConfig> {
    return crudable.getAll<WorkingSetConfig>().toMutableList().sortedBy { it.name }.toMutableList()
  }

  override fun onUpdate(crudable: Crudable, value: WorkingSetConfig): Boolean {
    return crudable.update(value).isPresent
  }

  override fun onDelete(crudable: Crudable, value: WorkingSetConfig) {
    crudable.delete(value)
  }

  override fun onAdd(crudable: Crudable, value: WorkingSetConfig): Boolean {
    return crudable.add(value).isPresent
  }

  override fun onApplyingMergedCollection(crudable: Crudable, merged: MergedCollections<WorkingSetConfig>) {
    crudable.applyMergedCollections(merged)
  }

  override val clazz = WorkingSetConfig::class.java

  override operator fun set(row: Int, item: WorkingSetConfig) {
    get(row).dsMasks = item.dsMasks
    get(row).ussPaths = item.ussPaths
    super.set(row, item)
  }

}