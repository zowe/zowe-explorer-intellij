package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI

data class LibraryQuery(val library: MFVirtualFile)

class MemberFileFetchProviderFactory : FileFetchProviderFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): FileFetchProvider<*, *, *> {
    return MemberFileFetchProvider(dataOpsManager)
  }
}

class MemberFileFetchProvider(private val dataOpsManager: DataOpsManager) :
  RemoteAttributedFileFetchBase<LibraryQuery, RemoteMemberAttributes, MFVirtualFile>(dataOpsManager) {

  private val remoteDatasetAttributesService by lazy {
    dataOpsManager.getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
  }

  override val requestClass = LibraryQuery::class.java

  override val vFileClass = MFVirtualFile::class.java

  override val responseClass = RemoteMemberAttributes::class.java

  override fun fetchResponse(
    query: RemoteQuery<LibraryQuery, Unit>,
    progressIndicator: ProgressIndicator
  ): Collection<RemoteMemberAttributes> {
    val libraryAttributes = runReadAction { remoteDatasetAttributesService.getAttributes(query.request.library) }
    return if (libraryAttributes != null) {
      var attributes: Collection<RemoteMemberAttributes>? = null
      var exception: Throwable? = null

      val response = api<DataAPI>(query.connectionConfig).listDatasetMembers(
        authorizationToken = query.connectionConfig.token,
        datasetName = libraryAttributes.name
      ).cancelByIndicator(progressIndicator).execute()

      if (response.isSuccessful) {
        attributes = response.body()?.items?.map { RemoteMemberAttributes(it, query.request.library) }
      } else {
        exception = CallException(response, "Cannot retrieve member list")
      }

      if (exception != null) {
        throw exception
      }

      attributes ?: emptyList()
    } else throw IllegalArgumentException("Virtual file is not a library")
  }

  override fun cleanupUnusedFile(file: MFVirtualFile, query: RemoteQuery<LibraryQuery, Unit>) {
    attributesService.clearAttributes(file)
    file.delete(this)
  }
}