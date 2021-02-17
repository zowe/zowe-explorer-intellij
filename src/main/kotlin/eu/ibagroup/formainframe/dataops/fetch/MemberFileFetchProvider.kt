package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.application.runReadAction
import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.api.api
import eu.ibagroup.formainframe.dataops.api.enqueueSync
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.dataOpsManager
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import java.io.IOException

data class LibraryQuery(val library: MFVirtualFile)

class MemberFileFetchProvider :
  RemoteAttributedFileFetchBase<LibraryQuery, RemoteMemberAttributes, MFVirtualFile>() {

  private val remoteDatasetAttributesService by lazy {
    dataOpsManager.getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
  }

  override val requestClass = LibraryQuery::class.java

  override val vFileClass = MFVirtualFile::class.java

  override fun makeFetchTaskTitle(query: RemoteQuery<LibraryQuery>): String {
    return "Fetching members for ${query.request.library.name}"
  }

  override val responseClass = RemoteMemberAttributes::class.java

  override fun makeSecondaryTitle(query: RemoteQuery<LibraryQuery>): String {
    return ""
  }

  override fun fetchResponse(query: RemoteQuery<LibraryQuery>): Collection<RemoteMemberAttributes> {
    val libraryAttributes = runReadAction { remoteDatasetAttributesService.getAttributes(query.request.library) }
    return if (libraryAttributes != null) {
      var attributes: Collection<RemoteMemberAttributes>? = null
      var exception: Throwable = IOException("Cannot fetch members for ${libraryAttributes.name}")
      api<DataAPI>(query.connectionConfig).listDatasetMembers(
        authorizationToken = query.connectionConfig.token,
        datasetName = libraryAttributes.name
      ).enqueueSync {
        onResponse { _, response ->
          if (response.isSuccessful) {
            attributes = response.body()?.items?.map { RemoteMemberAttributes(it, query.request.library) }
          } else {
            exception = IOException("${response.code()} " + (response.errorBody()?.string() ?: ""))
          }
        }
        onException { _, t -> exception = t }
      }
      attributes ?: throw exception
    } else throw IllegalArgumentException("Virtual file is not a library")
  }

  override fun cleanupUnusedFile(file: MFVirtualFile, query: RemoteQuery<LibraryQuery>) {
    attributesService.clearAttributes(file)
    file.delete(this)
  }
}