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

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.MaskedRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.utils.asMutableList
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.utils.nullIfBlank
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.DataSetsList
import eu.ibagroup.r2z.Dataset
import eu.ibagroup.r2z.XIBMAttr
import retrofit2.Response

// TODO: doc
class DatasetFileFetchProviderFactory : FileFetchProviderFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): FileFetchProvider<*, *, *> {
    return DatasetFileFetchProvider(dataOpsManager)
  }
}

private val logger = log<DatasetFileFetchProvider>()

/**
 * Implementation of batched provider for fetching datasets.
 * @see RemoteBatchedFileFetchProviderBase
 * @author Valiantsin Krus
 */
class DatasetFileFetchProvider(dataOpsManager: DataOpsManager) :
  RemoteBatchedFileFetchProviderBase<DataSetsList, Dataset, DSMask, RemoteDatasetAttributes, MFVirtualFile>(
    dataOpsManager
  ) {

  override val requestClass = DSMask::class.java

  override val vFileClass = MFVirtualFile::class.java

  override val responseClass = RemoteDatasetAttributes::class.java

  private var configService = service<ConfigService>()

  // TODO: doc
  override fun fetchResponse(
    query: RemoteQuery<DSMask, Unit>,
    progressIndicator: ProgressIndicator
  ): Collection<RemoteDatasetAttributes> {
    log.info("Fetching DS Lists for $query")
    return super.fetchResponse(query, progressIndicator)
  }


  // TODO: doc
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

  /**
   * Fetches 1 batch of datasets.
   * @see RemoteBatchedFileFetchProviderBase.fetchBatch
   */
  override fun fetchBatch(
    query: RemoteQuery<DSMask, Unit>,
    progressIndicator: ProgressIndicator,
    start: String?
  ): Response<DataSetsList> {
    val batchSize = if (start != null) configService.batchSize + 1 else configService.batchSize
    return api<DataAPI>(query.connectionConfig).listDataSets(
      authorizationToken = query.connectionConfig.authToken,
      dsLevel = query.request.mask,
      volser = query.request.volser.nullIfBlank(),
      xIBMAttr = XIBMAttr(isTotal = true),
      xIBMMaxItems = if (query is UnitRemoteQueryImpl) 0 else batchSize,
      start = start
    ).cancelByIndicator(progressIndicator).execute()
  }

  /**
   * Converts datasets response list to BatchedBody.
   * @see RemoteBatchedFileFetchProviderBase.convertResponseToBody
   */
  override fun convertResponseToBody(responseList: DataSetsList?): BatchedBody<Dataset> {
    return BatchedBody(responseList?.items?.map { BatchedItem(it.name, it) }, responseList?.totalRows)
  }

  /**
   * Builds RemoteDatasetAttributes from dataset batched item.
   * @see RemoteBatchedFileFetchProviderBase.buildAttributes
   */
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
