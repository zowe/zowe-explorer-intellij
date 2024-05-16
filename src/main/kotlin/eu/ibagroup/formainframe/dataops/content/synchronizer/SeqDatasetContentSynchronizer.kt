/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.content.synchronizer

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.api.apiWithBytesConverter
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.applyIfNotNull
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.findAnyNullable
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.utils.mapNotNull
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import okhttp3.ResponseBody
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.DatasetOrganization
import org.zowe.kotlinsdk.XIBMDataType
import retrofit2.Call
import java.io.IOException

class SeqDatasetContentSynchronizerFactory : ContentSynchronizerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): ContentSynchronizer {
    return SeqDatasetContentSynchronizer(dataOpsManager)
  }
}

private val log = log<SeqDatasetContentSynchronizer>()

/** Content synchronizer class for sequential datasets */
class SeqDatasetContentSynchronizer(
  dataOpsManager: DataOpsManager
) : RemoteAttributedContentSynchronizer<RemoteDatasetAttributes>(dataOpsManager) {
  override val vFileClass = MFVirtualFile::class.java

  override val attributesClass = RemoteDatasetAttributes::class.java

  override val entityName = "seq_datasets"

  /**
   * Fetch remote content bytes for the sequential dataset
   * @param attributes the attributes of the dataset to get requesters, the name of the dataset and the content mode
   * @param progressIndicator a progress indicator for the operation
   * @return content bytes after the operation is completed
   */
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

          content = if (attributes.contentMode.type == XIBMDataType.Type.BINARY) {
            response.body()?.bytes()
          } else {
            response.body()?.string()?.removeLastNewLine()?.toByteArray()
          }

        } else {
          throwable = CallException(response, "Cannot fetch data from ${attributes.name}")
        }
      } catch (t: Throwable) {
        throwable = t
      }
      content
    }.findAnyNullable() ?: throw throwable
  }

  /**
   * Make the fetch call to retrieve the dataset content
   * @param connectionConfig the connection config to make the call with
   * @param attributes the dataset attributes to get VOLSER, name and content mode
   * @return the call instance to track the result
   */
  private fun makeFetchCall(
    connectionConfig: ConnectionConfig,
    attributes: RemoteDatasetAttributes
  ): Call<ResponseBody> {
    val volser = attributes.volser
    val xIBMDataType = attributes.contentMode
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

  /**
   * Make the upload call to upload the dataset content to the mainframe
   * @param connectionConfig the connection config to make the call with
   * @param attributes the dataset attributes to get VOLSER, name and content mode
   * @param content the dataset content bytes to upload to the mainframe
   * @return the call instance to track the result
   */
  private fun makeUploadCall(
    connectionConfig: ConnectionConfig,
    attributes: RemoteDatasetAttributes,
    content: ByteArray
  ): Call<Void> {
    val volser = attributes.volser
    val xIBMDataType = attributes.contentMode
    return if (volser != null) {
      val newContent = if (xIBMDataType.type === XIBMDataType.Type.BINARY) content else content.addNewLine()
      apiWithBytesConverter<DataAPI>(connectionConfig).writeToDataset(
        authorizationToken = connectionConfig.authToken,
        datasetName = attributes.name,
        volser = volser,
        content = newContent,
        xIBMDataType = xIBMDataType
      )
    } else {
      apiWithBytesConverter<DataAPI>(connectionConfig).writeToDataset(
        authorizationToken = connectionConfig.authToken,
        datasetName = attributes.name,
        content = content,
        xIBMDataType = xIBMDataType
      )
    }
  }

  /**
   * Upload new content bytes of the dataset to the mainframe
   * @param attributes the attributes of the dataset to get requesters and the name of the dataset
   * @param newContentBytes the new content bytes to upload
   * @param progressIndicator a progress indicator for the operation
   */
  override fun uploadNewContent(
    attributes: RemoteDatasetAttributes,
    newContentBytes: ByteArray,
    progressIndicator: ProgressIndicator?
  ) {
    log.info("Upload remote content for $attributes")
    var throwable: Throwable = IOException("Unknown error")
    var uploaded = false
    for (requester in attributes.requesters) {
      try {
        log.info("Trying to execute a call using $requester")
        val response = makeUploadCall(requester.connectionConfig, attributes, newContentBytes)
          .applyIfNotNull(progressIndicator) { indicator ->
            cancelByIndicator(indicator)
          }.execute()
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

  /**
   * Check if the content synchronizer accepts the provided sequential dataset virtual file
   * @param file the file to check
   * @return true if:
   * 1. The dataset is not migrated
   * 2. The dataset organization parameter is not VS (it is not a VSAM or VSAM-related file)
   * 3. It is not an ALIAS
   */
  override fun accepts(file: VirtualFile): Boolean {
    val isOurVFile = super.accepts(file)
    return if (isOurVFile) {
      val dsAttributes = dataOpsManager.tryToGetAttributes(file)?.castOrNull<RemoteDatasetAttributes>()
      return dsAttributes != null
          && !dsAttributes.isMigrated
          && dsAttributes.datasetInfo.datasetOrganization != DatasetOrganization.VS
          && dsAttributes.datasetInfo.volumeSerial != "*ALIAS"
    } else {
      false
    }
  }
}
