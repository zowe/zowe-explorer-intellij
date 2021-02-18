package eu.ibagroup.formainframe.dataops.allocation

import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.api.api
import eu.ibagroup.formainframe.dataops.api.enqueueSync
import eu.ibagroup.r2z.DataAPI

class DatasetAllocator : RemoteAllocatorBase<DatasetAllocationParams>() {

  override val title: String
    get() = "Allocate dataset"

  override fun performAllocationRequest(query: RemoteQuery<DatasetAllocationParams>): AllocationStatus {
    var status = AllocationStatus.FAILED
    var throwable: Throwable? = null
    api<DataAPI>(query.connectionConfig).createDataset(
      authorizationToken = query.connectionConfig.token,
      datasetName = query.request.datasetName,
      body = query.request.allocationParameters
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

  override val requestClass: Class<out DatasetAllocationParams> = DatasetAllocationParams::class.java
}

