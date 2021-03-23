package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.api.enqueueSync
import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.MaskedRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.utils.asMutableList
import eu.ibagroup.formainframe.utils.nullIfBlank
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.Dataset
import java.io.IOException

class DatasetFileFetchProviderFactory : FileFetchProviderFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): FileFetchProvider<*, *, *> {
    return DatasetFileFetchProvider(dataOpsManager)
  }
}

class DatasetFileFetchProvider(dataOpsManager: DataOpsManager) :
  RemoteAttributedFileFetchBase<DSMask, RemoteDatasetAttributes, MFVirtualFile>(dataOpsManager) {

  override val requestClass = DSMask::class.java

  override val vFileClass = MFVirtualFile::class.java

  override fun fetchResponse(query: RemoteQuery<DSMask, Unit>): Collection<RemoteDatasetAttributes> {
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
          run{
            exception = IOException("${response.code()} " + (response.errorBody()?.string() ?: ""))
            if (response.code() == 401) {
              ApplicationManager.getApplication().invokeLater{
                Messages.showWarningDialog("Cannot fetch because of wrong credentials of the user",
                        response.code().toString() + " - " + response.message())
              }
            }
          }
        }
      }
      onException { _, t ->
        exception = t
      }
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

}