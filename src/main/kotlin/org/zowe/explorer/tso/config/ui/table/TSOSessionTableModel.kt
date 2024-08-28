/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.tso.config.ui.table

import org.zowe.explorer.common.ui.CrudableTableModel
import org.zowe.explorer.tso.config.TSOSessionConfig
import org.zowe.explorer.tso.config.toDialogState
import org.zowe.explorer.tso.config.ui.TSOSessionDialogState
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.MergedCollections
import org.zowe.explorer.utils.toMutableList

/**
 * Table model for TSO sessions. Provides operations on session entries of the table
 */
class TSOSessionTableModel(
  crudable: Crudable
): CrudableTableModel<TSOSessionDialogState>(crudable) {

  override val clazz = TSOSessionDialogState::class.java

  private val configClazz = TSOSessionConfig::class.java

  init {
    columnInfos = arrayOf(
      SessionNameColumn(),
      ConnectionNameColumn(crudable),
      LogonProcColumn(),
      AccountNumberColumn()
    )
    initialize()
  }

  /**
   * Get TSO sessions from the crudable object
   */
  override fun fetch(crudable: Crudable): MutableList<TSOSessionDialogState> {
    return crudable.getAll(configClazz).map { it.toDialogState() }.toMutableList()
  }

  /**
   * Callback to process after applyMergedCollection is proceeded. Will continue applying merged collections to the crudable
   */
  override fun onApplyingMergedCollection(crudable: Crudable, merged: MergedCollections<TSOSessionDialogState>) {
    crudable.applyMergedCollections(
      configClazz,
      MergedCollections(
        toAdd = merged.toAdd.map { it.tsoSessionConfig },
        toUpdate = merged.toUpdate.map { it.tsoSessionConfig },
        toDelete = merged.toDelete.map { it.tsoSessionConfig }
      )
    )
  }

  /**
   * Delete TSO sessions callback for "delete" event
   */
  override fun onDelete(crudable: Crudable, value: TSOSessionDialogState) {
    crudable.delete(value.tsoSessionConfig)
  }

  /**
   * Update TSO sessions callback for "update" event
   */
  override fun onUpdate(crudable: Crudable, value: TSOSessionDialogState): Boolean {
    return crudable.update(value.tsoSessionConfig)?.isPresent ?: false
  }

  /**
   * Add TSO sessions callback for "add" event
   */
  override fun onAdd(crudable: Crudable, value: TSOSessionDialogState): Boolean {
    return crudable.add(value.tsoSessionConfig)?.isPresent ?: false
  }

  /**
   * Set the item for the row
   */
  override fun set(row: Int, item: TSOSessionDialogState) {
    get(row).charset = item.charset
    get(row).codepage = item.codepage
    get(row).rows = item.rows
    get(row).columns = item.columns
    get(row).userGroup = item.userGroup
    get(row).regionSize = item.regionSize
    get(row).timeout = item.timeout
    get(row).maxAttempts = item.maxAttempts
    super.set(row, item)
  }

}
