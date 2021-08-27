package eu.ibagroup.formainframe.config.jobs

import eu.ibagroup.r2z.ActiveStatus
import eu.ibagroup.r2z.ExecData
import java.util.*

class JobsFilter(
  var owner: String = "",
  var prefix: String = "",
  var jobId: String = "",
  var userCorrelator: String = "",
  var execData: ExecData = ExecData.NO,
  var status: ActiveStatus = ActiveStatus.ACTIVE
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val jobsFilter = other as JobsFilter
    return owner == jobsFilter.owner &&
        prefix == jobsFilter.prefix &&
        jobId == jobsFilter.jobId &&
        userCorrelator == jobsFilter.userCorrelator &&
        execData == jobsFilter.execData &&
        status == jobsFilter.status
  }

  override fun hashCode(): Int {
    return Objects.hash(owner, prefix, jobId, userCorrelator, execData, status)
  }

  override fun toString(): String {
    return "JobsFilter(owner=$owner, prefix=$prefix, jobId=$jobId, userCorrelator=$userCorrelator, execData=$execData, status=$status)"
  }

}
