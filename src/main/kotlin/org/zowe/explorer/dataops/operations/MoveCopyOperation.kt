/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.operations

import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.UnitOperation
import org.zowe.explorer.dataops.attributes.*
import org.zowe.explorer.explorer.Explorer

class MoveCopyOperation(
  val source: VirtualFile,
  val sourceAttributes: FileAttributes?,
  val destination: VirtualFile,
  val destinationAttributes: FileAttributes?,
  val isMove: Boolean,
  val forceOverwriting: Boolean,
  val newName: String?,
  val explorer: Explorer<*>? = null
) : UnitOperation {
  constructor(
    source: VirtualFile,
    destination: VirtualFile,
    isMove: Boolean,
    forceOverwriting: Boolean,
    newName: String?,
    dataOpsManager: DataOpsManager,
    explorer: Explorer<*>? = null
  ) : this(
    source = source,
    sourceAttributes = dataOpsManager.tryToGetAttributes(source),
    destination = destination,
    destinationAttributes = dataOpsManager.tryToGetAttributes(destination),
    isMove = isMove,
    forceOverwriting = forceOverwriting,
    newName = newName,
    explorer = explorer
  )
}

@Suppress("UNCHECKED_CAST")
fun MoveCopyOperation.commonUrls(dataOpsManager: DataOpsManager): Collection<Pair<Requester, ConnectionConfig>> {
  val sourceAttributesPrepared = if (sourceAttributes is RemoteMemberAttributes) {
    sourceAttributes.getLibraryAttributes(dataOpsManager) ?: throw IllegalArgumentException("Cannot find lib attributes")
  } else {
    sourceAttributes
  }
  return (sourceAttributesPrepared as MFRemoteFileAttributes<Requester>)
    .findCommonUrlConnections(destinationAttributes as MFRemoteFileAttributes<Requester>)
}
