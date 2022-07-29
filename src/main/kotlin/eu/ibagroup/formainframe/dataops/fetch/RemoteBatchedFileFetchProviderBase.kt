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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException

// TODO: redundant. rework.
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
  dataOpsManager: DataOpsManager,
  val BATCH_SIZE: Int = 100
) : RemoteAttributedFileFetchBase<Request, Response, File>(dataOpsManager) {

  abstract val log: Logger

  /**
   * Fetches all batches.
   * @param query query with all necessary information to send request to zosmf.
   * @param progressIndicator progress indicator to display progress of fetching items in UI.
   * @return collection of batch responses.
   */
  override fun fetchResponse(
    query: RemoteQuery<Request, Unit>,
    progressIndicator: ProgressIndicator
  ): Collection<Response> {
    val attributes: Collection<Response>?
    var fetchedItems: List<BatchedItem<ResponseItem>>? = null
    val totalRows: Int? = null
    var start: String? = null
    var fetchNeeded = true
    var failedResponse: retrofit2.Response<ResponseList>? = null

    while (fetchNeeded) {

      val response = fetchBatch(query, progressIndicator, start)
      var newBatchSize: Int?
      if (response.isSuccessful) {
        val newBatchList = convertResponseToBody(response.body())
        val newBatch = newBatchList.items?.toMutableList()
        if (fetchedItems != null && newBatch?.size != 0) {
          newBatch?.removeFirst()
        }

        fetchedItems = newBatch?.let {
          fetchedItems?.toMutableList()?.apply { addAll(newBatch) }
        } ?: newBatch

        if (fetchedItems?.size != 0) {
          start = fetchedItems?.last()?.name
        }
        newBatchSize = newBatch?.size

        log.info("${query.request} returned ${newBatch?.size ?: 0} entities")
      } else {
        failedResponse = response
        break
      }
      fetchNeeded = totalRows?.let { newBatchSize?.equals(totalRows - 1) } == false
    }

    attributes = fetchedItems?.map { buildAttributes(query, it) }

    if (failedResponse != null) {
      throw CallException(failedResponse, "Cannot retrieve dataset list")
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
    query: RemoteQuery<Request, Unit>,
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
  abstract fun buildAttributes(query: RemoteQuery<Request, Unit>, batchedItem: BatchedItem<ResponseItem>): Response
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
