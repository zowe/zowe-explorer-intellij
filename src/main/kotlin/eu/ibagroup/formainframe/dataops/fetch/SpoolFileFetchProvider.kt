/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteSpoolFileAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.exceptions.responseMessageMap
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import org.zowe.kotlinsdk.JESApi

/**
 * Query with file to fetch children.
 * @author Valiantsin Krus
 */
data class JobQuery(val library: MFVirtualFile)

/**
 * Factory for registering SpoolFileFetchProvider in Intellij IoC container.
 * @author Valiantsin Krus
 */
class SpoolFileFetchProviderFactory : FileFetchProviderFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): FileFetchProvider<*, *, *> {
    return SpoolFileFetchProvider(dataOpsManager)
  }
}

private val log = log<SpoolFileFetchProvider>()

/**
 * Fetch provider for requesting spool files list of the job from zosmf.
 * @author Valiantsin Krus
 */
class SpoolFileFetchProvider(dataOpsManager: DataOpsManager) :
  RemoteAttributedFileFetchBase<ConnectionConfig, JobQuery, RemoteSpoolFileAttributes, MFVirtualFile>(dataOpsManager) {

  private val remoteJobAttributesService by lazy {
    dataOpsManager.getAttributesService<RemoteJobAttributes, MFVirtualFile>()
  }


  override val requestClass = JobQuery::class.java

  override val vFileClass = MFVirtualFile::class.java

  override val responseClass = RemoteSpoolFileAttributes::class.java

  /**
   * Fetches spool files relying on information in query.
   * @see RemoteAttributedFileFetchBase.fetchResponse
   */
  override fun fetchResponse(
    query: RemoteQuery<ConnectionConfig, JobQuery, Unit>,
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
        val headMessage = responseMessageMap[response.message()] ?: "Cannot retrieve Job files list"
        exception = CallException(response, headMessage)
      }

      if (exception != null) {
        throw exception
      }

      return attributes ?: emptyList()
    } else throw IllegalArgumentException("Virtual file is not a Job")
  }

  /**
   * Clears attributes of unused job file.
   * @see RemoteAttributedFileFetchBase.cleanupUnusedFile
   */
  override fun cleanupUnusedFile(file: MFVirtualFile, query: RemoteQuery<ConnectionConfig, JobQuery, Unit>) {
    log.info("About to clean-up file=$file, query=$query")
    attributesService.clearAttributes(file)
    file.delete(this)
  }
}
