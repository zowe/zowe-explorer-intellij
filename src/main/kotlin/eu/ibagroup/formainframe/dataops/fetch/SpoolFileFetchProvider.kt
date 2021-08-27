package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteSpoolFileAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.JESApi

data class JobQuery(val library: MFVirtualFile)

class SpoolFileFetchProviderFactory : FileFetchProviderFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): FileFetchProvider<*, *, *> {
    return SpoolFileFetchProvider(dataOpsManager)
  }
}

private val log = log<SpoolFileFetchProvider>()

class SpoolFileFetchProvider(dataOpsManager: DataOpsManager) :
  RemoteAttributedFileFetchBase<JobQuery, RemoteSpoolFileAttributes, MFVirtualFile>(dataOpsManager) {

  private val remoteJobAttributesService by lazy {
    dataOpsManager.getAttributesService<RemoteJobAttributes, MFVirtualFile>()
  }


  override val requestClass = JobQuery::class.java

  override val vFileClass = MFVirtualFile::class.java

  override val responseClass = RemoteSpoolFileAttributes::class.java

  override fun fetchResponse(
    query: RemoteQuery<JobQuery, Unit>,
    progressIndicator: ProgressIndicator
  ): Collection<RemoteSpoolFileAttributes> {
    val jobAttributes = remoteJobAttributesService.getAttributes(query.request.library)
    if (jobAttributes != null) {
      log.info("Fetching Job Lists for $query")
      var attributes: Collection<RemoteSpoolFileAttributes>? = null
      var exception: Throwable? = null

      val response = api<JESApi>(query.connectionConfig).getJobSpoolFiles(
        basicCredentials = query.connectionConfig.authToken,
        jobId = jobAttributes.jobInfo.jobId,
        jobName = jobAttributes.jobInfo.jobName
      ).cancelByIndicator(progressIndicator).execute()

      if (response.isSuccessful) {
        attributes = response.body()?.map { RemoteSpoolFileAttributes(it, query.request.library) }
        log.info("${query.request} returned ${attributes?.size ?: 0} entities")
        log.debug {
          attributes?.joinToString("\n") ?: ""
        }
      } else {
        exception = CallException(response, "Cannot retrieve Job files list")
      }

      if (exception != null) {
        throw exception
      }

      return attributes ?: emptyList()
    } else throw IllegalArgumentException("Virtual file is not a Job")
  }

  override fun cleanupUnusedFile(file: MFVirtualFile, query: RemoteQuery<JobQuery, Unit>) {
    log.info("About to clean-up file=$file, query=$query")
    attributesService.clearAttributes(file)
    file.delete(this)
  }
}
