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

package eu.ibagroup.formainframe.dataops.content.synchronizer

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.DependentFileAttributes
import eu.ibagroup.formainframe.dataops.attributes.MFRemoteFileAttributes
import eu.ibagroup.formainframe.dataops.attributes.Requester
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import java.io.IOException

/** Abstract content synchronizer class for the files that are dependent for others */
abstract class DependentFileContentSynchronizer<
        VFile : VirtualFile,
        InfoType, R : Requester<ConnectionConfig>,
        Attributes : DependentFileAttributes<InfoType, VFile>,
        ParentAttributes : MFRemoteFileAttributes<ConnectionConfig, R>
        >(dataOpsManager: DataOpsManager, private val log: Logger) :
  RemoteAttributedContentSynchronizer<Attributes>(dataOpsManager) {

  abstract val parentAttributesClass: Class<out ParentAttributes>

  private val parentAttributesService by lazy { dataOpsManager.getAttributesService(parentAttributesClass, vFileClass) }

  open val parentFileType: String = "File"

  /**
   * Fetch remote content bytes for the dependent file
   * @param attributes the attributes of the file to get the parent file and the name of the file
   * @param progressIndicator a progress indicator for the operation
   * @return content bytes after the operation is completed
   */
  override fun fetchRemoteContentBytes(attributes: Attributes, progressIndicator: ProgressIndicator?): ByteArray {
    log.info("Fetch remote content for $attributes")
    val parentLib = attributes.parentFile
    val parentAttributes = parentAttributesService.getAttributes(parentLib)
      ?: throw IOException("Cannot find parent ${parentFileType.lowercase()} attributes for ${parentFileType.lowercase()} ${parentLib.path}")
    log.info("$parentFileType attributes are $parentAttributes")
    var throwable = Throwable("Unknown")
    var content: ByteArray? = null
    for (requester in parentAttributes.requesters) {
      try {
        log.info("Trying to execute a call using $requester")
        val response = executeGetContentRequest(attributes, parentAttributes, requester, progressIndicator)
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

  /**
   * Upload new content bytes of the dependent file to the mainframe
   * @param attributes the attributes of the file to get the parent file and the name of the file
   * @param newContentBytes the new content bytes to upload
   * @param progressIndicator a progress indicator for the operation
   */
  override fun uploadNewContent(
    attributes: Attributes,
    newContentBytes: ByteArray,
    progressIndicator: ProgressIndicator?
  ) {
    log.info("Upload remote content for $attributes")
    val parentLib = attributes.parentFile
    val parentAttributes = parentAttributesService.getAttributes(parentLib)
      ?: throw IOException("Cannot find parent ${parentFileType.lowercase()} attributes for ${parentFileType.lowercase()} ${parentLib.path}")
    log.info("$parentFileType attributes are $parentAttributes")
    var throwable: Throwable? = null
    for (requester in parentAttributes.requesters) {
      try {
        log.info("Trying to execute a call using $requester")
        val response =
          executePutContentRequest(attributes, parentAttributes, requester, newContentBytes, progressIndicator)
            ?: return
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
    requester: Requester<ConnectionConfig>,
    progressIndicator: ProgressIndicator?
  ): retrofit2.Response<*>

  abstract fun executePutContentRequest(
    attributes: Attributes,
    parentAttributes: ParentAttributes,
    requester: Requester<ConnectionConfig>,
    newContentBytes: ByteArray,
    progressIndicator: ProgressIndicator?
  ): retrofit2.Response<Void>?

}

/** Convert the retrofit response to byte array */
private fun retrofit2.Response<*>.toBytes(): ByteArray? {
  return when (val b = body()) {
    is ByteArray -> b
    else -> body()?.toString()?.removeLastNewLine()?.toByteArray()
  }
}
