/*
 * Copyright (c) 2020-2024 IBA Group.
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

package eu.ibagroup.formainframe.dataops

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.dataops.fetch.LibraryQuery
import eu.ibagroup.formainframe.dataops.sort.SortQueryKeys
import eu.ibagroup.formainframe.utils.UNIT_CLASS

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
) : RemoteQuery<ConnectionConfig, R, Unit>, SortableQuery {
  override val resultClass: Class<out Unit>
    get() = UNIT_CLASS

  override val sortKeys: List<SortQueryKeys>
    get() = when (request) {
      is DSMask -> mutableListOf(SortQueryKeys.DATASET_MODIFICATION_DATE, SortQueryKeys.ASCENDING)
      is LibraryQuery -> mutableListOf(SortQueryKeys.MEMBER_MODIFICATION_DATE, SortQueryKeys.ASCENDING)
      else -> mutableListOf()
    }

  /**
   * Sets default values for params that identify current state of fetching.
   */
  fun clear() {
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
