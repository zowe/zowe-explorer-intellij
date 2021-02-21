package eu.ibagroup.formainframe.dataops.allocation

import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.api.api
import eu.ibagroup.formainframe.dataops.api.enqueueSync
import eu.ibagroup.formainframe.explorer.ui.AddMemberState
import eu.ibagroup.r2z.DataAPI

class MemberAllocator : RemoteAllocatorBase<AddMemberState>() {

  override val requestClass = AddMemberState::class.java

  override val title = "Crating member"

  override fun performAllocationRequest(query: RemoteQuery<AddMemberState>): AllocationStatus {
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