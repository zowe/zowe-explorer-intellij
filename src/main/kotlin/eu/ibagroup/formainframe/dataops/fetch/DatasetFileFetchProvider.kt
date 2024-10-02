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

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.MaskedRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.exceptions.responseMessageMap
import eu.ibagroup.formainframe.explorer.EXPLORER_NOTIFICATION_GROUP_ID
import eu.ibagroup.formainframe.telemetry.NotificationCompatibleException
import eu.ibagroup.formainframe.utils.asMutableList
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.utils.nullIfBlank
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.DataSetsList
import org.zowe.kotlinsdk.Dataset
import org.zowe.kotlinsdk.XIBMAttr
import retrofit2.Response

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

  private var configService = ConfigService.getService()

  override fun fetchResponse(
    query: RemoteQuery<ConnectionConfig, DSMask, Unit>,
    progressIndicator: ProgressIndicator
  ): Collection<RemoteDatasetAttributes> {
    log.info("Fetching DS Lists for $query")
    return super.fetchResponse(query, progressIndicator)
  }


  /**
   * Clean up unused invalid files from the local virtual file system.
   * If the file does not belong to the connection config or if the VOLSER is different, it won't be deleted,
   * only invalid requesters will be deleted
   * @param file the file to remove
   * @param query the query to check the file attributes requesters
   */
  override fun cleanupUnusedFile(file: MFVirtualFile, query: RemoteQuery<ConnectionConfig, DSMask, Unit>) {
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
   * If there is a failure due to a custom migration volume, proceeds with fetching a batch
   * with the [XIBMAttr.Type.VOL] parameter to get at least basic attributes for the request
   * @see RemoteBatchedFileFetchProviderBase.fetchBatch
   */
  override fun fetchBatch(
    query: RemoteQuery<ConnectionConfig, DSMask, Unit>,
    progressIndicator: ProgressIndicator,
    start: String?
  ): Response<DataSetsList> {
    val batchSize = if (start != null) configService.batchSize + 1 else configService.batchSize

    var response = api<DataAPI>(query.connectionConfig).listDataSets(
      authorizationToken = query.connectionConfig.authToken,
      dsLevel = query.request.mask,
      volser = query.request.volser.nullIfBlank(),
      xIBMAttr = XIBMAttr(type = XIBMAttr.Type.BASE, isTotal = true),
      xIBMMaxItems = if (query is UnitRemoteQueryImpl) 0 else batchSize,
      start = start
    ).cancelByIndicator(progressIndicator).execute()

    // https://github.com/zowe/zowe-explorer-intellij/issues/129
    if (!response.isSuccessful && response.code() == 500) {
      val responseErrorBody = response.errorBody()
      val responseContentType = responseErrorBody?.contentType() ?: "application/json".toMediaType()
      val responseErrorBodyStr = responseErrorBody?.string() ?: ""
      response =
        if (
          responseErrorBodyStr.contains("ServletDispatcher failed - received TSO Prompt when expecting TsoServletResponse")
          && responseErrorBodyStr.contains("DMS2987")
        ) {
          val custMigrVolComputed = responseErrorBodyStr
            .split("DMS2987 DATA SET CATALOGED TO CA DISK PSEUDO-VOLUME ")
            .getOrElse(1) { "" }
            .split("\"")
            .getOrElse(0) { "" }

          val isCustMigrVolRecognized = MFVirtualFileSystem.belongsToCustMigrVols(custMigrVolComputed)
          val furtherProcessingMessage =
            if (isCustMigrVolRecognized)
              " Plug-in is capable of processing datasets on this volume and won't try to fetch attributes or content for such datasets."
            else
              " Plug-in does not recognize this custom migration volume. Please, provide us with the diagnostics message in the issue below."

          Notification(
            EXPLORER_NOTIFICATION_GROUP_ID,
            "Fetching datasets list error",
            "Failed to fetch attributes for datasets."
              + " The cause: there is a custom migration volume that z/OSMF does not recognize."
              + " The volume: $custMigrVolComputed."
              + furtherProcessingMessage
              + " Current workaround: try to use a mask that omits the datasets on the custom migration volumes."
              + " The issue to provide more diagnostics and get some understanding: https://github.com/zowe/zowe-explorer-intellij/issues/129."
              + "\nDetailed message from z/OSMF REST API:"
              + "\n"
              + responseErrorBodyStr,
            NotificationType.WARNING
          ).let {
            Notifications.Bus.notify(it)
          }

          api<DataAPI>(query.connectionConfig).listDataSets(
            authorizationToken = query.connectionConfig.authToken,
            dsLevel = query.request.mask,
            volser = query.request.volser.nullIfBlank(),
            xIBMAttr = XIBMAttr(type = XIBMAttr.Type.VOL, isTotal = true),
            xIBMMaxItems = if (query is UnitRemoteQueryImpl) 0 else batchSize,
            start = start
          ).cancelByIndicator(progressIndicator).execute()
        } else {
          Response.error(500, responseErrorBodyStr.toResponseBody(responseContentType))
        }
    }
    return response
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
    query: RemoteQuery<ConnectionConfig, DSMask, Unit>,
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

  override fun getSingleFetchedFile(
    query: RemoteQuery<ConnectionConfig, DSMask, Unit>,
    progressIndicator: ProgressIndicator
  ): MFVirtualFile {
    val response = api<DataAPI>(query.connectionConfig).listDataSets(
      authorizationToken = query.connectionConfig.authToken,
      dsLevel = query.request.mask,
      volser = query.request.volser.nullIfBlank(),
      xIBMMaxItems = 1,
      xIBMAttr = XIBMAttr(type = XIBMAttr.Type.BASE, isTotal = true)
    ).cancelByIndicator(progressIndicator).execute()
    val fetchedItems = if (response.isSuccessful) {
      convertResponseToBody(response.body()).items
    } else {
      val headMessage =
        responseMessageMap[response.message()] ?: "Cannot retrieve ${query.request.mask} dataset attributes"
      throw CallException(response, headMessage)
    }
    if (fetchedItems?.size != 1) {
      val title = "Incompatible list of datasets with attributes"
      val details =
        "Expected exactly 1 item in list returned for the request to fetch single item attributes.\n" +
          "The item: ${query.request.mask}, VOLSER: ${query.request.mask}.\n" +
          "Actual elements amount: ${fetchedItems?.size ?: 0}"
      throw NotificationCompatibleException(title, details)
    }
    val attributes = buildAttributes(query, fetchedItems[0])
    return convertResponseToFile(attributes)
      ?: throw NotificationCompatibleException(
        "Unable to find or create virtual file",
        "Unable to find or create virtual file for attributes of ${attributes.name}"
      )
  }

}
