/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.r2z.Job
import eu.ibagroup.r2z.XIBMDataType

/**
 * Attributes containing information about the job.
 * @param jobInfo information about the job that was received from zosmf.
 * @param url resource URL based on original HTTP request.
 * @param requesters list of information objects with job filter and connection configuration inside.
 * @see JobsRequester
 * @author Valiantsin Krus
 */
data class RemoteJobAttributes(
  val jobInfo: Job,
  override val url: String,
  override val requesters: MutableList<JobsRequester>,
) : MFRemoteFileAttributes<JobsRequester> {
  override val name: String
    get() = jobInfo.jobName

  override val length: Long
    get() = 0L

  override var contentMode: XIBMDataType = XIBMDataType(XIBMDataType.Type.TEXT)

  /**
   * Clones current instance of job attributes.
   * @see FileAttributes.clone
   * @return cloned object.
   */
  override fun clone(): FileAttributes {
    return RemoteJobAttributes(
      jobInfo.clone(), url, requesters.map {
        JobsRequester(it.connectionConfig, it.jobsFilter)
      }.toMutableList()
    )
  }

}
