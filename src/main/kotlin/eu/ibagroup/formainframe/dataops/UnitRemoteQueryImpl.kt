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

import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.dataops.fetch.UssQuery
import eu.ibagroup.formainframe.dataops.sort.SortQueryKeys
import eu.ibagroup.formainframe.utils.UNIT_CLASS

/**
 * Class which represents Unit type of the Result of remote query
 */
data class UnitRemoteQueryImpl<Connection : ConnectionConfigBase, R>(
  override val request: R,
  override val connectionConfig: Connection,
) : RemoteQuery<Connection, R, Unit>, SortableQuery {
  override val resultClass: Class<out Unit>
    get() = UNIT_CLASS
  override val sortKeys: List<SortQueryKeys>
    get() = when (request) {
      is UssQuery -> mutableListOf(SortQueryKeys.FILE_MODIFICATION_DATE, SortQueryKeys.ASCENDING)
      is JobsFilter -> mutableListOf(SortQueryKeys.JOB_CREATION_DATE, SortQueryKeys.ASCENDING)
      else -> mutableListOf()
    }
}
