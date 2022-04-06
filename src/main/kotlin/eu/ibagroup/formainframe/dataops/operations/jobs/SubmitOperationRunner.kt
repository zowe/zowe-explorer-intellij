package eu.ibagroup.formainframe.dataops.operations.jobs

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.dataops.operations.OperationRunnerFactory
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.r2z.JESApi
import eu.ibagroup.r2z.SubmitFileNameBody
import eu.ibagroup.r2z.SubmitJobRequest

class SubmitOperationRunner : OperationRunner<SubmitJobOperation, SubmitJobRequest> {

  override val operationClass = SubmitJobOperation::class.java

  override fun run(operation: SubmitJobOperation, progressIndicator: ProgressIndicator): SubmitJobRequest {
    progressIndicator.checkCanceled()

    val response = api<JESApi>(operation.connectionConfig).submitJobRequest(
      basicCredentials = operation.connectionConfig.authToken,
      body = SubmitFileNameBody(operation.request.submitFilePath)
    ).cancelByIndicator(progressIndicator).execute()
    val body = response.body()
    if (!response.isSuccessful || body == null) {
      throw CallException(
        response,
        "Cannot submit ${operation.request.submitFilePath} on ${operation.connectionConfig.name}"
      )
    }
    return body
  }

  override val resultClass = SubmitJobRequest::class.java

  override fun canRun(operation: SubmitJobOperation): Boolean {
    return true
  }
}

class SubmitJobOperationFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return SubmitOperationRunner()
  }
}

data class SubmitOperationParams(
  val submitFilePath: String
)

data class SubmitJobOperation(
  override val request: SubmitOperationParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteQuery<SubmitOperationParams, SubmitJobRequest> {
  override val resultClass = SubmitJobRequest::class.java
}
