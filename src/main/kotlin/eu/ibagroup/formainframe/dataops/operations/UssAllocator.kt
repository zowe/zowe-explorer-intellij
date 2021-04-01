package eu.ibagroup.formainframe.dataops.operations

import com.google.gson.Gson
import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.r2z.CreateUssFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.FilePath

class UssAllocatorFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): Allocator<*> {
    return UssAllocator(dataOpsManager)
  }
}

data class UssAllocationParams(
  val parameters: CreateUssFile,
  val fileName: String,
  val path: String,
)

data class UssAllocationOperation(
  override val request: UssAllocationParams,
  override val connectionConfig: ConnectionConfig,
  override val urlConnection: UrlConnection
) : RemoteAllocationOperation<UssAllocationParams>

class UssAllocator(dataOpsManager: DataOpsManager) : RemoteAllocatorBase<UssAllocationOperation>(dataOpsManager) {

  override val operationClass = UssAllocationOperation::class.java

  override fun performAllocationRequest(query: UssAllocationOperation, progressIndicator: ProgressIndicator?) {
    progressIndicator?.checkCanceled()
    val response = api<DataAPI>(query.connectionConfig).createUssFile(
      authorizationToken = query.connectionConfig.token,
      filePath = FilePath(query.request.path + "/" + query.request.fileName),
      body = query.request.parameters
    ).execute()
    if (!response.isSuccessful) {
      throw CallException(response, "Cannot allocate dataset ${query.request.fileName} on ${query.connectionConfig.name}")
    }
  }
}
