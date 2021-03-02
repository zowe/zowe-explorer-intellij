package eu.ibagroup.formainframe.dataops.synchronizer

import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.utils.findAnyNullable
import eu.ibagroup.formainframe.utils.mapNotNull
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.XIBMDataType
import java.io.IOException

class UssFileContentSynchronizerFactory : ContentSynchronizerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): ContentSynchronizer {
    return UssFileContentSynchronizer(dataOpsManager)
  }
}

class UssFileContentSynchronizer(
  dataOpsManager: DataOpsManager
) : RemoteAttributesContentSynchronizerBase<RemoteUssAttributes>(dataOpsManager) {

  override val vFileClass = MFVirtualFile::class.java

  override val attributesClass = RemoteUssAttributes::class.java

  override val storageNamePostfix = "uss"

  override fun fetchRemoteContentBytes(attributes: RemoteUssAttributes): ByteArray {
    var throwable: Throwable = IOException("Unknown error")
    return attributes.requesters.stream().mapNotNull {
      var content: ByteArray? = null
      try {
        val response = api<DataAPI>(it.connectionConfig).retrieveUssFileContent(
          authorizationToken = it.connectionConfig.token,
          filePath = attributes.path.substring(1),
          xIBMDataType = XIBMDataType.BINARY
        ).execute()
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

  override fun uploadNewContent(attributes: RemoteUssAttributes, newContentBytes: ByteArray) {
    var throwable: Throwable = IOException("Unknown error")
    var uploaded = false
    for (requester in attributes.requesters) {
      try {
        val response = api<DataAPI>(requester.connectionConfig).writeToUssFile(
          authorizationToken = requester.connectionConfig.token,
          filePath = attributes.path.substring(1),
          body = String(newContentBytes),
          xIBMDataType = XIBMDataType.BINARY
        ).execute()
        if (response.isSuccessful) {
          uploaded = true
        } else {
          throwable = IOException(response.code().toString())
        }
        if (uploaded) {
          break
        }
      } catch (t: Throwable) {
        throwable = t
      }
    }
    if (!uploaded) {
      throw throwable
    }
  }
}