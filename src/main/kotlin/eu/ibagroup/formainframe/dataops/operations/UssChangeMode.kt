package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.r2z.ChangeMode
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.FilePath

class UssChangeModeFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<UssChangeModeOperation, Unit> {
    return UssChangeMode()
  }
}

data class UssChangeModeParams(
  val parameters: ChangeMode,
  val path: String,
)

data class UssChangeModeOperation(
  override val request: UssChangeModeParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteUnitOperation<UssChangeModeParams>

class UssChangeMode : OperationRunner<UssChangeModeOperation, Unit> {

  override val operationClass = UssChangeModeOperation::class.java
  override val resultClass = Unit::class.java

  override fun run(
    operation: UssChangeModeOperation,
    progressIndicator: ProgressIndicator
  ) {
    progressIndicator.checkCanceled()
    val response = api<DataAPI>(operation.connectionConfig).changeFileMode(
      authorizationToken = operation.connectionConfig.authToken,
      filePath = FilePath(operation.request.path),
      body = operation.request.parameters
    ).cancelByIndicator(progressIndicator).execute()
    if (!response.isSuccessful) {
      throw CallException(
        response,
        "Cannot change file mode on ${operation.request.path}"
      )
    }
  }

  override fun canRun(operation: UssChangeModeOperation): Boolean {
    return true
  }
}