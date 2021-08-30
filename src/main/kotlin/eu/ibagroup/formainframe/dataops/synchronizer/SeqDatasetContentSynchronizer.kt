package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.DatasetOrganization
import eu.ibagroup.r2z.XIBMDataType
import eu.ibagroup.r2z.annotations.ZVersion
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