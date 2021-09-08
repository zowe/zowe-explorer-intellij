package eu.ibagroup.formainframe.config.ws.ui.jobs

import com.intellij.util.ui.ColumnInfo
import eu.ibagroup.formainframe.common.ui.CrudableTableModel
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.JobsWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.UrlColumn
import eu.ibagroup.formainframe.config.ws.ui.WSConnectionNameColumn
import eu.ibagroup.formainframe.config.ws.ui.WSNameColumn
import eu.ibagroup.formainframe.config.ws.ui.WSUsernameColumn
import eu.ibagroup.formainframe.utils.crudable.*
import eu.ibagroup.formainframe.utils.findAnyNullable
import eu.ibagroup.formainframe.utils.toMutableList

class JobsWsTableModel(crudable: Crudable): CrudableTableModel<JobsWorkingSetConfig>(crudable) {

  init {
    columnInfos = arrayOf(
      WSNameColumn { this.items },
      WSConnectionNameColumn<JobsWorkingSetConfig>(crudable),
      WSUsernameColumn { crudable.getByUniqueKey<Credentials>(it.connectionConfigUuid)?.username },
      UrlColumn { crudable.getByUniqueKey<ConnectionConfig>(it.connectionConfigUuid)?.url}
    )
  }

  override fun fetch(crudable: Crudable): MutableList<JobsWorkingSetConfig> {
    return crudable.getAll<JobsWorkingSetConfig>().toMutableList().sortedBy { it.name }.toMutableList()
  }

  override fun onUpdate(crudable: Crudable, value: JobsWorkingSetConfig): Boolean {
    return crudable.update(value).isPresent
  }

  override fun onDelete(crudable: Crudable, value: JobsWorkingSetConfig) {
    crudable.delete(value)
  }

  override fun onAdd(crudable: Crudable, value: JobsWorkingSetConfig): Boolean {
    return crudable.add(value).isPresent
  }

  override fun onApplyingMergedCollection(crudable: Crudable, merged: MergedCollections<JobsWorkingSetConfig>) {
    crudable.applyMergedCollections(merged)
  }

  override val clazz = JobsWorkingSetConfig::class.java

  override fun set(row: Int, item: JobsWorkingSetConfig) {
    get(row).jobsFilters = item.jobsFilters
    super.set(row, item)
  }

  object JesWsNameColumn: ColumnInfo<JobsWorkingSetConfig, String>("Name") {
    override fun valueOf(item: JobsWorkingSetConfig): String = item.name

    override fun setValue(item: JobsWorkingSetConfig, value: String) {
      item.name = value
    }
  }

  class UserNameColumn(private val crudable: Crudable) : ColumnInfo<JobsWorkingSetConfig, String>("Username") {
    override fun valueOf(item: JobsWorkingSetConfig): String = crudable.getByUniqueKey<Credentials>(item.connectionConfigUuid)?.username ?: ""

    override fun setValue(item: JobsWorkingSetConfig, value: String) {
      item.name = value
    }
  }

  class UrlColumn(private val crudable: Crudable) : ColumnInfo<JobsWorkingSetConfig, String>("z/OSMF URL") {
    override fun valueOf(item: JobsWorkingSetConfig): String = crudable.getByUniqueKey<ConnectionConfig>(item.connectionConfigUuid)?.url ?: ""
  }

  class ConnectionNameColumn(private val crudable: Crudable) : ColumnInfo<JobsWorkingSetConfig, String>("Connection Name") {
    override fun valueOf(item: JobsWorkingSetConfig): String = crudable.getByUniqueKey<ConnectionConfig>(item.connectionConfigUuid)?.name ?: ""

    override fun setValue(item: JobsWorkingSetConfig, value: String) {
      crudable.find<ConnectionConfig> { it.name == value }.findAnyNullable()?.let {
        item.connectionConfigUuid = it.uuid
      }
    }
  }
}
