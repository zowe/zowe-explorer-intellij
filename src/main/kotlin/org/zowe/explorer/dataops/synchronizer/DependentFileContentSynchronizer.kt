/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.synchronizer

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.*
import org.zowe.explorer.dataops.exceptions.CallException
import java.io.IOException

abstract class DependentFileContentSynchronizer<
    VFile : VirtualFile,
    InfoType, R : Requester,
    Attributes : DependentFileAttributes<InfoType, VFile>,
    ParentAttributes : MFRemoteFileAttributes<R>>
  (dataOpsManager: DataOpsManager, private val log: Logger) :
  RemoteAttributesContentSynchronizerBase<Attributes>(dataOpsManager) {

  abstract val parentAttributesClass: Class<out ParentAttributes>

  private val parentAttributesService by lazy { dataOpsManager.getAttributesService(parentAttributesClass, vFileClass) }

  open val parentFileType: String = "File"

  override fun fetchRemoteContentBytes(attributes: Attributes, progressIndicator: ProgressIndicator?): ByteArray {
    log.info("Fetch remote content for $attributes")
    val parentLib = attributes.parentFile
    val parentAttributes = parentAttributesService.getAttributes(parentLib)
      ?: throw IOException("Cannot find parent ${parentFileType.toLowerCase()} attributes for ${parentFileType.toLowerCase()} ${parentLib.path}")
    log.info("$parentFileType attributes are $parentAttributes")
    var throwable = Throwable("Unknown")
    var content: ByteArray? = null
    for (requester in parentAttributes.requesters) {
      try {
        log.info("Trying to execute a call using $requester")
        val response = executeGetContentRequest(attributes, parentAttributes, progressIndicator, requester)
        if (response.isSuccessful) {
          log.info("Content has been fetched successfully")
          content = response.toBytes()
          break
        } else {
          throwable = CallException(response, "Cannot fetch data from ${parentAttributes.name}(${attributes.name})")
        }
      } catch (t: Throwable) {
        throwable = t
      }
    }
    return content ?: throw throwable
  }

  override fun uploadNewContent(attributes: Attributes, newContentBytes: ByteArray) {
    log.info("Upload remote content for $attributes")
    val parentLib = attributes.parentFile
    val parentAttributes = parentAttributesService.getAttributes(parentLib)
      ?: throw IOException("Cannot find parent ${parentFileType.toLowerCase()} attributes for ${parentFileType.toLowerCase()} ${parentLib.path}")
    log.info("$parentFileType attributes are $parentAttributes")
    var throwable: Throwable? = null
    for (requester in parentAttributes.requesters) {
      try {
        log.info("Trying to execute a call using $requester")
        val response = executePutContentRequest(attributes, parentAttributes, requester, newContentBytes) ?: return
        if (response.isSuccessful) {
          log.info("Content has been uploaded successfully")
          throwable = null
          break
        } else {
          throwable = CallException(response, "Cannot upload data to ${parentAttributes.name}(${attributes.name})")
        }
      } catch (t: Throwable) {
        throwable = t
      }
    }
    if (throwable != null) {
      throw throwable
    }
  }

  abstract fun executeGetContentRequest(
    attributes: Attributes,
    parentAttributes: ParentAttributes,
    progressIndicator: ProgressIndicator?,
    requester: Requester
  ): retrofit2.Response<*>

  abstract fun executePutContentRequest(
    attributes: Attributes,
    parentAttributes: ParentAttributes,
    requester: Requester,
    newContentBytes: ByteArray
  ): retrofit2.Response<Void>?

}

private fun  retrofit2.Response<*>.toBytes(): ByteArray? {
  return when (val b = body()){
    is ByteArray -> b
    else -> body()?.toString()?.removeLastNewLine()?.toByteArray()
  }
}
