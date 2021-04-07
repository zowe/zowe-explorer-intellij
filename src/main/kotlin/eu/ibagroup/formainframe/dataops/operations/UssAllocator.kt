package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.r2z.CreateUssFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.FilePath

class UssAllocatorFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): Allocator<*> {
    return UssAllocator()
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

class UssAllocator : Allocator<UssAllocationOperation> {

  override val operationClass = UssAllocationOperation::class.java

  override fun run(
    operation: UssAllocationOperation,
    progressIndicator: ProgressIndicator
  ) {
    progressIndicator.checkCanceled()
    val response = api<DataAPI>(operation.connectionConfig).createUssFile(
      authorizationToken = operation.connectionConfig.token,
      filePath = FilePath(operation.request.path + "/" + operation.request.fileName),
      body = operation.request.parameters
    ).cancelByIndicator(progressIndicator).execute()
    if (!response.isSuccessful) {
      throw CallException(
        response,
        "Cannot allocate dataset ${operation.request.fileName} on ${operation.connectionConfig.name}"
      )
    }
  }
}
