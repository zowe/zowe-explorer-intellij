/*
 * Copyright (c) 2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package org.zowe.explorer.v3.state.config.jes

/**
 * Job filter config item. Describes the job filter to display jobs for
 * @property owner the job owner to search jobs by
 * @property prefix the job name prefix to search jobs by
 * @property jobId the exact job ID to search a job by
 */
data class JobFilterConfigItem(
  var owner: String = "",
  var prefix: String = "",
  var jobId: String = ""
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is JobFilterConfigItem) return false

    if (owner != other.owner) return false
    if (prefix != other.prefix) return false
    if (jobId != other.jobId) return false

    return true
  }

  override fun hashCode(): Int {
    var result = owner.hashCode()
    result = 31 * result + prefix.hashCode()
    result = 31 * result + jobId.hashCode()
    return result
  }
}
