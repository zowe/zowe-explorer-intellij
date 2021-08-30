package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.SymlinkMode

data class UssQuery(val path: String)

private const val UPPER_DIR_NAME = ".."

class UssFileFetchProviderFactory : FileFetchProviderFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): FileFetchProvider<*, *, *> {
    return UssFileFetchProvider(dataOpsManager)
  }
}

private val log = log<UssFileFetchProvider>()

class UssFileFetchProvider(
  dataOpsManager: DataOpsManager
) : RemoteAttributedFileFetchBase<UssQuery, RemoteUssAttributes, MFVirtualFile>(dataOpsManager) {

  override val requestClass = UssQuery::class.java

  override val vFileClass = MFVirtualFile::class.java

  override fun fetchResponse(
    query: RemoteQuery<UssQuery, Unit>,
    progressIndicator: ProgressIndicator
  ): Collection<RemoteUssAttributes> {
    log.info("Fetching USS Lists for $query")
    var attributes: Collection<RemoteUssAttributes>? = null
    var exception: Throwable? = null

    val response = api<DataAPI>(query.connectionConfig)
      .listUssPath(
        authorizationToken = query.connectionConfig.authToken,
        path = query.request.path,
        depth = 1,
        followSymlinks = SymlinkMode.REPORT
      ).cancelByIndicator(progressIndicator)
      .execute()

    if (response.isSuccessful) {
      attributes = response.body()?.items?.filter {
        it.name != UPPER_DIR_NAME
      }?.map {
        RemoteUssAttributes(
          rootPath = query.request.path,
          ussFile = it,
          url = query.connectionConfig.url,
          connectionConfig = query.connectionConfig
        )
      }
      log.info("${query.request} returned ${attributes?.size ?: 0} entities")
      log.debug {
        attributes?.joinToString("\n") ?: ""
      }
    } else {
      exception = CallException(response, "Cannot retrieve USS files list")
    }

    if (exception != null) {
      throw exception
    }

    return attributes ?: emptyList()
  }

  override val responseClass = RemoteUssAttributes::class.java

  override fun cleanupUnusedFile(file: MFVirtualFile, query: RemoteQuery<UssQuery, Unit>) {
    log.info("About to clean-up file=$file, query=$query")
    attributesService.clearAttributes(file)
    file.delete(this)
  }

}