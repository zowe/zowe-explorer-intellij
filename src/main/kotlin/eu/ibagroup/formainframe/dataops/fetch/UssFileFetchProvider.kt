package eu.ibagroup.formainframe.dataops.fetch

import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.config.connect.username
import eu.ibagroup.formainframe.dataops.api.api
import eu.ibagroup.formainframe.dataops.api.enqueueSync
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.SymlinkMode
import java.io.IOException

data class UssQuery(val path: String)

class UssFileFetchProviderFactory : FileFetchProviderFactory {
  override fun buildProvider(explorer: Explorer): FileFetchProvider<*, *, *> {
    return UssFileFetchProvider(explorer)
  }

}

class UssFileFetchProvider(
  explorer: Explorer
) : RemoteAttributedFileFetchBase<UssQuery, RemoteUssAttributes, MFVirtualFile>(explorer) {

  override val requestClass = UssQuery::class.java

  override val vFileClass = MFVirtualFile::class.java

  override fun makeFetchTaskTitle(query: RemoteQuery<UssQuery>): String {
    return "Fetching USS listings for ${query.request.path}"
  }

  override fun makeSecondaryTitle(query: RemoteQuery<UssQuery>): String {
    return ""
  }

  override fun fetchResponse(query: RemoteQuery<UssQuery>): Collection<RemoteUssAttributes> {
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
              user = username(query.connectionConfig)
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

  override fun cleanupUnusedFile(file: MFVirtualFile, query: RemoteQuery<UssQuery>) {
    attributesService.clearAttributes(file)
    file.delete(this)
  }

}