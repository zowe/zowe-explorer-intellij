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
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.*
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.DatasetOrganization
import org.zowe.kotlinsdk.XIBMDataType
import org.zowe.kotlinsdk.annotations.ZVersion
import retrofit2.Call
import java.io.IOException

class SeqDatasetContentSynchronizerFactory : ContentSynchronizerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): ContentSynchronizer {
    return SeqDatasetContentSynchronizer(dataOpsManager)
  }
}

private val log = log<SeqDatasetContentSynchronizer>()

class SeqDatasetContentSynchronizer(
  dataOpsManager: DataOpsManager
) : RemoteAttributesContentSynchronizerBase<RemoteDatasetAttributes>(dataOpsManager) {
  override val vFileClass = MFVirtualFile::class.java

  override val attributesClass = RemoteDatasetAttributes::class.java

  override val storageNamePostfix = "seq_datasets"

  override fun fetchRemoteContentBytes(
    attributes: RemoteDatasetAttributes,
    progressIndicator: ProgressIndicator?
  ): ByteArray {
    log.info("Fetch remote content for $attributes")
    var throwable: Throwable = IOException("Unknown error")
    return attributes.requesters.stream().mapNotNull {
      var content: ByteArray? = null
      try {
        log.info("Trying to execute a call using $it")
        val response = makeFetchCall(it.connectionConfig, attributes).apply {
          progressIndicator?.let { pi -> cancelByIndicator(pi) }
        }.applyIfNotNull(progressIndicator) { indicator ->
          cancelByIndicator(indicator)
        }.execute()
        if (response.isSuccessful) {
          log.info("Content has been fetched successfully")
          content = response.body()?.removeLastNewLine()?.toByteArray()
        } else {
          throwable = CallException(response, "Cannot fetch data from ${attributes.name}")
        }
      } catch (t: Throwable) {
        throwable = t
      }
      content
    }.findAnyNullable() ?: throw throwable
  }

  private fun makeFetchCall(connectionConfig: ConnectionConfig, attributes: RemoteDatasetAttributes): Call<String> {
    val volser = attributes.volser
    val xIBMDataType = updateDataTypeWithEncoding(connectionConfig, attributes.contentMode)
    return if (volser != null) {
      api<DataAPI>(connectionConfig).retrieveDatasetContent(
        authorizationToken = connectionConfig.authToken,
        datasetName = attributes.name,
        volser = volser,
        xIBMDataType = xIBMDataType
      )
    } else {
      api<DataAPI>(connectionConfig).retrieveDatasetContent(
        authorizationToken = connectionConfig.authToken,
        datasetName = attributes.name,
        xIBMDataType = xIBMDataType
      )
    }
  }

  private fun makeUploadCall(
    connectionConfig: ConnectionConfig,
    attributes: RemoteDatasetAttributes,
    content: ByteArray
  ): Call<Void> {
    val volser = attributes.volser
    val xIBMDataType = updateDataTypeWithEncoding(connectionConfig, attributes.contentMode)
    return if (volser != null) {
      api<DataAPI>(connectionConfig).writeToDataset(
        authorizationToken = connectionConfig.authToken,
        datasetName = attributes.name,
        volser = volser,
        content = String(content).addNewLine(),
        xIBMDataType = xIBMDataType
      )
    } else {
      api<DataAPI>(connectionConfig).writeToDataset(
        authorizationToken = connectionConfig.authToken,
        datasetName = attributes.name,
        content = String(content),
        xIBMDataType = xIBMDataType
      )
    }
  }

  override fun uploadNewContent(attributes: RemoteDatasetAttributes, newContentBytes: ByteArray) {
    log.info("Upload remote content for $attributes")
    var throwable: Throwable = IOException("Unknown error")
    var uploaded = false
    for (requester in attributes.requesters) {
      try {
        log.info("Trying to execute a call using $requester")
        val response = makeUploadCall(requester.connectionConfig, attributes, newContentBytes).execute()
        if (response.isSuccessful) {
          uploaded = true
        } else {
          throwable = CallException(response, "Cannot upload data to ${attributes.name}")
        }
      } catch (t: Throwable) {
        throwable = t
      }
      if (uploaded) {
        break
      }
    }
    if (!uploaded) {
      throw throwable
    }
  }

  override fun accepts(file: VirtualFile): Boolean {
    return super.accepts(file) &&
      dataOpsManager.tryToGetAttributes(file)?.castOrNull<RemoteDatasetAttributes>()?.let {
        !it.isMigrated && it.datasetInfo.datasetOrganization != DatasetOrganization.VS
      } == true
  }

}
