package eu.ibagroup.formainframe.config.ws

import eu.ibagroup.formainframe.utils.crudable.annotations.Column
import eu.ibagroup.formainframe.utils.isTheSameAs

class JobsWorkingSetConfig: WorkingSetConfig {

  @Column
  var jobsFilters: MutableCollection<JobsFilter> = mutableListOf()

  constructor() : super()

  constructor(name: String, connectionConfigUuid: String, jobsFilters: MutableCollection<JobsFilter>) : super(name, connectionConfigUuid) {
    this.jobsFilters = jobsFilters
  }
  constructor(
    uuid: String,
    name: String,
    connectionConfigUuid: String,
    jobsFilters: MutableCollection<JobsFilter>
  ) : super(name, connectionConfigUuid, uuid) {
    this.jobsFilters = jobsFilters
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as JobsWorkingSetConfig

    if (name != other.name) return false
    if (connectionConfigUuid != other.connectionConfigUuid) return false
    if (!(jobsFilters isTheSameAs other.jobsFilters)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + connectionConfigUuid.hashCode()
    result = 31 * result + jobsFilters.hashCode()
    return result
  }

  override fun toString(): String {
    return "JobsWorkingSetConfig(name='$name', connectionConfigUuid='$connectionConfigUuid', jobsFilters=$jobsFilters)"
  }
}
