package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.MaskedRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.asMutableList
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.nullIfBlank
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.Dataset

class DatasetFileFetchProviderFactory : FileFetchProviderFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): FileFetchProvider<*, *, *> {
    return DatasetFileFetchProvider(dataOpsManager)
  }
}

class DatasetFileFetchProvider(dataOpsManager: DataOpsManager) :
  RemoteAttributedFileFetchBase<DSMask, RemoteDatasetAttributes, MFVirtualFile>(dataOpsManager) {

  override val requestClass = DSMask::class.java

  override val vFileClass = MFVirtualFile::class.java

  override fun fetchResponse(
    query: RemoteQuery<DSMask, Unit>,
    progressIndicator: ProgressIndicator
  ): Collection<RemoteDatasetAttributes> {
    var attributes: Collection<RemoteDatasetAttributes>? = null
    var exception: Throwable? = null
    val response = api<DataAPI>(query.connectionConfig).listDataSets(
      authorizationToken = query.connectionConfig.token,
      dsLevel = query.request.mask,
      volser = query.request.volser.nullIfBlank()
    ).cancelByIndicator(progressIndicator).execute()
    if (response.isSuccessful) {
      attributes = response.body()?.items?.map { buildAttributes(query, it) }
    } else {
      exception = CallException(response, "Cannot retrieve dataset list")
    }

    if (exception != null) {
      throw exception
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
    if (deletingFileAttributes != null) {
      val needsDeletionFromFs = deletingFileAttributes.requesters.all {
        it.connectionConfig == query.connectionConfig && it.queryVolser == query.request.volser
      }
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

  override fun convertResponseToFile(response: RemoteDatasetAttributes): MFVirtualFile? {
    return if (!response.isMigrated) {
      super.convertResponseToFile(response)
    } else {
      null
    }
  }

}