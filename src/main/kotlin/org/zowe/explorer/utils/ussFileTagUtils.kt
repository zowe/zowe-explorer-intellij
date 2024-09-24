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

package org.zowe.explorer.utils

import com.ibm.mq.headers.CCSID
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.content.synchronizer.DEFAULT_BINARY_CHARSET
import org.zowe.explorer.dataops.operations.uss.ChangeFileTagOperation
import org.zowe.explorer.dataops.operations.uss.ChangeFileTagOperationParams
import org.zowe.explorer.telemetry.NotificationsService
import okhttp3.ResponseBody
import okhttp3.internal.indexOfNonWhitespace
import org.zowe.kotlinsdk.FileTagList
import org.zowe.kotlinsdk.TagAction
import org.zowe.kotlinsdk.UssFileDataType
import java.nio.charset.Charset

/**
 * Checks if the uss file tag is set.
 * @param attributes uss file attributes.
 */
fun checkUssFileTag(attributes: RemoteUssAttributes) {
  val charset = getUssFileTagCharset(attributes)
  if (charset != null) {
    attributes.charset = charset
  } else {
    attributes.charset = DEFAULT_BINARY_CHARSET
  }
}

/**
 * Get encoding from file tag or return null if it doesn't exist.
 * @param attributes uss file attributes.
 */
fun getUssFileTagCharset(attributes: RemoteUssAttributes): Charset? {
  val responseBody = listUssFileTag(attributes)
  val body = responseBody?.string()
  if (body?.isNotEmpty() == true) {
    val stdout = org.zowe.kotlinsdk.gson.fromJson(body, FileTagList::class.java).stdout[0]
    if (stdout.indexOf("t ") > -1) {
      val startPos = stdout.indexOfNonWhitespace(1)
      val endPos = stdout.indexOf(' ', startPos)
      val tagCharset = stdout.substring(startPos, endPos)
      val ccsid = CCSID.getCCSID(tagCharset)
      val codePage = CCSID.getCodepage(ccsid)
      return Charset.forName(codePage)
    }
  }
  return null
}

/**
 * Sets or removes uss file encoding to the file tag.
 * @param attributes uss file attributes.
 */
fun updateFileTag(attributes: RemoteUssAttributes) {
  if (attributes.charset == DEFAULT_BINARY_CHARSET) {
    removeUssFileTag(attributes)
  } else {
    setUssFileTag(attributes)
  }
}

/**
 * Lists the contents of the uss file tag.
 * @param attributes uss file attributes.
 * @return response body or null if empty.
 */
fun listUssFileTag(attributes: RemoteUssAttributes): ResponseBody? {
  var response: ResponseBody? = null

  runCatching {
    val connectionConfig = attributes.requesters[0].connectionConfig
    response = DataOpsManager.getService().performOperation(
      operation = ChangeFileTagOperation(
        request = ChangeFileTagOperationParams(
          filePath = attributes.path,
          action = TagAction.LIST,
        ),
        connectionConfig = connectionConfig
      ),
    )
  }.onFailure {
    NotificationsService.getService().notifyError(it, custTitle = "Cannot list a USS file tag for ${attributes.path}")
  }
  return response
}

/**
 * Sets the file tag to the current uss file encoding by attributes.
 * @param attributes uss file attributes.
 */
fun setUssFileTag(attributes: RemoteUssAttributes) {
  setUssFileTagCommon(attributes.charset.name(), attributes.path, attributes.requesters[0].connectionConfig)
}

/**
 * Sets the file tag to the current uss file encoding by common params.
 * @see setUssFileTagCommon
 */
fun setUssFileTag(charset: String, path: String, connectionConfig: ConnectionConfig) {
  setUssFileTagCommon(charset, path, connectionConfig)
}

/**
 * Common func to set the file tag to the current uss file encoding.
 * If the encoding name starts with 'x-IBM', then the 'x-' is truncated from the name
 * (this is necessary to match between encoding names).
 * @param charsetName file charset name to set.
 * @param filePath path to uss file.
 * @param connectionConfig connection config on which the file is located.
 */
private fun setUssFileTagCommon(charsetName: String, filePath: String, connectionConfig: ConnectionConfig) {
  var charset = charsetName
  if (charset.contains("x-IBM")) {
    charset = charset.substring(2)
  }
  val ccsid = CCSID.getCCSID(charset)
  val codeSet = ccsid.toString()

  runCatching {
    DataOpsManager.getService().performOperation(
      operation = ChangeFileTagOperation(
        request = ChangeFileTagOperationParams(
          filePath = filePath,
          action = TagAction.SET,
          type = UssFileDataType.TEXT,
          codeSet = codeSet
        ),
        connectionConfig = connectionConfig
      )
    )
  }.onFailure {
    NotificationsService.getService().notifyError(it, custTitle = "Cannot set a USS file tag for $filePath")
  }
}

/**
 * Removes the uss file tag value.
 * @param attributes uss file attributes.
 */
fun removeUssFileTag(attributes: RemoteUssAttributes) {
  runCatching {
    val connectionConfig = attributes.requesters[0].connectionConfig
    DataOpsManager.getService().performOperation(
      operation = ChangeFileTagOperation(
        request = ChangeFileTagOperationParams(
          filePath = attributes.path,
          action = TagAction.REMOVE,
        ),
        connectionConfig = connectionConfig
      )
    )
  }.onFailure {
    NotificationsService.getService().notifyError(it, custTitle = "Cannot remove a USS file tag for ${attributes.path}")
  }
}

private val unsupportedEncodings = listOf(
  "GBK", "x-IBM300", "UTF-16"
)

/**
 * Returns a list of supported encodings to set in a file tag.
 */
fun getSupportedEncodings(): List<Charset> {
  val ccsids = CCSID.getCCSIDs().toList()
  val encodings = ccsids.mapNotNull {
    runCatching {
      val codepage = CCSID.getCodepage(it as Int)
      Charset.forName(codepage)
    }.getOrNull()
  }.distinct().filter {
    !unsupportedEncodings.contains(it.name())
  }
  return encodings
}
