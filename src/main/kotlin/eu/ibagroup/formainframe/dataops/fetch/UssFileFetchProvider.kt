package eu.ibagroup.formainframe.dataops.fetch

import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.api.enqueueSync
import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.SymlinkMode
import java.io.IOException

data class UssQuery(val path: String)

class UssFileFetchProviderFactory : FileFetchProviderFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): FileFetchProvider<*, *, *> {
    return UssFileFetchProvider(dataOpsManager)
  }
}

class UssFileFetchProvider(
  dataOpsManager: DataOpsManager
) : RemoteAttributedFileFetchBase<UssQuery, RemoteUssAttributes, MFVirtualFile>(dataOpsManager) {

  override val requestClass = UssQuery::class.java

  override val vFileClass = MFVirtualFile::class.java

  override fun fetchResponse(query: RemoteQuery<UssQuery, Unit>): Collection<RemoteUssAttributes> {
    var attributes: Collection<RemoteUssAttributes>? = null
    var exception: Throwable = IOException("Cannot fetch ${query.request.path}")
    api<DataAPI>(query.connectionConfig).listUssPath(
      authorizationToken = query.connectionConfig.token,
      path = query.request.path,
      depth = 1,
      followSymlinks = SymlinkMode.REPORT
    ).enqueueSync {
      onResponse { _, response ->
        if (response.isSuccessful) {
          attributes = response.body()?.items?.map {
            RemoteUssAttributes(
              rootPath = query.request.path,
              ussFile = it,
              url = query.urlConnection.url,
              connectionConfig = query.connectionConfig
            )
          }
        } else {
          exception = IOException("${response.code()} " + (response.errorBody()?.string() ?: ""))
        }
      }
      onException { _, t -> exception = t }
    }
    return attributes ?: throw exception
  }

  override val responseClass = RemoteUssAttributes::class.java

  override fun cleanupUnusedFile(file: MFVirtualFile, query: RemoteQuery<UssQuery, Unit>) {
    attributesService.clearAttributes(file)
    file.delete(this)
  }

}