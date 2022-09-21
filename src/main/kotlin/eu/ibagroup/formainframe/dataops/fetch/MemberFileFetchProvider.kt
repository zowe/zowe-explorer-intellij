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
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.Member
import eu.ibagroup.r2z.MembersList
import eu.ibagroup.r2z.XIBMAttr
import retrofit2.Response

// TODO: doc
data class LibraryQuery(val library: MFVirtualFile)

class MemberFileFetchProviderFactory : FileFetchProviderFactory {
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

  // TODO: doc
  override fun cleanupUnusedFile(file: MFVirtualFile, query: RemoteQuery<LibraryQuery, Unit>) {
    log.info("About to clean-up file=$file, query=$query")
    attributesService.clearAttributes(file)
    file.delete(this)
  }

  // TODO: doc
  override fun fetchResponse(
    query: RemoteQuery<LibraryQuery, Unit>,
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
    query: RemoteQuery<LibraryQuery, Unit>,
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
    query: RemoteQuery<LibraryQuery, Unit>,
    batchedItem: BatchedItem<Member>
  ): RemoteMemberAttributes {
    return RemoteMemberAttributes(batchedItem.original, query.request.library)
  }
}
