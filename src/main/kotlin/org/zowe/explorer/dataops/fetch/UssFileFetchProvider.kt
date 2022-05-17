/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.fetch

import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.SymlinkMode

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
