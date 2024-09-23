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

package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SmartList
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.mergeWith
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.createAttributes
import org.zowe.kotlinsdk.XIBMDataType

/**
 * Factory class which builds remote uss attributes service instance. Defined in plugin.xml.
 */
class RemoteUssAttributesServiceFactory : AttributesServiceFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): AttributesService<*, *> {
    return RemoteUssAttributesService(dataOpsManager)
  }
}

/**
 * Implementation of attributes service for working with uss files.
 * @see AttributesService
 * @see MFRemoteAttributesServiceBase
 */
class RemoteUssAttributesService(
  dataOpsManager: DataOpsManager
) : MFRemoteAttributesServiceBase<ConnectionConfig, RemoteUssAttributes>(dataOpsManager) {

  override val attributesClass = RemoteUssAttributes::class.java

  override val subFolderName = "USS"

  /**
   * Method to build unique attributes for a uss files.
   * @param attributes - attributes from which to build unique remote attributes.
   * @return a new instance of RemoteUssAttributes.
   */
  override fun buildUniqueAttributes(attributes: RemoteUssAttributes): RemoteUssAttributes {
    return RemoteUssAttributes(
      path = attributes.path,
      isDirectory = attributes.isDirectory,
      fileMode = null,
      url = attributes.url,
      requesters = SmartList(),
      length = 0L,
      uid = attributes.uid,
      owner = attributes.owner,
      gid = attributes.gid,
      groupId = attributes.groupId,
      symlinkTarget = null
    )
  }

  /**
   * Method to build a new remote uss attributes by merging them with old attributes (by mask requester).
   * @param oldAttributes - old attributes from which to merge.
   * @param newAttributes - new attributes to be merged.
   * @return a new instance of RemoteUssAttributes.
   */
  override fun mergeAttributes(
    oldAttributes: RemoteUssAttributes,
    newAttributes: RemoteUssAttributes
  ): RemoteUssAttributes {
    return RemoteUssAttributes(
      path = newAttributes.path,
      isDirectory = newAttributes.isDirectory,
      fileMode = newAttributes.fileMode,
      url = newAttributes.url,
      requesters = oldAttributes.requesters.mergeWith(newAttributes.requesters),
      length = newAttributes.length,
      uid = newAttributes.uid,
      owner = newAttributes.owner,
      gid = newAttributes.gid,
      groupId = newAttributes.groupId,
      modificationTime = newAttributes.modificationTime,
      symlinkTarget = newAttributes.symlinkTarget
    )
  }

  /**
   * Reassign the attributes of the file after a URL folder renaming.
   * @see MFRemoteAttributesServiceBase.reassignAttributesAfterUrlFolderRenaming
   */
  override fun reassignAttributesAfterUrlFolderRenaming(
    file: MFVirtualFile,
    oldAttributes: RemoteUssAttributes,
    newAttributes: RemoteUssAttributes
  ) {
    fsModel.setWritable(file, newAttributes.isWritable)
    file.isReadable = newAttributes.isReadable
    if (oldAttributes.name != newAttributes.name) {
      file.rename(this, newAttributes.name)
    }
    if (oldAttributes.parentDirPath != newAttributes.parentDirPath) {
      var current = fsRoot
      createPathChain(newAttributes).dropLast(1).map { nameWithFileAttr ->
        findOrCreate(current, nameWithFileAttr).also { current = it }
      }
      file.move(this, current)
    }
  }

  /**
   * Updates the value of the writable flag to false after file content changed,
   * if the content mode is binary.
   * @param vFile virtual file for flag update.
   * @param attributes uss file attributes [RemoteUssAttributes].
   */
  fun updateWritableFlagAfterContentChanged(vFile: VirtualFile, attributes: RemoteUssAttributes) {
    if (attributes.contentMode.type == XIBMDataType.Type.BINARY) {
      vFile.isWritable = false
    } else {
      vFile.isWritable = attributes.isWritable
    }
  }

  /**
   * Method to build a list of paths elements from given attributes.
   * @param attributes - attributes to build a path element chain.
   * @return list of path element seed.
   */
  override fun continuePathChain(attributes: RemoteUssAttributes): List<PathElementSeed> {
    return if (attributes.path != "/") {
      val pathTokens = attributes.path.substring(1).split("/")
      pathTokens.dropLast(1).map { PathElementSeed(it, createAttributes(directory = true)) }.plus(
        listOf(
          PathElementSeed(
            name = pathTokens.last(),
            fileAttributes = createAttributes(directory = attributes.isDirectory, writable = attributes.isWritable),
            postCreateAction = {
              isReadable = attributes.isReadable
            }
          )
        )
      )
    } else {
      listOf()
    }
  }
}
