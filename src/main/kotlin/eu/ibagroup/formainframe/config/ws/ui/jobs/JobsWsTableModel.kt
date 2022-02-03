package eu.ibagroup.formainframe.config.ws.ui.jobs

import eu.ibagroup.formainframe.config.ws.JobsWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.*
import eu.ibagroup.formainframe.utils.crudable.*

class JobsWsTableModel(crudable: Crudable) : AbstractWsTableModel<JobsWorkingSetConfig>(crudable) {

  override fun set(row: Int, item: JobsWorkingSetConfig) {
    get(row).jobsFilters = item.jobsFilters
    super.set(row, item)
  }

  override val clazz = JobsWorkingSetConfig::class.java

  init {
    initialize()
  }

}
