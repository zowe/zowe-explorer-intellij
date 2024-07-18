/*
 * Copyright (c) 2020 IBA Group.
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

package org.zowe.explorer.dataops.attributes

import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.ConnectionConfigBase

/** Interface to describe possible mainframe remote file attributes and interactions with them */
interface MFRemoteFileAttributes<Connection : ConnectionConfigBase, R : Requester<Connection>> : FileAttributes {

  val url: String

  val requesters: MutableList<R>

}

/**
 * Check if two files are used with the same connection config by their attributes
 * @param other the other's file attributes to compare with the source one attributes
 */
fun <R : Requester<ConnectionConfig>> MFRemoteFileAttributes<ConnectionConfig, R>.findCommonUrlConnections(
  other: MFRemoteFileAttributes<ConnectionConfig, R>
): Collection<Pair<R, ConnectionConfig>> {
  val thisRequestersWithUrlConnection = requesters.map { it to it.connectionConfig }
  val otherUrlConnections = other.requesters.map { it.connectionConfig }
  return thisRequestersWithUrlConnection.filter { (_, connectionConfig) ->
    otherUrlConnections.any {
      connectionConfig.url == it.url && connectionConfig.isAllowSelfSigned == it.isAllowSelfSigned
    }
  }
}
