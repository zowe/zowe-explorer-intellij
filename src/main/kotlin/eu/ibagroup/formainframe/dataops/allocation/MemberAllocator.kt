package eu.ibagroup.formainframe.dataops.allocation

import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.api.api
import eu.ibagroup.formainframe.dataops.api.enqueueSync
import eu.ibagroup.r2z.DataAPI

class MemberAllocator : RemoteAllocatorBase<MemberAllocationParams>() {

  override val requestClass = MemberAllocationParams::class.java

  override val title = "Crating member"

  override fun performAllocationRequest(query: RemoteQuery<MemberAllocationParams>): AllocationStatus {
    var status = AllocationStatus.FAILED
    var throwable: Throwable? = null
    api<DataAPI>(query.connectionConfig).writeToDatasetMember(
      authorizationToken = query.connectionConfig.token,
      datasetName = query.request.datasetName,
      memberName = query.request.memberName,
      content = ""

    ).enqueueSync {
      onResponse { call, response ->
        if (response.isSuccessful) {
          status = AllocationStatus.SUCCESS
        }
      }
      onException { call, t ->
        throwable = t
      }

    }
    if (throwable != null) throw throwable!!
    return status
  }
}