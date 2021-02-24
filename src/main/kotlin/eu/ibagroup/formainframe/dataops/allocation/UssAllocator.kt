package eu.ibagroup.formainframe.dataops.allocation

import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.api.api
import eu.ibagroup.formainframe.dataops.api.enqueueSync
import eu.ibagroup.formainframe.explorer.ui.CreateFileState
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.FilePath

class UssAllocator : RemoteAllocatorBase<CreateFileState>() {

  override val requestClass = CreateFileState::class.java

  override val title = "Allocate USS File/Dir"

  override fun performAllocationRequest(query: RemoteQuery<CreateFileState>): AllocationStatus {
    var status = AllocationStatus.FAILED
    var throwable: Throwable? = null
    api<DataAPI>(query.connectionConfig).createUssFile(
      authorizationToken = query.connectionConfig.token,
      filePath = FilePath(query.request.path + "/" + query.request.fileName),
      body = query.request.parameters
    ).enqueueSync {
      onResponse { _, response ->
        if (response.isSuccessful) {
          status = AllocationStatus.SUCCESS
        }
      }
      onException { _, t ->
        throwable = t
      }

    }
    if (throwable != null) throw throwable!!
    return status
  }
}