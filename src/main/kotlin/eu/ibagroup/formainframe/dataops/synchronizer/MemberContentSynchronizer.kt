package eu.ibagroup.formainframe.dataops.synchronizer

import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import java.io.IOException

class MemberContentSynchronizerFactory : ContentSynchronizerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): ContentSynchronizer {
    return MemberContentSynchronizer(dataOpsManager)
  }
}

class MemberContentSynchronizer(
  dataOpsManager: DataOpsManager
) : RemoteAttributesContentSynchronizerBase<RemoteMemberAttributes>(dataOpsManager) {

  override val vFileClass = MFVirtualFile::class.java

  override val attributesClass = RemoteMemberAttributes::class.java

  private val datasetAttributesService = dataOpsManager
    .getAttributesService(RemoteDatasetAttributes::class.java, vFileClass)

  override val storageNamePostfix = "members"

  override fun fetchRemoteContentBytes(attributes: RemoteMemberAttributes): ByteArray {
    val parentLib = attributes.libraryFile
    val libAttributes = datasetAttributesService.getAttributes(parentLib)
      ?: throw IOException("Cannot find parent library attributes for library ${parentLib.path}")
    var throwable = Throwable("Unknown")
    var content: ByteArray? = null
    for (requester in libAttributes.requesters) {
      try {
        val response = api<DataAPI>(requester.connectionConfig).retrieveMemberContent(
          authorizationToken = requester.connectionConfig.token,
          datasetName = libAttributes.name,
          memberName = attributes.name,
          xIBMDataType = attributes.contentMode
        ).execute()
        if (response.isSuccessful) {
          content = response.body()?.toByteArray()
          break
        } else {
          throwable = Throwable("Todo ${response.code()}")
        }
      } catch (t: Throwable) {
        throwable = t
      }
    }
    return content ?: throw throwable
  }

  override fun uploadNewContent(attributes: RemoteMemberAttributes, newContentBytes: ByteArray) {
    val parentLib = attributes.libraryFile
    val libAttributes = datasetAttributesService.getAttributes(parentLib)
      ?: throw IOException("Cannot find parent library attributes for library ${parentLib.path}")
    var throwable: Throwable? = null
    for (requester in libAttributes.requesters) {
      try {
        val response = api<DataAPI>(requester.connectionConfig).writeToDatasetMember(
          authorizationToken = requester.connectionConfig.token,
          datasetName = libAttributes.name,
          memberName = attributes.name,
          content = String(newContentBytes),
          xIBMDataType = attributes.contentMode
        ).execute()
        if (response.isSuccessful) {
          throwable = null
          break
        } else {
          throwable = Throwable("Todo ${response.code()}")
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