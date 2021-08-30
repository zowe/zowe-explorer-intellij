package eu.ibagroup.formainframe.dataops.operations.migration

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.operations.OperationRunnerFactory
import eu.ibagroup.formainframe.dataops.operations.RemoteUnitOperation
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.HRecall

class RecallOperationRunner : MigrationRunner<RecallOperation> {

  override fun canRun(operation: RecallOperation): Boolean {
    val file = operation.request.file
    val attributes = service<DataOpsManager>().tryToGetAttributes(file)
    return if (attributes is RemoteDatasetAttributes) {
      attributes.isMigrated
    } else {
      false
    }
  }

  override val operationClass = RecallOperation::class.java

  override fun run(operation: RecallOperation, progressIndicator: ProgressIndicator) {
    progressIndicator.checkCanceled()
    val response = api<DataAPI>(operation.connectionConfig).recallMigratedDataset(
      authorizationToken = operation.connectionConfig.authToken,
      datasetName = operation.request.file.name,
      body = HRecall(wait = true)
    ).cancelByIndicator(progressIndicator).execute()
    if (!response.isSuccessful) {
      throw CallException(
        response,
        "Cannot recall dataset ${operation.request.file.name} on ${operation.connectionConfig.name}"
      )
    }
  }
}

class RecallOperationFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): MigrationRunner<*> {
    return RecallOperationRunner()
  }
}


data class RecallOperationParams(
  val file: VirtualFile
)

data class RecallOperation(
  override val request: RecallOperationParams,
  override val connectionConfig: eu.ibagroup.formainframe.config.connect.ConnectionConfig,
) : RemoteUnitOperation<RecallOperationParams>