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

package eu.ibagroup.formainframe.analytics.events

import eu.ibagroup.formainframe.dataops.attributes.*

/**
 * Determines value of FileType enum by file attributes.
 * @param fileAttributes attributes of the file to determine value of FileType enum.
 * @return determined value of FileType if it is possible to determine it (throws exception otherwise).
 */
private fun attributesToFileType(fileAttributes: FileAttributes): FileType {
  return when (fileAttributes) {
    is RemoteDatasetAttributes -> FileType.DATASET
    is RemoteUssAttributes -> {
      if (fileAttributes.isDirectory) FileType.USS_DIR else FileType.USS_FILE
    }
    is RemoteMemberAttributes -> FileType.MEMBER
    is RemoteSpoolFileAttributes -> FileType.SPOOL_FILE
    else -> throw IllegalArgumentException("FileType cannot be determined by $fileAttributes.")
  }
}

/**
 * Convert value of FileType enum from Zowe Kotlin SDK library to plugin FileType.
 * @see org.zowe.kotlinsdk.FileType
 * @see FileType
 * @param fileType value of Zowe Kotlin SDK FileType.
 * @return value of plugin FileType.
 */
private fun zoweKSDKFileTypeToAnalyticsFileType(fileType: org.zowe.kotlinsdk.FileType): FileType {
  return if (fileType == org.zowe.kotlinsdk.FileType.FILE) FileType.USS_FILE else FileType.USS_DIR
}

/**
 * Class for processing analytics events with files (e.g. create, open, delete and etc).
 * @param fileType type of the file the action was done on.
 * @param fileAction action that was done on the file.
 * @author Valiantsin Krus
 */
class FileEvent(
  private val fileType: FileType,
  private val fileAction: FileAction
) : AnalyticsEvent("files") {

  /**
   * Determines file event properties.
   * @return map with properties "fileType", "fileAction".
   */
  override fun getProps(): Map<String, String> {
    return mapOf(
      Pair("fileType", fileType.toString()),
      Pair("fileAction", fileAction.toString())
    )
  }

  /**
   * One more constructor for creating file event from file attributes.
   * @param fileAttributes type of the file (value of Zowe Kotlin SDK FileType) the action was done on.
   * @param fileAction action that was done on the file.
   */
  constructor(fileAttributes: FileAttributes, fileAction: FileAction) : this(
    attributesToFileType(fileAttributes),
    fileAction
  )

  /**
   * One more constructor for creating file event from Zowe Kotlin SDK FileType.
   * @param fileType attributes of the file the action was done on.
   * @param fileAction action that was done on the file.
   */
  constructor(
    fileType: org.zowe.kotlinsdk.FileType,
    fileAction: FileAction
  ) : this(zoweKSDKFileTypeToAnalyticsFileType(fileType), fileAction)
}

/**
 * Possible type of file that can be used as a property of FileEvent in analytics.
 * @see FileEvent
 * @author Valiantsin Krus
 */
enum class FileType(val value: String) {
  USS_FILE("USS_FILE"),
  USS_DIR("USS_DIR"),
  DATASET("DATASET"),
  MEMBER("MEMBER"),
  SPOOL_FILE("SPOOL_FILE");

  override fun toString(): String {
    return value
  }
}

/**
 * Actions on the files that can be tracked in analytics.
 * @see FileEvent
 * @author Valiantsin Krus
 */
enum class FileAction(val value: String) {
  CREATE("CREATE"),
  DELETE("DELETE"),
  OPEN("OPEN"),
  RENAME("RENAME"),
  COPY("COPY"),
  MOVE("MOVE");
}
