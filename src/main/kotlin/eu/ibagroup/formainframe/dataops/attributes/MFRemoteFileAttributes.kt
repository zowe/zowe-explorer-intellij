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

/** Interface to describe possible mainframe remote file attributes and interactions with them */
interface MFRemoteFileAttributes<R : Requester> : FileAttributes {

  val url: String

  val requesters: MutableList<R>

}

interface Requester {
  val connectionConfig: ConnectionConfig
}

/**
 * Check if two files are used with the same connection config by their attributes
 * @param other the other's file attributes to compare with the source one attributes
 */
inline fun <reified R : Requester> MFRemoteFileAttributes<R>.findCommonUrlConnections(other: MFRemoteFileAttributes<R>)
        : Collection<Pair<R, ConnectionConfig>> {
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
