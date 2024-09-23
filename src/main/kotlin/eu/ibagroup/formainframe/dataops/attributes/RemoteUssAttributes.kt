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

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.getOwner
import eu.ibagroup.formainframe.config.connect.getUsername
import eu.ibagroup.formainframe.config.connect.tryToExtractOwnerFromConfig
import eu.ibagroup.formainframe.dataops.content.synchronizer.DEFAULT_BINARY_CHARSET
import eu.ibagroup.formainframe.utils.Copyable
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.clone
import org.zowe.kotlinsdk.FileMode
import org.zowe.kotlinsdk.FileModeValue
import org.zowe.kotlinsdk.UssFile
import org.zowe.kotlinsdk.XIBMDataType
import java.nio.charset.Charset

private const val CURRENT_DIR_NAME = "."

/**
 * Constructs path to file/folder depending on if object is file or folder
 * and if object is located in root directory or deeper in file hierarchy
 * @param rootPath path to object without object name itself
 * @param ussFile chosen file/folder
 * @return processed path to file/folder
 */
private fun constructPath(rootPath: String, ussFile: UssFile): String {
  return when {
    ussFile.name.isEmpty() || ussFile.name == CURRENT_DIR_NAME -> {
      rootPath
    }
    rootPath == USS_DELIMITER -> {
      rootPath + ussFile.name
    }
    else -> {
      rootPath + USS_DELIMITER + ussFile.name
    }
  }
}

const val USS_DELIMITER = "/"

/**
 * Data class which represents attributes of USS file/folder
 * @param path path to file/folder
 * @param isDirectory is object folder or not
 * @param fileMode all access modes of file/folder
 * @param url url to file/folder
 * @param requesters list of requesters
 * @param length size of file/folder
 * @param uid uid which identifies unique user number in system
 * @param owner owner of file/folder
 * @param gid gid which identifies unique group number in system
 * @param groupId group of file/folder
 * @param modificationTime last modification time
 * @param symlinkTarget link to file/folder, for link file type
 */
data class RemoteUssAttributes(
  val path: String,
  val isDirectory: Boolean,
  val fileMode: FileMode?,
  override val url: String,
  override val requesters: MutableList<UssRequester>,
  override val length: Long = 0L,
  val uid: Long? = null,
  val owner: String? = null,
  val gid: Long? = null,
  val groupId: String? = null,
  val modificationTime: String? = null,
  val symlinkTarget: String? = null
) : MFRemoteFileAttributes<ConnectionConfig, UssRequester>, Copyable {

  /**
   * Constructor for creating uss attributes object
   * @param rootPath path to file/folder
   * @param ussFile object representing file/folder
   * @param url url to file/folder
   * @param connectionConfig object which contains info about configuration to mainframe
   */
  constructor(rootPath: String, ussFile: UssFile, url: String, connectionConfig: ConnectionConfig) : this(
    path = constructPath(rootPath, ussFile),
    isDirectory = ussFile.isDirectory,
    fileMode = ussFile.fileMode,
    url = url,
    requesters = mutableListOf(UssRequester(connectionConfig)),
    length = ussFile.size ?: 0L,
    uid = ussFile.uid,
    owner = ussFile.user,
    gid = ussFile.gid,
    groupId = ussFile.groupId,
    modificationTime = ussFile.modificationTime,
    symlinkTarget = ussFile.target
  )

  /**
   * Clones uss attributes objects and returns copy of it
   */
  override fun clone(): FileAttributes {
    return this.clone(RemoteUssAttributes::class.java)
  }

  val isSymlink
    get() = symlinkTarget != null

  override val name
    get() = path.split(USS_DELIMITER).last()

  val parentDirPath
    get() = path.substring(1).split(USS_DELIMITER).dropLast(1).joinToString(separator = USS_DELIMITER)

  val isWritable: Boolean
    get() {
      val hasFileOwnerInRequesters = requesters.any {
        runCatching { tryToExtractOwnerFromConfig(it.connectionConfig) }.getOrNull()?.equals(owner, ignoreCase = true) ?: false
      }
      val mode = if (hasFileOwnerInRequesters) {
        fileMode?.owner
      } else {
        fileMode?.all
      }
      return mode == FileModeValue.WRITE.mode
              || mode == FileModeValue.WRITE_EXECUTE.mode
              || mode == FileModeValue.READ_WRITE.mode
              || mode == FileModeValue.READ_WRITE_EXECUTE.mode
    }

  val isReadable: Boolean
    get() {
      val hasFileOwnerInRequesters = requesters.any {
        runCatching { getOwner(it.connectionConfig) }.getOrNull()?.equals(owner, ignoreCase = true) ?: false
      }
      val mode = if (hasFileOwnerInRequesters) {
        fileMode?.owner
      } else {
        fileMode?.all
      }
      return mode == FileModeValue.READ.mode
              || mode == FileModeValue.READ_WRITE.mode
              || mode == FileModeValue.READ_EXECUTE.mode
              || mode == FileModeValue.READ_WRITE_EXECUTE.mode
    }

  val isExecutable: Boolean
    get() {
      val hasFileOwnerInRequesters = requesters.any { getOwner(it.connectionConfig).equals(owner, ignoreCase = true) }
      val mode = if (hasFileOwnerInRequesters) {
        fileMode?.owner
      } else {
        fileMode?.all
      }
      return mode == FileModeValue.EXECUTE.mode
              || mode == FileModeValue.READ_EXECUTE.mode
              || mode == FileModeValue.WRITE_EXECUTE.mode
              || mode == FileModeValue.READ_WRITE_EXECUTE.mode
    }

  var charset: Charset = DEFAULT_BINARY_CHARSET

  override var contentMode: XIBMDataType = XIBMDataType(XIBMDataType.Type.BINARY)

  override val isCopyPossible: Boolean
    get() = true

  override val isPastePossible: Boolean
    get() = isDirectory

}

/**
 * Util function to cast FileAttributes to RemoteUssAttributes or throw exception.
 * @throws IllegalArgumentException
 * @return source attributes that was cast to RemoteUssAttributes.
 */
fun FileAttributes?.toUssAttributes(fileName: String): RemoteUssAttributes {
  return castOrNull<RemoteUssAttributes>()
    ?: throw IllegalArgumentException("Cannot find attributes for file \"${fileName}\"")
}
