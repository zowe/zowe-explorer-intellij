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

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase

/** Interface to describe possible mainframe remote file attributes and interactions with them */
interface MFRemoteFileAttributes<Connection: ConnectionConfigBase, R : Requester<Connection>> : FileAttributes {

  val url: String

  val requesters: MutableList<R>

}

/** Interface that is necessary to implement requests to zosmf for specific entity (uss files, datasets, jobs etc.) */
interface Requester<Connection: ConnectionConfigBase> {
  val connectionConfig: Connection
}

/**
 * Check if two files are used with the same connection config by their attributes
 * @param other the other's file attributes to compare with the source one attributes
 */
inline fun <reified R : Requester<ConnectionConfig>>
    MFRemoteFileAttributes<ConnectionConfig, R>.findCommonUrlConnections(
      other: MFRemoteFileAttributes<ConnectionConfig, R>
    ): Collection<Pair<R, ConnectionConfig>> {
  val thisRequestersWithUrlConnection = requesters.map {
    Pair(it, it.connectionConfig)
  }
  val otherUrlConnections = other.requesters.map {
    it.connectionConfig
  }
  return thisRequestersWithUrlConnection.filter { pair ->
    otherUrlConnections.any {
      pair.second.url == it.url && pair.second.isAllowSelfSigned == it.isAllowSelfSigned
    }
  }
}
