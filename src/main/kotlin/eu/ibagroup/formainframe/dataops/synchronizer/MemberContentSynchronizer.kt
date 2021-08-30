package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.MaskedRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.Requester
import eu.ibagroup.formainframe.utils.applyIfNotNull
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.Member
import retrofit2.Response

class MemberContentSynchronizerFactory : ContentSynchronizerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): ContentSynchronizer {
    return MemberContentSynchronizer(dataOpsManager)
  }
}

private val log = log<MemberContentSynchronizer>()

class MemberContentSynchronizer(
  dataOpsManager: DataOpsManager
) : DependentFileContentSynchronizer<MFVirtualFile, Member, MaskedRequester, RemoteMemberAttributes, RemoteDatasetAttributes>(dataOpsManager, log<MemberContentSynchronizer>()) {

  override val vFileClass = MFVirtualFile::class.java

  override val attributesClass = RemoteMemberAttributes::class.java

  override val storageNamePostfix = "members"

  override val parentFileType = "Library"

  override val parentAttributesClass = RemoteDatasetAttributes::class.java

  override fun executeGetContentRequest(
    attributes: RemoteMemberAttributes,
    parentAttributes: RemoteDatasetAttributes,
    progressIndicator: ProgressIndicator?,
    requester: Requester
  ): Response<String> {
    return api<DataAPI>(requester.connectionConfig).retrieveMemberContent(
      authorizationToken = requester.connectionConfig.authToken,
      datasetName = parentAttributes.name,
      memberName = attributes.name,
      xIBMDataType = attributes.contentMode
    ).applyIfNotNull(progressIndicator) { indicator ->
      cancelByIndicator(indicator)
    }.execute()
  }

  override fun executePutContentRequest(
    attributes: RemoteMemberAttributes,
    parentAttributes: RemoteDatasetAttributes,
    requester: Requester,
    newContentBytes: ByteArray
  ): Response<Void> {
    return api<DataAPI>(requester.connectionConfig).writeToDatasetMember(
      authorizationToken = requester.connectionConfig.authToken,
      datasetName = parentAttributes.name,
      memberName = attributes.name,
      content = String(newContentBytes).addNewLine(),
      xIBMDataType = attributes.contentMode
    ).execute()
  }

}
