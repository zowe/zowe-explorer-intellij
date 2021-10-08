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

import com.intellij.openapi.progress.ProgressIndicator
import com.jetbrains.rd.util.getLogger
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.Member
import eu.ibagroup.r2z.MembersList
import eu.ibagroup.r2z.XIBMAttr
import retrofit2.Response

data class LibraryQuery(val library: MFVirtualFile)

class MemberFileFetchProviderFactory : FileFetchProviderFactory {
    override fun buildComponent(dataOpsManager: DataOpsManager): FileFetchProvider<*, *, *> {
        return MemberFileFetchProvider(dataOpsManager)
    }
}

private val log = log<MemberFileFetchProvider>()

class MemberFileFetchProvider(private val dataOpsManager: DataOpsManager) :
    RemoteAttributedFileFetchBase<LibraryQuery, RemoteMemberAttributes, MFVirtualFile>(dataOpsManager) {

    private val remoteDatasetAttributesService by lazy {
        dataOpsManager.getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
    }

    override val requestClass = LibraryQuery::class.java

    override val vFileClass = MFVirtualFile::class.java

    override val responseClass = RemoteMemberAttributes::class.java

    override fun fetchResponse(
        query: RemoteQuery<LibraryQuery, Unit>,
        progressIndicator: ProgressIndicator
    ): Collection<RemoteMemberAttributes> {
        val libraryAttributes = remoteDatasetAttributesService.getAttributes(query.request.library)
        log.info("Fetching Members for $query\nlibraryAttributes=$libraryAttributes")
        return if (libraryAttributes != null) {
            var attributes: Collection<RemoteMemberAttributes>? = null
            var fetchedItems: List<Member>? = null
            var totalRows: Int? = null
            var start: String? = null
            var fetchNeeded = true
            var failedResponse: Response<MembersList>? = null

            while (fetchNeeded) {
                val response = api<DataAPI>(query.connectionConfig).listDatasetMembers(
                    authorizationToken = query.connectionConfig.authToken,
                    datasetName = libraryAttributes.name,
                    xIBMAttr = XIBMAttr(isTotal = true),
                    xIBMMaxItems = BATCH_SIZE,
                    start = start
                ).cancelByIndicator(progressIndicator).execute()

                var newBatchSize: Int?
                if (response.isSuccessful) {
                    val newBatch = response.body()?.items?.toMutableList()
                    newBatchSize = newBatch?.size

                    totalRows = response.body()?.totalRows
                    if (fetchedItems != null && newBatch?.size != 0) {
                        newBatch?.removeFirst()
                    }

                    fetchedItems = newBatch?.let {
                        fetchedItems?.toMutableList()?.apply {
                            addAll(newBatch)
                        }
                    } ?: newBatch
                    if (fetchedItems?.size != 0) {
                        start = fetchedItems?.last()?.name
                    }

                    log.info("${query.request} returned ${attributes?.size ?: 0} entities")
                } else {
                    failedResponse = response
                    break
                }
                fetchNeeded = if (totalRows != null && newBatchSize != null) {
                    newBatchSize != totalRows
                } else {
                    false
                }
            }

            attributes = fetchedItems?.map { RemoteMemberAttributes(it, query.request.library) }

            if (failedResponse != null) {
                throw CallException(failedResponse, "Cannot retrieve member list")
            }

            return attributes ?: emptyList()
        } else throw IllegalArgumentException("Virtual file is not a library")
    }

    override fun cleanupUnusedFile(file: MFVirtualFile, query: RemoteQuery<LibraryQuery, Unit>) {
        log.info("About to clean-up file=$file, query=$query")
        attributesService.clearAttributes(file)
        file.delete(this)
    }
}
