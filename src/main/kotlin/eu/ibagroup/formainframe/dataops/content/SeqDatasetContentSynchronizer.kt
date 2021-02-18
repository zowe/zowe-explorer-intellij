package eu.ibagroup.formainframe.dataops.content

import com.intellij.openapi.application.ApplicationManager
import eu.ibagroup.formainframe.common.appLevelPluginDisposable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.api.api
import eu.ibagroup.formainframe.dataops.api.enqueueSync
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.utils.findAnyNullable
import eu.ibagroup.formainframe.utils.mapNotNull
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import retrofit2.Call
import java.io.IOException

class SeqDatasetContentSynchronizer : RemoteAttributesContentSynchronizerBase<RemoteDatasetAttributes>(
  ApplicationManager.getApplication().messageBus, appLevelPluginDisposable
) {
  override val vFileClass = MFVirtualFile::class.java

  override val attributesClass = RemoteDatasetAttributes::class.java

  override val storageNamePostfix = "seq_datasets"

  override fun fetchRemoteContentBytes(attributes: RemoteDatasetAttributes): ByteArray {
    var throwable: Throwable = IOException("Unknown error")
    return attributes.requesters.stream().mapNotNull {
      var content: ByteArray? = null
      try {
        val response = makeFetchCall(it.connectionConfig, attributes).execute()
        if (response.isSuccessful) {
          content = response.body()?.toByteArray()
        } else {
          throwable = IOException(response.code().toString())
        }
      } catch (t: Throwable) {
        throwable = t
      }
      content
    }.findAnyNullable() ?: throw throwable
  }

  private fun makeFetchCall(connectionConfig: ConnectionConfig, attributes: RemoteDatasetAttributes): Call<String> {
    val volser = attributes.volser
    return if (volser != null) {
      api<DataAPI>(connectionConfig).retrieveDatasetContent(
        authorizationToken = connectionConfig.token,
        datasetName = attributes.name,
        volser = volser
      )
    } else {
      api<DataAPI>(connectionConfig).retrieveDatasetContent(
        authorizationToken = connectionConfig.token,
        datasetName = attributes.name
      )
    }
  }

  private fun makeUploadCall(
    connectionConfig: ConnectionConfig,
    attributes: RemoteDatasetAttributes,
    content: ByteArray
  ): Call<Void> {
    val volser = attributes.volser
    return if (volser != null) {
      api<DataAPI>(connectionConfig).writeToDataset(
        authorizationToken = connectionConfig.token,
        datasetName = attributes.name,
        volser = volser,
        content = String(content)
      )
    } else {
      api<DataAPI>(connectionConfig).writeToDataset(
        authorizationToken = connectionConfig.token,
        datasetName = attributes.name,
        content = String(content)
      )
    }
  }

  override fun uploadNewContent(attributes: RemoteDatasetAttributes, newContentBytes: ByteArray) {
    var throwable: Throwable = IOException("Unknown error")
    var uploaded = false
    for (requester in attributes.requesters) {
      try {
        val response = makeUploadCall(requester.connectionConfig, attributes, newContentBytes).execute()
        if (response.isSuccessful) {
          uploaded = true
        } else {
          throwable = IOException(response.code().toString())
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
}