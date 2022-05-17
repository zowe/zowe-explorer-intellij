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

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.applyIfNotNull
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.XIBMDataType
import org.zowe.kotlinsdk.annotations.ZVersion
import java.io.IOException

class MemberContentSynchronizerFactory : ContentSynchronizerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): ContentSynchronizer {
    return MemberContentSynchronizer(dataOpsManager)
  }
}

private val log = log<MemberContentSynchronizer>()

class MemberContentSynchronizer(
  dataOpsManager: DataOpsManager
) : RemoteAttributesContentSynchronizerBase<RemoteMemberAttributes>(dataOpsManager) {

  override val vFileClass = MFVirtualFile::class.java

  override val attributesClass = RemoteMemberAttributes::class.java

  private val datasetAttributesService = dataOpsManager
    .getAttributesService(RemoteDatasetAttributes::class.java, vFileClass)

  override val storageNamePostfix = "members"

  override fun fetchRemoteContentBytes(
    attributes: RemoteMemberAttributes,
    progressIndicator: ProgressIndicator?
  ): ByteArray {
    log.info("Fetch remote content for $attributes")
    val parentLib = attributes.parentFile
    val libAttributes = datasetAttributesService.getAttributes(parentLib)
      ?: throw IOException("Cannot find parent library attributes for library ${parentLib.path}")
    log.info("Lib attributes are $libAttributes")
    var throwable = Throwable("Unknown")
    var content: ByteArray? = null
    for (requester in libAttributes.requesters) {
      try {
        log.info("Trying to execute a call using $requester")
        val connectionConfig = requester.connectionConfig
        val xIBMDataType = updateDataTypeWithEncoding(connectionConfig, attributes.contentMode)
        val response = api<DataAPI>(connectionConfig).retrieveMemberContent(
          authorizationToken = connectionConfig.authToken,
          datasetName = libAttributes.name,
          memberName = attributes.name,
          xIBMDataType = xIBMDataType
        ).applyIfNotNull(progressIndicator) { indicator ->
          cancelByIndicator(indicator)
        }.execute()
        if (response.isSuccessful) {
          log.info("Content has been fetched successfully")
          content = response.body()?.removeLastNewLine()?.toByteArray()
          break
        } else {
          throwable = CallException(response, "Cannot fetch data from ${libAttributes.name}(${attributes.name})")
        }
      } catch (t: Throwable) {
        throwable = t
      }
    }
    return content ?: throw throwable
  }

  override fun uploadNewContent(attributes: RemoteMemberAttributes, newContentBytes: ByteArray) {
    log.info("Upload remote content for $attributes")
    val parentLib = attributes.parentFile
    val libAttributes = datasetAttributesService.getAttributes(parentLib)
      ?: throw IOException("Cannot find parent library attributes for library ${parentLib.path}")
    log.info("Lib attributes are $libAttributes")
    var throwable: Throwable? = null
    for (requester in libAttributes.requesters) {
      try {
        log.info("Trying to execute a call using $requester")
        val connectionConfig = requester.connectionConfig
        val xIBMDataType = updateDataTypeWithEncoding(connectionConfig, attributes.contentMode)
        val response = api<DataAPI>(connectionConfig).writeToDatasetMember(
          authorizationToken = connectionConfig.authToken,
          datasetName = libAttributes.name,
          memberName = attributes.name,
          content = String(newContentBytes).addNewLine(),
          xIBMDataType = xIBMDataType
        ).execute()
        if (response.isSuccessful) {
          log.info("Content has been uploaded successfully")
          throwable = null
          break
        } else {
          throwable = CallException(response, "Cannot upload data to ${libAttributes.name}(${attributes.name})")
        }
      } catch (t: Throwable) {
        throwable = t
      }
    }
    if (throwable != null) {
      throw throwable
    }
  }
}
