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

package eu.ibagroup.formainframe.dataops.operations.mover

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.UnitOperation
import eu.ibagroup.formainframe.dataops.attributes.*
import eu.ibagroup.formainframe.explorer.Explorer

/**
 * Class which represents move or copy operation.
 * @param source virtual file to be moved or copied (source file).
 * @param sourceAttributes attributes of the source virtual file.
 * @param destination virtual file to which source will be moved or copied (destination file).
 * @param destinationAttributes attributes of the destination virtual file.
 * @param isMove file move flag.
 * @param forceOverwriting force file overwrite flag.
 * @param newName a new name of the source file.
 * @param explorer represents explorer object.
 */
class MoveCopyOperation(
  val source: VirtualFile,
  val sourceAttributes: FileAttributes?,
  val destination: VirtualFile,
  val destinationAttributes: FileAttributes?,
  val isMove: Boolean,
  val forceOverwriting: Boolean,
  val newName: String?,
  val explorer: Explorer<ConnectionConfig, *>? = null
) : UnitOperation {
  constructor(
    source: VirtualFile,
    destination: VirtualFile,
    isMove: Boolean,
    forceOverwriting: Boolean,
    newName: String?,
    dataOpsManager: DataOpsManager,
    explorer: Explorer<ConnectionConfig, *>? = null
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

  override fun toString(): String {
    return "MoveCopyOperation(source=$source, sourceAttributes=$sourceAttributes, destination=$destination, destinationAttributes=$destinationAttributes, isMove=$isMove, forceOverwriting=$forceOverwriting, newName=$newName, explorer=$explorer)"
  }
}

/**
 * Check if the source file and destination file use the same connection config by their attributes.
 * If the files have the same connection configs, then a non-empty collection is returned.
 * @param dataOpsManager instance of DataOpsManager service.
 * @return collection that contains pairs of requester and connection config values.
 */
@Suppress("UNCHECKED_CAST")
fun MoveCopyOperation.commonUrls(dataOpsManager: DataOpsManager): Collection<Pair<Requester<ConnectionConfig>, ConnectionConfig>> {
  val sourceAttributesPrepared = if (sourceAttributes is RemoteMemberAttributes) {
    sourceAttributes.getLibraryAttributes(dataOpsManager)
      ?: throw IllegalArgumentException("Cannot find lib attributes")
  } else {
    sourceAttributes
  }
  return (sourceAttributesPrepared as MFRemoteFileAttributes<ConnectionConfig, Requester<ConnectionConfig>>)
    .findCommonUrlConnections(destinationAttributes as MFRemoteFileAttributes<ConnectionConfig, Requester<ConnectionConfig>>)
}
