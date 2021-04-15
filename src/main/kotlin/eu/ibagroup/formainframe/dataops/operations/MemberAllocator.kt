package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.r2z.DataAPI

class MemberAllocatorFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): Allocator<*> {
    return MemberAllocator()
  }
}

data class MemberAllocationOperation(
  override val request: MemberAllocationParams,
  override val connectionConfig: ConnectionConfig,
  override val urlConnection: UrlConnection
) : RemoteAllocationOperation<MemberAllocationParams>

class MemberAllocator : Allocator<MemberAllocationOperation> {

  override val operationClass = MemberAllocationOperation::class.java

  override fun run(
    operation: MemberAllocationOperation,
    progressIndicator: ProgressIndicator
  ) {
    progressIndicator.checkCanceled()
    val request = api<DataAPI>(operation.connectionConfig).writeToDatasetMember(
      authorizationToken = operation.connectionConfig.token,
      datasetName = operation.request.datasetName,
      memberName = operation.request.memberName,
      content = ""
    ).cancelByIndicator(progressIndicator).execute()
    if (!request.isSuccessful) {
      throw Throwable(request.code().toString())
    }
  }
}

data class MemberAllocationParams(val datasetName: String, var memberName: String = "")