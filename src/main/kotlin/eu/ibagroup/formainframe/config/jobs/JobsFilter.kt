package eu.ibagroup.formainframe.config.jobs

import java.util.*

class JobsFilter(
  var owner: String = "",
  var prefix: String = "",
  var jobId: String = "",
  var maxCount: Int = -1,
  var userCorrelator: String = ""
){
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val jobsFilter = other as JobsFilter
    return owner == jobsFilter.owner &&
        prefix == jobsFilter.prefix &&
        jobId == jobsFilter.jobId &&
        maxCount == jobsFilter.maxCount &&
        userCorrelator == jobsFilter.userCorrelator
  }

  override fun hashCode(): Int {
    return Objects.hash(owner, prefix, jobId, maxCount, userCorrelator)
  }

  override fun toString(): String {
    return "JobsFilter(owner=$owner, prefix=$prefix, jobId=$jobId, maxCount=$maxCount, userCorrelator=$userCorrelator)"
  }


}