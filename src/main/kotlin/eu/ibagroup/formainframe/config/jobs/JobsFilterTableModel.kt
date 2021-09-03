/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.config.jobs

import com.intellij.util.ui.ColumnInfo
import eu.ibagroup.formainframe.common.ui.CrudableTableModel
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.utils.crudable.*
import eu.ibagroup.formainframe.utils.toMutableList

class JobsFilterTableModel(crudable: Crudable) : CrudableTableModel<JobsFilter>(crudable) {

  init {
    columnInfos = arrayOf(
      FilterColumn,
      ConnectionNameColumn(crudable),
      UrlColumn(crudable),
    )
  }

  override fun fetch(crudable: Crudable): MutableList<JobsFilter> = crudable.getAll<JobsFilter>().toMutableList()


  override fun onAdd(crudable: Crudable, value: JobsFilter): Boolean = crudable.add(value).isPresent

  override fun onUpdate(crudable: Crudable, value: JobsFilter): Boolean = crudable.update(value).isPresent

  override fun onDelete(crudable: Crudable, value: JobsFilter) {
    crudable.delete(value)
  }

  override fun onApplyingMergedCollection(crudable: Crudable, merged: MergedCollections<JobsFilter>) = crudable.applyMergedCollections(merged)

  override val clazz = JobsFilter::class.java

  object FilterColumn: ColumnInfo<JobsFilter, String>("Name") {

    override fun valueOf(item: JobsFilter): String = item.toString()

  }

  class UrlColumn(private val crudable: Crudable) : ColumnInfo<JobsFilter, String>("z/OSMF URL") {

    override fun valueOf(item: JobsFilter): String = crudable.getByUniqueKey<ConnectionConfig>(""/*item.connectionConfigUuid*/)?.url ?: ""

  }

  class ConnectionNameColumn(private val crudable: Crudable) : ColumnInfo<JobsFilter, String>("Connection Name") {

    override fun valueOf(item: JobsFilter): String = crudable.getByUniqueKey<ConnectionConfig>(""/*item.connectionConfigUuid*/)?.name ?: ""

  }
}
