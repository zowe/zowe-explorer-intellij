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
import eu.ibagroup.r2z.ExecData
import eu.ibagroup.r2z.JESApi
import eu.ibagroup.r2z.annotations.ZVersion
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Factory to register JobFetchProvider in Intellij IoC container.
 * @author Valiantsin Krus
 */
class JobFileFetchProviderFactory : FileFetchProviderFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): FileFetchProvider<*, *, *> {
    return JobFetchProvider(dataOpsManager)
  }
}

private val log = log<JobFetchProvider>()

/**
 * Provider for fetching list of jobs by the corresponding filter (e.g. owner, job name, job id).
 * @author Valiantsin Krus
 */
class JobFetchProvider(dataOpsManager: DataOpsManager) :
  RemoteAttributedFileFetchBase<JobsFilter, RemoteJobAttributes, MFVirtualFile>(dataOpsManager) {

  override val requestClass = JobsFilter::class.java

  override val vFileClass = MFVirtualFile::class.java

  override val responseClass = RemoteJobAttributes::class.java

  /**
   * Fetches jobs from zosmf. Creates and registers file attributes for them.
   * @see RemoteFileFetchProviderBase.fetchResponse
   */
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
        jobId = query.request.jobId,
        execData = ExecData.YES
      ).cancelByIndicator(progressIndicator).execute()
    } else {
      api<JESApi>(query.connectionConfig).getFilteredJobs(
        basicCredentials = query.connectionConfig.authToken,
        owner = query.request.owner,
        prefix = query.request.prefix,
        userCorrelator = query.request.userCorrelatorFilter,
        execData = ExecData.YES
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
      log.info("Getting job timestamps and return code for returned jobs list...")
      if(attributes!!.isNotEmpty()) {
        val firstJobInfo = attributes[0].jobInfo
        if(firstJobInfo.execStarted == null && firstJobInfo.execEnded == null && firstJobInfo.execSubmitted == null) {
          log.info("Try to get jobs timestamps and return code through log fetcher due to z/OS version < ${ZVersion.ZOS_2_4}")
          val updatedJobAttributes = mutableListOf<RemoteJobAttributes>()
          val jobFetchHelperList = CopyOnWriteArrayList<JobFetchHelper>()
          for (attr in attributes) {
            val jobFetchHelper = JobFetchHelper(query, attr)
            jobFetchHelperList.add(jobFetchHelper)
            jobFetchHelper.start()
          }
          while (jobFetchHelperList.size != 0) {
            for(jobFetchHelper in jobFetchHelperList) {
              if (!jobFetchHelper.isAlive) {
                updatedJobAttributes.add(jobFetchHelper.getUpdatedJobAttributes())
                jobFetchHelperList.remove(jobFetchHelper)
              }
            }
          }
          return updatedJobAttributes
        } else {
          log.info("jobs attributes already have timestamps and return code assigned")
        }
      } else {
        log.info("No jobs returned for query $query. Skipping")
      }
    } else {
      exception = CallException(response, "Cannot retrieve Job files list")
    }

    if (exception != null) {
      throw exception
    }

    return attributes ?: emptyList()
  }

  /**
   * Clears or update attributes of unused job file if needed.
   * @see RemoteFileFetchProviderBase.cleanupUnusedFile
   */
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
