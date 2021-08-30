package eu.ibagroup.formainframe.config.connect.ui

import eu.ibagroup.formainframe.common.ui.CrudableTableModel
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.utils.crudable.*
import eu.ibagroup.formainframe.utils.toMutableList

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
}