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

package org.zowe.explorer.dataops.fetch

import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.exceptions.responseMessageMap
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.SymlinkMode

/**
 * Query with uss file to fetch children
 */
data class UssQuery(val path: String)

private const val UPPER_DIR_NAME = ".."

/**
 * Factory for registering UssFileFetchProvider in Intellij IoC container
 */
class UssFileFetchProviderFactory : FileFetchProviderFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): FileFetchProvider<*, *, *> {
    return UssFileFetchProvider(dataOpsManager)
  }
}

private val log = log<UssFileFetchProvider>()

/**
 * Fetch provider for requesting uss files list
 */
class UssFileFetchProvider(
  dataOpsManager: DataOpsManager
) : RemoteAttributedFileFetchBase<ConnectionConfig, UssQuery, RemoteUssAttributes, MFVirtualFile>(dataOpsManager) {

  override val requestClass = UssQuery::class.java

  override val vFileClass = MFVirtualFile::class.java

  /**
   * Fetches uss files relying on information in query
   * @see RemoteAttributedFileFetchBase.fetchResponse
   */
  override fun fetchResponse(
    query: RemoteQuery<ConnectionConfig, UssQuery, Unit>,
    progressIndicator: ProgressIndicator
  ): Collection<RemoteUssAttributes> {
    log.info("Fetching USS Lists for $query")
    var attributes: Collection<RemoteUssAttributes>? = null
    var exception: Throwable? = null

    val response = api<DataAPI>(query.connectionConfig)
      .listUssPath(
        authorizationToken = query.connectionConfig.authToken,
        path = query.request.path,
        depth = 0,
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
      val headMessage = responseMessageMap[response.message()] ?: "Cannot retrieve USS files list"
      exception = CallException(response, headMessage)
    }

    if (exception != null) {
      throw exception
    }

    return attributes ?: emptyList()
  }

  override val responseClass = RemoteUssAttributes::class.java

  /**
   * Clears attributes of unused uss file
   * @see RemoteAttributedFileFetchBase.cleanupUnusedFile
   */
  override fun cleanupUnusedFile(file: MFVirtualFile, query: RemoteQuery<ConnectionConfig, UssQuery, Unit>) {
    log.info("About to clean-up file=$file, query=$query")
    attributesService.clearAttributes(file)
    file.delete(this)
  }

}
