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

import eu.ibagroup.formainframe.common.ui.CrudableTableModel
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.MergedCollections
import eu.ibagroup.formainframe.utils.toMutableList

/**
 * Base implementation of table model to display connections (z/OSMF, CICS and etc.).
 * @param crudable crudable from which to get connections.
 * @param connectionUrlColumnName name of the column to display connection url.
 * @author Valiantsin Krus
 */
abstract class ConnectionsTableModelBase<ConnectionConfig : ConnectionConfigBase, ConnectionDialogState : ConnectionDialogStateBase<ConnectionConfig>>(
  crudable: Crudable,
  connectionUrlColumnName: String = "z/OSMF URL"
) : CrudableTableModel<ConnectionDialogState>(
  crudable,
  ConnectionNameColumn(),
  ConnectionUrlColumn(connectionUrlColumnName),
  ConnectionUsernameColumn(),
  ConnectionUssOwnerColumn()
) {
  abstract val connectionConfigClass: Class<out ConnectionConfig>

  abstract fun connectionToDialogState(connectionConfig: ConnectionConfig, crudable: Crudable): ConnectionDialogState

  /**
   * Get connections from the crudable object
   * @param crudable the crudable object to get connections from
   * @return connection configs list
   */
  override fun fetch(crudable: Crudable): MutableList<ConnectionDialogState> {
    return crudable.getAll(connectionConfigClass).map {
      connectionToDialogState(it, crudable)
    }.toMutableList()
  }

  /**
   * Update credentials and connections callback for "update" event
   * @param crudable the crudable object to update configurations in
   * @param value the connection dialog state value with the new credentials and connection config
   * @return true if all the values after the update are present
   */
  override fun onUpdate(crudable: Crudable, value: ConnectionDialogState): Boolean {
    return with(crudable) {
      listOf(
        update(value.credentials),
        update(value.connectionConfig)
      ).all { it?.isPresent ?: false }
    }
  }

  /**
   * Delete credentials and connections callback for "delete" event
   * @param crudable the crudable object to delete configurations in
   * @param value the connection dialog state value with the credentials and connection config to delete
   */
  override fun onDelete(crudable: Crudable, value: ConnectionDialogState) {
    with(crudable) {
      delete(value.credentials)
      delete(value.connectionConfig)
    }
  }

  /**
   * Add credentials and connections callback for "add" event
   * @param crudable the crudable object to add configurations in
   * @param value the connection dialog state value with the new credentials and connection config to add
   * @return true if all the values after the addition are present
   */
  override fun onAdd(crudable: Crudable, value: ConnectionDialogState): Boolean {
    return with(crudable) {
      listOf(
        add(value.credentials),
        add(value.connectionConfig)
      ).all { it?.isPresent ?: false }
    }
  }

  /**
   * Callback to process after applyMergedCollection is proceeded. Will continue applying merged collections to the crudable
   * @param crudable the crudable object to apply configurations in
   * @param merged the merged collections to apply
   */
  override fun onApplyingMergedCollection(crudable: Crudable, merged: MergedCollections<ConnectionDialogState>) {
    listOf(
      Pair(Credentials::class.java) { it: ConnectionDialogState -> it.credentials },
      Pair(Credentials::class.java) { it: ConnectionDialogState -> it.connectionConfig },
    ).forEach { pair ->
      crudable.applyMergedCollections(
        pair.first, MergedCollections(
          toAdd = merged.toAdd.map { pair.second(it) },
          toUpdate = merged.toUpdate.map { pair.second(it) },
          toDelete = merged.toDelete.map { pair.second(it) }
        )
      )
    }
  }
}