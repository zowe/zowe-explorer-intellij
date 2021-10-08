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

import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.MaskedRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.asMutableList
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.utils.nullIfBlank
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.*
import retrofit2.Response

class DatasetFileFetchProviderFactory : FileFetchProviderFactory {
    override fun buildComponent(dataOpsManager: DataOpsManager): FileFetchProvider<*, *, *> {
        return DatasetFileFetchProvider(dataOpsManager)
    }
}

private val log = log<DatasetFileFetchProvider>()

class DatasetFileFetchProvider(dataOpsManager: DataOpsManager) :
    RemoteAttributedFileFetchBase<DSMask, RemoteDatasetAttributes, MFVirtualFile>(dataOpsManager) {

    override val requestClass = DSMask::class.java

    override val vFileClass = MFVirtualFile::class.java

    override fun fetchResponse(
        query: RemoteQuery<DSMask, Unit>,
        progressIndicator: ProgressIndicator
    ): Collection<RemoteDatasetAttributes> {
        log.info("Fetching DS Lists for $query")
        var attributes: Collection<RemoteDatasetAttributes>? = null
        var fetchedItems: List<Dataset>? = null
        var totalRows: Int? = null
        var start: String? = null
        var fetchNeeded = true
        var failedResponse: Response<DataSetsList>? = null

        while (fetchNeeded) {
            val response = api<DataAPI>(query.connectionConfig).listDataSets(
                authorizationToken = query.connectionConfig.authToken,
                dsLevel = query.request.mask,
                volser = query.request.volser.nullIfBlank(),
                xIBMAttr = XIBMAttr(isTotal = true),
                xIBMMaxItems = BATCH_SIZE,
                start = start
            ).cancelByIndicator(progressIndicator).execute()

            var newBatchSize: Int?
            if (response.isSuccessful) {
                val newBatch = response.body()?.items?.toMutableList()
                totalRows = response.body()?.totalRows
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

                log.info("${query.request} returned ${attributes?.size ?: 0} entities")
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

    private fun buildAttributes(query: RemoteQuery<DSMask, Unit>, dataset: Dataset): RemoteDatasetAttributes {
        return RemoteDatasetAttributes(
            dataset,
            query.urlConnection.url,
            MaskedRequester(
                query.connectionConfig,
                query.request
            ).asMutableList()
        )
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

}
