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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.BatchedRemoteQuery
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.exceptions.responseMessageMap
import eu.ibagroup.formainframe.utils.castOrNull

/**
 * Abstract class to fetch files in batches (of 100 units by default)
 * @param ResponseList class of response list that returns retrofit (e.g. DataSetsList, MembersList)
 * @param ResponseItem class of response item (e.g. Dataset, Member)
 * @param Response implementation class of file attributes.
 * @see FileAttributes
 * @param File implementation class of VirtualFile that plugin uses.
 * @author Valiantsin Krus
 */
abstract class RemoteBatchedFileFetchProviderBase<ResponseList : Any, ResponseItem : Any, Request : Any, Response : FileAttributes, File : VirtualFile>(
  dataOpsManager: DataOpsManager
) : RemoteAttributedFileFetchBase<ConnectionConfig, Request, Response, File>(dataOpsManager) {

  abstract val log: Logger

  /**
   * Fetches next batch by provided query (if query is BatchedRemoteQuery)
   * or all files (if query is UnitRemoteQueryImpl). It also updates parameters
   * of current fetching state inside batched query.
   * @param query query with all necessary information to send request to zosmf.
   * @param progressIndicator progress indicator to display progress of fetching items in UI.
   * @return collection of batch responses.
   */
  override fun fetchResponse(
    query: RemoteQuery<ConnectionConfig, Request, Unit>,
    progressIndicator: ProgressIndicator
  ): Collection<Response> {
    var fetchedItems: List<BatchedItem<ResponseItem>>? = emptyList()
    var failedResponse: retrofit2.Response<ResponseList>? = null
    progressIndicator.fraction = 0.0

    if (query is UnitRemoteQueryImpl) {
      val response = fetchBatch(query, progressIndicator, null)
      if (response.isSuccessful) {
        fetchedItems = convertResponseToBody(response.body()).items
      } else {
        failedResponse = response
      }
    } else {
      val unitQClassName = UnitRemoteQueryImpl::class.java.name
      val batchedQClassName = BatchedRemoteQuery::class.java
      val qClassName = query.javaClass.name
      val batchedQuery = query.castOrNull<BatchedRemoteQuery<Request>>()
        ?: throw IllegalArgumentException("Passed query should be $unitQClassName or $batchedQClassName and not $qClassName")

      if (batchedQuery.fetchNeeded) {

        val response = fetchBatch(query, progressIndicator, batchedQuery.start)
        if (response.isSuccessful) {
          val newBatchList = convertResponseToBody(response.body())
          batchedQuery.totalRows = batchedQuery.totalRows ?: newBatchList.totalRows

          val newBatch = newBatchList.items?.toMutableList()
          if (batchedQuery.alreadyFetched != 0 && newBatch?.size != 0) {
            newBatch?.removeFirst()
          }

          fetchedItems = newBatch?.let {
            fetchedItems?.toMutableList()?.apply { addAll(newBatch) }
          } ?: newBatch

          if (fetchedItems?.size != 0) {
            batchedQuery.start = fetchedItems?.last()?.name
          }
          batchedQuery.alreadyFetched += fetchedItems?.size ?: 0

          log.info("${query.request} returned ${newBatch?.size ?: 0} entities")
        } else {
          failedResponse = response
        }
        batchedQuery.fetchNeeded = batchedQuery.totalRows?.equals(batchedQuery.alreadyFetched) == false
      }
    }

    val attributes = fetchedItems?.map { buildAttributes(query, it) }

    if (failedResponse != null) {
      val headMessage = responseMessageMap[failedResponse.message()] ?: "Cannot retrieve dataset list"
      throw CallException(failedResponse, headMessage)
    }

    return attributes ?: emptyList()
  }

  /**
   * Fetches 1 batch. Method that should be overridden in implementation
   * @param query query with all necessary information to send request to zosmf.
   * @param progressIndicator progress indicator to display progress of fetching items in UI.
   * @param start name of the file from which next batch started.
   * @return response with batch response list inside.
   */
  abstract fun fetchBatch(
    query: RemoteQuery<ConnectionConfig, Request, Unit>,
    progressIndicator: ProgressIndicator,
    start: String?
  ): retrofit2.Response<ResponseList>

  /**
   * Converts retrofits response to BatchedBody.
   * @see BatchedBody
   * @param responseList response from retrofit.
   * @return batched body for continuous processing.
   */
  abstract fun convertResponseToBody(responseList: ResponseList?): BatchedBody<ResponseItem>

  /**
   * Builds file attributes from batched item.
   * @see BatchedBody
   * @param query query with all necessary information to send request to zosmf.
   * @param batchedItem item that was created in converting process for BatchedBody.
   * @return created attributes.
   */
  abstract fun buildAttributes(
    query: RemoteQuery<ConnectionConfig, Request, Unit>,
    batchedItem: BatchedItem<ResponseItem>
  ): Response
}

/**
 * Wrapper for working with batched lists.
 * @param Original class of element of response list that would be placed in batched list (e.g. Dataset, Member).
 * @param items list of batched items which wraps the original list element.
 * @param totalRows total elements count (for example, total datasets count
 *                  on mainframe is 1000, size of batch is 100 than totalRows=1000).
 * @author Valiantsin Krus
 */
data class BatchedBody<Original>(
  val items: List<BatchedItem<Original>>?,
  val totalRows: Int? = null
)

/**
 * Wrapper for original element of response list. Used as an element of BatchedBody.
 * @param name name of the file stored inside batched item.
 * @param original original response item.
 * @author Valiantsin Krus
 */
data class BatchedItem<Original>(
  val name: String,
  val original: Original
)
