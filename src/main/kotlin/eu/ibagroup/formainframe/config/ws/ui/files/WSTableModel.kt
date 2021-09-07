package eu.ibagroup.formainframe.config.ws.ui.files

import eu.ibagroup.formainframe.common.ui.CrudableTableModel
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.*
import eu.ibagroup.formainframe.utils.crudable.*
import eu.ibagroup.formainframe.utils.toMutableList

class WSTableModel(
  crudable: Crudable,
) : CrudableTableModel<FilesWorkingSetConfig>(crudable) {

  init {
    columnInfos = arrayOf(
      WSNameColumn { this.items },
      WSConnectionNameColumn<FilesWorkingSetConfig>(crudable),
      WSUsernameColumn { crudable.getByUniqueKey<Credentials>(it.connectionConfigUuid)?.username },
      UrlColumn { crudable.getByUniqueKey<ConnectionConfig>(it.connectionConfigUuid)?.url}
    )
  }

  override fun fetch(crudable: Crudable): MutableList<FilesWorkingSetConfig> {
    return crudable.getAll<FilesWorkingSetConfig>().toMutableList().sortedBy { it.name }.toMutableList()
  }

  override fun onUpdate(crudable: Crudable, value: FilesWorkingSetConfig): Boolean {
    return crudable.update(value).isPresent
  }

  override fun onDelete(crudable: Crudable, value: FilesWorkingSetConfig) {
    crudable.delete(value)
  }

  override fun onAdd(crudable: Crudable, value: FilesWorkingSetConfig): Boolean {
    return crudable.add(value).isPresent
  }

  override fun onApplyingMergedCollection(crudable: Crudable, merged: MergedCollections<FilesWorkingSetConfig>) {
    crudable.applyMergedCollections(merged)
  }

  override val clazz = FilesWorkingSetConfig::class.java

  override operator fun set(row: Int, item: FilesWorkingSetConfig) {
    get(row).dsMasks = item.dsMasks
    get(row).ussPaths = item.ussPaths
    super.set(row, item)
  }

}
