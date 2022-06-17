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

abstract class RemoteBatchedFileFetchProviderBase<ResponseList : Any, ResponseItem : Any, Request : Any, Response : FileAttributes, File : VirtualFile>(
  dataOpsManager: DataOpsManager,
  val BATCH_SIZE: Int = 100
) : RemoteAttributedFileFetchBase<Request, Response, File>(dataOpsManager) {

  abstract val log: Logger

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

  abstract fun fetchBatch(
    query: RemoteQuery<Request, Unit>,
    progressIndicator: ProgressIndicator,
    start: String?
  ): retrofit2.Response<ResponseList>

  abstract fun convertResponseToBody(responseList: ResponseList?): BatchedBody<ResponseItem>

  abstract fun buildAttributes(query: RemoteQuery<Request, Unit>, batchedItem: BatchedItem<ResponseItem>): Response
}

data class BatchedBody<Original>(
  val items: List<BatchedItem<Original>>?,
  val totalRows: Int? = null
)

data class BatchedItem<Original>(
  val name: String,
  val original: Original
)
