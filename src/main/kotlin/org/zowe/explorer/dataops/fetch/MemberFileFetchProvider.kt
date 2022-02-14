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
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.getAttributesService
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.explorer.vfs.MFVirtualFile
import eu.ibagroup.r2z.*
import retrofit2.Response

data class LibraryQuery(val library: MFVirtualFile)

class MemberFileFetchProviderFactory : FileFetchProviderFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): FileFetchProvider<*, *, *> {
    return MemberFileFetchProvider(dataOpsManager)
  }
}

private val logger = log<MemberFileFetchProvider>()

class MemberFileFetchProvider(private val dataOpsManager: DataOpsManager) :
  RemoteBatchedFileFetchProviderBase<MembersList, Member, LibraryQuery, RemoteMemberAttributes, MFVirtualFile>(dataOpsManager) {

  private val remoteDatasetAttributesService by lazy {
    dataOpsManager.getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
  }

  override val requestClass = LibraryQuery::class.java

  override val vFileClass = MFVirtualFile::class.java

  override val responseClass = RemoteMemberAttributes::class.java

  override val log = logger

  override fun cleanupUnusedFile(file: MFVirtualFile, query: RemoteQuery<LibraryQuery, Unit>) {
    log.info("About to clean-up file=$file, query=$query")
    attributesService.clearAttributes(file)
    file.delete(this)
  }

  override fun fetchResponse(
    query: RemoteQuery<LibraryQuery, Unit>,
    progressIndicator: ProgressIndicator
  ): Collection<RemoteMemberAttributes> {
    log.info("Fetching DS Lists for $query")
    return super.fetchResponse(query, progressIndicator)
  }

  override fun fetchBatch(
    query: RemoteQuery<LibraryQuery, Unit>,
    progressIndicator: ProgressIndicator,
    start: String?
  ): Response<MembersList> {
    val libraryAttributes = remoteDatasetAttributesService.getAttributes(query.request.library)
    return if (libraryAttributes !== null)
      api<DataAPI>(query.connectionConfig).listDatasetMembers(
        authorizationToken = query.connectionConfig.authToken,
        datasetName = libraryAttributes.name,
        xIBMAttr = XIBMAttr(isTotal = true),
        xIBMMaxItems = BATCH_SIZE,
        start = start
      ).cancelByIndicator(progressIndicator).execute()
    else throw IllegalArgumentException("Virtual file is not a library")
  }

  override fun convertResponseToBody(responseList: MembersList?): BatchedBody<Member> {
    return BatchedBody(responseList?.items?.map { BatchedItem(it.name, it) }, responseList?.totalRows)
  }

  override fun buildAttributes(
    query: RemoteQuery<LibraryQuery, Unit>,
    batchedItem: BatchedItem<Member>
  ): RemoteMemberAttributes {
    return RemoteMemberAttributes(batchedItem.original, query.request.library)
  }
}
