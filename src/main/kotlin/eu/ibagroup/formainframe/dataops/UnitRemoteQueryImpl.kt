/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops

import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.explorer.ui.UssNode
import eu.ibagroup.formainframe.utils.UNIT_CLASS

/**
 * Class which represents Unit type of the Result of remote query
 */
data class UnitRemoteQueryImpl<Connection: ConnectionConfigBase, R>(
  override val request: R,
  override val connectionConfig: Connection
) : RemoteQuery<Connection, R, Unit> {
  val sortKeys = mutableListOf(SortQueryKeys.DATE, SortQueryKeys.ASCENDING)
  var requester: UssNode? = null
  override val resultClass: Class<out Unit>
    get() = UNIT_CLASS
}

/**
 * Enum class represents the sorting keys which is currently enabled for particular Node
 */
enum class SortQueryKeys(private val sortType: String) {
  NAME("name"),
  TYPE("type"),
  DATE("date"),
  NONE("none"),
  ASCENDING("ascending"),
  DESCENDING("descending");

  override fun toString(): String {
    return sortType
  }

}