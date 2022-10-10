/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops

import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.utils.UNIT_CLASS

/**
 * Query for fetching files batch by batch (first 100 -> load more -> next 100).
 *  @param request object with all necessary information to fetch files.
 *  @param connectionConfig connection to mainframe with which to communicate.
 *  @param totalRows total rows count for specified query.
 *  @param alreadyFetched count of ptf that was fetched for this query.
 *  @param start file name from which to get selection (will be the first file of fetched files list).
 *  @param fetchNeeded identifies if all files have been fetched or not.
 *  @author Valiantsin Krus
 */
class BatchedRemoteQuery<R>(
  override val request: R,
  override val connectionConfig: ConnectionConfig,
  var totalRows: Int? = null,
  var alreadyFetched: Int = 0,
  var start: String? = null,
  var fetchNeeded: Boolean = true
): RemoteQuery<R, Unit> {
  override val resultClass: Class<out Unit>
    get() = UNIT_CLASS

  /**
   * Sets default values for params that identify current state of fetching.
   */
  fun clear () {
    totalRows = null
    start = null
    fetchNeeded = true
    alreadyFetched = 0
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as BatchedRemoteQuery<*>

    if (connectionConfig != other.connectionConfig) return false
    if (request != other.request) return false

    return true
  }

  override fun hashCode(): Int {
    var result = connectionConfig.hashCode()
    result = 31 * result + request.hashCode()
    return result
  }
}
