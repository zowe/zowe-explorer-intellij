/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

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