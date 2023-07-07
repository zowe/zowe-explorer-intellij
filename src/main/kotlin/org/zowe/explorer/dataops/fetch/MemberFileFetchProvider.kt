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

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.UnitRemoteQueryImpl
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.getAttributesService
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.Member
import org.zowe.kotlinsdk.MembersList
import org.zowe.kotlinsdk.XIBMAttr
import retrofit2.Response

/**
 * Data class which represents request,
 * contains info about object on mainframe
 */
data class LibraryQuery(val library: MFVirtualFile)

/**
 * Class which represents factory for member file fetch provider
 */
class MemberFileFetchProviderFactory : FileFetchProviderFactory {

  /**
   * Creates instance of file fetch provider
   */
  override fun buildComponent(dataOpsManager: DataOpsManager): FileFetchProvider<*, *, *> {
    return MemberFileFetchProvider(dataOpsManager)
  }
}

private val logger = log<MemberFileFetchProvider>()

/**
 * Implementation of batched provider for fetching members.
 * @see RemoteBatchedFileFetchProviderBase
 * @author Valiantsin Krus
 */
class MemberFileFetchProvider(private val dataOpsManager: DataOpsManager) :
  RemoteBatchedFileFetchProviderBase<MembersList, Member, LibraryQuery, RemoteMemberAttributes, MFVirtualFile>(
    dataOpsManager
  ) {

  private val remoteDatasetAttributesService by lazy {
    dataOpsManager.getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
  }

  override val requestClass = LibraryQuery::class.java

  override val vFileClass = MFVirtualFile::class.java

  override val responseClass = RemoteMemberAttributes::class.java

  override val log = logger

  private val configService = service<ConfigService>()

  /**
   * Clears or updates attributes of unused dataset member file if needed
   * @param file object which need to clear/update
   * @param query request which need to be performed
   */
  override fun cleanupUnusedFile(file: MFVirtualFile, query: RemoteQuery<ConnectionConfig, LibraryQuery, Unit>) {
    log.info("About to clean-up file=$file, query=$query")
    attributesService.clearAttributes(file)
    file.delete(this)
  }

  /**
   * Fetches response of member fetching request
   * @param query body of fetch request
   * @param progressIndicator indicator to reflect fetching process status
   */
  override fun fetchResponse(
    query: RemoteQuery<ConnectionConfig, LibraryQuery, Unit>,
    progressIndicator: ProgressIndicator
  ): Collection<RemoteMemberAttributes> {
    log.info("Fetching DS Lists for $query")
    return if (!query.request.library.isReadable) {
      emptyList()
    } else {
      super.fetchResponse(query, progressIndicator)
    }
  }

  /**
   * Fetches 1 batch of members.
   * @see RemoteBatchedFileFetchProviderBase.fetchBatch
   */
  override fun fetchBatch(
    query: RemoteQuery<ConnectionConfig, LibraryQuery, Unit>,
    progressIndicator: ProgressIndicator,
    start: String?
  ): Response<MembersList> {
    val libraryAttributes = remoteDatasetAttributesService.getAttributes(query.request.library)
    val batchSize = if (start != null) configService.batchSize + 1 else configService.batchSize
    return if (libraryAttributes !== null)
      api<DataAPI>(query.connectionConfig).listDatasetMembers(
        authorizationToken = query.connectionConfig.authToken,
        datasetName = libraryAttributes.name,
        xIBMAttr = XIBMAttr(isTotal = true),
        xIBMMaxItems = if (query is UnitRemoteQueryImpl) 0 else batchSize,
        start = start
      ).cancelByIndicator(progressIndicator).execute()
    else throw IllegalArgumentException("Virtual file is not a library")
  }

  /**
   * Converts members response list to BatchedBody.
   * @see RemoteBatchedFileFetchProviderBase.convertResponseToBody
   */
  override fun convertResponseToBody(responseList: MembersList?): BatchedBody<Member> {
    return BatchedBody(responseList?.items?.map { BatchedItem(it.name, it) }, responseList?.totalRows)
  }

  /**
   * Builds RemoteMemberAttributes from member batched item.
   * @see RemoteBatchedFileFetchProviderBase.buildAttributes
   */
  override fun buildAttributes(
    query: RemoteQuery<ConnectionConfig, LibraryQuery, Unit>,
    batchedItem: BatchedItem<Member>
  ): RemoteMemberAttributes {
    return RemoteMemberAttributes(batchedItem.original, query.request.library)
  }
}
