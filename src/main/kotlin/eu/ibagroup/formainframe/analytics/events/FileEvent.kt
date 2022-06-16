/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.analytics.events

import eu.ibagroup.formainframe.dataops.attributes.*

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

private fun r2zFileTypeToAnalyticsFileType(fileType: eu.ibagroup.r2z.FileType): FileType {
  return if (fileType == eu.ibagroup.r2z.FileType.FILE) FileType.USS_FILE else FileType.USS_DIR
}

class FileEvent(
  private val fileType: FileType,
  private val fileAction: FileAction
) : AnalyticsEvent("files") {


  override fun getProps(): Map<String, String> {
    return mapOf(
      Pair("fileType", fileType.toString()),
      Pair("fileAction", fileAction.toString())
    )
  }

  constructor(fileAttributes: FileAttributes, fileAction: FileAction) : this(
    attributesToFileType(fileAttributes),
    fileAction
  )

  constructor(
    fileType: eu.ibagroup.r2z.FileType,
    fileAction: FileAction
  ) : this(r2zFileTypeToAnalyticsFileType(fileType), fileAction)
}

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

enum class FileAction(val value: String) {
  CREATE("CREATE"),
  DELETE("DELETE"),
  OPEN("OPEN"),
  RENAME("RENAME"),
  FORCE_RENAME("FORCE_RENAME"),
  COPY("COPY"),
  MOVE("MOVE"),
}
