package eu.ibagroup.formainframe.config.jobs

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.utils.crudable.EntityWithUuid
import eu.ibagroup.formainframe.utils.crudable.annotations.Column
import eu.ibagroup.formainframe.utils.crudable.annotations.ForeignKey
import org.jetbrains.annotations.NotNull
import java.util.*


class JobsFilter {

  var owner: String = ""

  var prefix: String = ""

  var jobId: String = ""

  var userCorrelatorFilter: String = ""

  constructor()

  constructor(owner: String, prefix: String, jobId: String) {
    this.owner = owner
    this.prefix = prefix
    this.jobId = jobId
  }


  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val filter = other as JobsFilter
    return owner == filter.owner && prefix == filter.prefix && jobId == filter.jobId && userCorrelatorFilter == filter.userCorrelatorFilter
  }

  override fun hashCode(): Int {
    return Objects.hash(owner, prefix, jobId, userCorrelatorFilter)
  }

  override fun toString(): String = if (jobId.isEmpty()) {
    "Owner = $owner, Prefix = $prefix"
  } else {
    "JobID = $jobId"
  }


}
