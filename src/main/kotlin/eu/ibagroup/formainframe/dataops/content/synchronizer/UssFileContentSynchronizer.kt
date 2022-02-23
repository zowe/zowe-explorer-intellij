package eu.ibagroup.formainframe.dataops.content.synchronizer

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.applyIfNotNull
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.findAnyNullable
import eu.ibagroup.formainframe.utils.mapNotNull
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import java.io.IOException

class UssFileContentSynchronizerFactory: ContentSynchronizerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): ContentSynchronizer {
    return UssFileContentSynchronizer(dataOpsManager)
  }
}

class UssFileContentSynchronizer(
  dataOpsManager: DataOpsManager
): RemoteAttributedContentSynchronizer<RemoteUssAttributes>(dataOpsManager) {

  override val vFileClass = MFVirtualFile::class.java

  override val attributesClass = RemoteUssAttributes::class.java

  override val entityName = "uss"

  override fun fetchRemoteContentBytes(
    attributes: RemoteUssAttributes,
    progressIndicator: ProgressIndicator?
  ): ByteArray {
    var throwable: Throwable = IOException("Unknown error")
    return attributes.requesters.stream().mapNotNull {
      var content: ByteArray? = null
      try {
        val connectionConfig = it.connectionConfig
        val xIBMDataType = updateDataTypeWithEncoding(connectionConfig, attributes.contentMode)
        val response = api<DataAPI>(connectionConfig).retrieveUssFileContent(
          authorizationToken = connectionConfig.authToken,
          filePath = attributes.path.substring(1),
          xIBMDataType = xIBMDataType
        ).applyIfNotNull(progressIndicator) { indicator ->
          cancelByIndicator(indicator)
        }.execute()
        if (response.isSuccessful) {
          content = response.body()?.removeLastNewLine()?.toByteArray()
        } else {
          throwable = CallException(response, "Cannot fetch data from ${attributes.path}")
        }
      } catch (t: Throwable) {
        throwable = t
      }
      content
    }.findAnyNullable() ?: throw throwable
  }

  override fun uploadNewContent(
    attributes: RemoteUssAttributes,
    newContentBytes: ByteArray,
    progressIndicator: ProgressIndicator?
  ) {
    var throwable: Throwable = IOException("Unknown error")
    var uploaded = false
    for (requester in attributes.requesters) {
      try {
        val connectionConfig = requester.connectionConfig
        val xIBMDataType = updateDataTypeWithEncoding(connectionConfig, attributes.contentMode)
        val response = api<DataAPI>(connectionConfig).writeToUssFile(
          authorizationToken = connectionConfig.authToken,
          filePath = attributes.path.substring(1),
          body = String(newContentBytes).addNewLine(),
          xIBMDataType = xIBMDataType
        ).applyIfNotNull(progressIndicator) { indicator ->
          cancelByIndicator(indicator)
        }.execute()
        if (response.isSuccessful) {
          uploaded = true
        } else {
          throwable = CallException(response, "Cannot upload data to ${attributes.path}")
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
