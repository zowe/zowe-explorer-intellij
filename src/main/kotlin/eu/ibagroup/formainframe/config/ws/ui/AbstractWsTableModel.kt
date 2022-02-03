package eu.ibagroup.formainframe.config.ws.ui

import eu.ibagroup.formainframe.common.ui.CrudableTableModel
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.*
import eu.ibagroup.formainframe.utils.toMutableList

abstract class AbstractWsTableModel<WSConfig : WorkingSetConfig>(
  crudable: Crudable
) : CrudableTableModel<WSConfig>(crudable) {

  init {
    columnInfos = arrayOf(
      WSNameColumn { this.items },
      WSConnectionNameColumn<WSConfig>(crudable),
      WSUsernameColumn { crudable.getByUniqueKey<Credentials>(it.connectionConfigUuid)?.username },
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
