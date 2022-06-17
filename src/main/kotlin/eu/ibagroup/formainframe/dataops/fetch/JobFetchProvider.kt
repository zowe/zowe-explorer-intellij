/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.JobsRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.asMutableList
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.JESApi
import eu.ibagroup.r2z.JobStatus
import retrofit2.Response

class JobFileFetchProviderFactory : FileFetchProviderFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): FileFetchProvider<*, *, *> {
    return JobFetchProvider(dataOpsManager)
  }
}

private val log = log<JobFetchProvider>()

class JobFetchProvider(dataOpsManager: DataOpsManager) :
  RemoteAttributedFileFetchBase<JobsFilter, RemoteJobAttributes, MFVirtualFile>(dataOpsManager) {

  override val requestClass = JobsFilter::class.java

  override val vFileClass = MFVirtualFile::class.java

  override val responseClass = RemoteJobAttributes::class.java

  override fun fetchResponse(
    query: RemoteQuery<JobsFilter, Unit>,
    progressIndicator: ProgressIndicator
  ): Collection<RemoteJobAttributes> {
    log.info("Fetching Job Lists for $query")
    var attributes: Collection<RemoteJobAttributes>? = null
    var exception: Throwable? = null

    val response = if (query.request.jobId.isNotEmpty()) {
      api<JESApi>(query.connectionConfig).getFilteredJobs(
        basicCredentials = query.connectionConfig.authToken,
        jobId = query.request.jobId
      ).cancelByIndicator(progressIndicator).execute()
    } else {
      api<JESApi>(query.connectionConfig).getFilteredJobs(
        basicCredentials = query.connectionConfig.authToken,
        owner = query.request.owner,
        prefix = query.request.prefix,
        userCorrelator = query.request.userCorrelatorFilter
      ).cancelByIndicator(progressIndicator).execute()
    }

    if (response.isSuccessful) {
      attributes = response.body()?.map {
        RemoteJobAttributes(
          it,
          query.connectionConfig.url,
          JobsRequester(query.connectionConfig, query.request).asMutableList()
        )
      }
      log.info("${query.request} returned ${attributes?.size ?: 0} entities")
//      log.debug {
//        attributes?.joinToString("\n") ?: ""
//      }
    } else {
      exception = CallException(response, "Cannot retrieve Job files list")
    }

    if (exception != null) {
      throw exception
    }

    return attributes ?: emptyList()
  }

  override fun cleanupUnusedFile(file: MFVirtualFile, query: RemoteQuery<JobsFilter, Unit>) {
    val deletingFileAttributes = attributesService.getAttributes(file)
    log.info("Cleaning-up file attributes $deletingFileAttributes")
    if (deletingFileAttributes != null) {
      val needsDeletionFromFs = deletingFileAttributes.requesters.all {
        it.connectionConfig == query.connectionConfig
      }
      log.info("needsDeletionFromFs=$needsDeletionFromFs; $deletingFileAttributes")
      if (needsDeletionFromFs) {
        attributesService.clearAttributes(file)
        file.delete(this)
      } else {
        attributesService.updateAttributes(file) {
          requesters.removeAll {
            it.connectionConfig == query.connectionConfig
          }
        }
      }
    }
  }

}
