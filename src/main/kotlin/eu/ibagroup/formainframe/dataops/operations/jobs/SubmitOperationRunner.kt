package eu.ibagroup.formainframe.dataops.operations.jobs

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
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

    val fileName = when (val attributes = service<DataOpsManager>().tryToGetAttributes(operation.request.file)) {
      is RemoteUssAttributes -> {
        attributes.path
      }
      is RemoteDatasetAttributes -> {
        "//'${attributes.datasetInfo.name}'"
      }
      else -> {
        val castedAttributes = attributes as RemoteMemberAttributes
        val parentFileAttributes = service<DataOpsManager>().tryToGetAttributes(castedAttributes.parentFile) as RemoteDatasetAttributes
        "//'${parentFileAttributes.datasetInfo.name}(${castedAttributes.info.name})'"
      }
    }

    val response = api<JESApi>(operation.connectionConfig).submitJobRequest(
      basicCredentials = operation.connectionConfig.authToken,
      body = SubmitFileNameBody(fileName)
    ).cancelByIndicator(progressIndicator).execute()
    val body = response.body()
    if (!response.isSuccessful || body == null) {
      throw CallException(
        response,
        "Cannot submit ${operation.request.file.name} on ${operation.connectionConfig.name}"
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
  val file: VirtualFile
)

data class SubmitJobOperation(
  override val request: SubmitOperationParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteQuery<SubmitOperationParams, SubmitJobRequest> {
  override val resultClass = SubmitJobRequest::class.java
}
