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

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.attributes.MaskedRequester
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.utils.asMutableList
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.explorer.utils.nullIfBlank
import org.zowe.explorer.vfs.MFVirtualFile
import eu.ibagroup.r2z.*
import retrofit2.Response

class DatasetFileFetchProviderFactory : FileFetchProviderFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): FileFetchProvider<*, *, *> {
    return DatasetFileFetchProvider(dataOpsManager)
  }
}

private val logger = log<DatasetFileFetchProvider>()

class DatasetFileFetchProvider(dataOpsManager: DataOpsManager) :
  RemoteBatchedFileFetchProviderBase<DataSetsList, Dataset, DSMask, RemoteDatasetAttributes, MFVirtualFile>(
    dataOpsManager
  ) {

  override val requestClass = DSMask::class.java

  override val vFileClass = MFVirtualFile::class.java

  override fun fetchResponse(
    query: RemoteQuery<DSMask, Unit>,
    progressIndicator: ProgressIndicator
  ): Collection<RemoteDatasetAttributes> {
    log.info("Fetching DS Lists for $query")
    return super.fetchResponse(query, progressIndicator)
  }

  override val responseClass = RemoteDatasetAttributes::class.java

  override fun cleanupUnusedFile(file: MFVirtualFile, query: RemoteQuery<DSMask, Unit>) {
    val deletingFileAttributes = attributesService.getAttributes(file)
    log.info("Cleaning-up file attributes $deletingFileAttributes")
    if (deletingFileAttributes != null) {
      val needsDeletionFromFs = deletingFileAttributes.requesters.all {
        it.connectionConfig == query.connectionConfig && it.queryVolser == query.request.volser
      }
      log.info("needsDeletionFromFs=$needsDeletionFromFs; $deletingFileAttributes")
      if (needsDeletionFromFs) {
        attributesService.clearAttributes(file)
        file.delete(this)
      } else {
        attributesService.updateAttributes(file) {
          requesters.removeAll {
            it.connectionConfig == query.connectionConfig && it.queryVolser == query.request.volser
          }
        }
      }
    }
  }

  override val log = logger

  override fun fetchBatch(
    query: RemoteQuery<DSMask, Unit>,
    progressIndicator: ProgressIndicator,
    start: String?
  ): Response<DataSetsList> {
    return api<DataAPI>(query.connectionConfig).listDataSets(
      authorizationToken = query.connectionConfig.authToken,
      dsLevel = query.request.mask,
      volser = query.request.volser.nullIfBlank(),
      xIBMAttr = XIBMAttr(isTotal = true),
      xIBMMaxItems = BATCH_SIZE,
      start = start
    ).cancelByIndicator(progressIndicator).execute()
  }

  override fun convertResponseToBody(responseList: DataSetsList?): BatchedBody<Dataset> {
    return BatchedBody(responseList?.items?.map { BatchedItem(it.name, it) }, responseList?.totalRows)
  }

  override fun buildAttributes(
    query: RemoteQuery<DSMask, Unit>,
    batchedItem: BatchedItem<Dataset>
  ): RemoteDatasetAttributes {
    return RemoteDatasetAttributes(
      batchedItem.original,
      query.connectionConfig.url,
      MaskedRequester(
        query.connectionConfig,
        query.request
      ).asMutableList()
    )
  }

}
