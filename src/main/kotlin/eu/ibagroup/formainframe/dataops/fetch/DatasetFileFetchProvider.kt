package eu.ibagroup.formainframe.dataops.fetch

import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.config.connect.username
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.api.api
import eu.ibagroup.formainframe.dataops.api.enqueueSync
import eu.ibagroup.formainframe.dataops.attributes.MaskedRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.utils.asMutableList
import eu.ibagroup.formainframe.utils.nullIfBlank
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.Dataset
import java.io.IOException


class DatasetFileFetchProvider :
  RemoteAttributedFileFetchBase<DSMask, RemoteDatasetAttributes, MFVirtualFile>() {

  override val requestClass = DSMask::class.java

  override val vFileClass = MFVirtualFile::class.java

  override fun makeFetchTaskTitle(query: RemoteQuery<DSMask>): String {
    return "Fetching listings for ${query.request.mask}"
  }

  override fun fetchResponse(query: RemoteQuery<DSMask>): Collection<RemoteDatasetAttributes> {
    var attributes: Collection<RemoteDatasetAttributes>? = null
    var exception: Throwable = IOException("Cannot fetch ${query.request.mask}")
    api<DataAPI>(query.connectionConfig).listDataSets(
      authorizationToken = query.connectionConfig.token,
      dsLevel = query.request.mask,
      volser = query.request.volser.nullIfBlank()
    ).enqueueSync {
      onResponse { _, response ->
        if (response.isSuccessful) {
          attributes = response.body()?.items?.map { buildAttributes(query, it) }
        } else {
          exception = IOException("${response.code()} " + (response.errorBody()?.string() ?: ""))
        }
      }
      onException { _, t ->
        exception = t
      }
    }
    return attributes ?: throw exception
  }

  private fun buildAttributes(query: RemoteQuery<DSMask>, dataset: Dataset): RemoteDatasetAttributes {
    return RemoteDatasetAttributes(
      dataset,
      query.urlConnection.url,
      MaskedRequester(
        username(query.connectionConfig),
        query.request
      ).asMutableList()
    )
  }

  override val responseClass = RemoteDatasetAttributes::class.java

  override fun makeSecondaryTitle(query: RemoteQuery<DSMask>): String {
    val firstPart = "Origin ${query.urlConnection.url}"
    val volser = query.request.volser
    val secondPart = if (volser.isNotBlank()) " volser $volser" else ""
    return firstPart + secondPart
  }

  override fun cleanupUnusedFile(file: MFVirtualFile, query: RemoteQuery<DSMask>) {
    val deletingFileAttributes = attributesService.getAttributes(file)
    if (deletingFileAttributes != null) {
      val needsDeletionFromFs = deletingFileAttributes.requesters.all {
        it.user == username(query.connectionConfig) && it.queryVolser == query.request.volser
      }
      if (needsDeletionFromFs) {
        attributesService.clearAttributes(file)
        file.delete(this)
      } else {
        attributesService.updateAttributes(file) {
          requesters.removeAll {
            it.user == username(query.connectionConfig) && it.queryVolser == query.request.volser
          }
        }
      }
    }
  }

}