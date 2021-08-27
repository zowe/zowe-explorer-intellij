package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.vfs.MFVirtualFile
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
