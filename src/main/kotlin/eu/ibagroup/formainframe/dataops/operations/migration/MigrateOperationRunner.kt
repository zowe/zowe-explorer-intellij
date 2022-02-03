package eu.ibagroup.formainframe.dataops.operations.migration

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.operations.OperationRunnerFactory
import eu.ibagroup.formainframe.dataops.operations.RemoteUnitOperation
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.HMigrate

data class MigrateOperationParams(val file: VirtualFile)

class MigrateOperationRunner : MigrationRunner<MigrateOperation> {

  override fun canRun(operation: MigrateOperation): Boolean {
    val file = operation.request.file
    val attributes = service<DataOpsManager>().tryToGetAttributes(file)
    return if (attributes is RemoteDatasetAttributes) {
      !attributes.isMigrated
    } else {
      false
    }
  }

  override val operationClass = MigrateOperation::class.java

  override fun run(operation: MigrateOperation, progressIndicator: ProgressIndicator) {
    progressIndicator.checkCanceled()
    val response = api<DataAPI>(operation.connectionConfig).migrateDataset(
      authorizationToken = operation.connectionConfig.authToken,
      datasetName = operation.request.file.name,
      body = HMigrate(wait = true)
    ).cancelByIndicator(progressIndicator).execute()
    if (!response.isSuccessful) {
      throw CallException(
        response,
        "Cannot migrate dataset ${operation.request.file.name} on ${operation.connectionConfig.name}"
      )
    }
  }
}

class MigrateOperationFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): MigrationRunner<*> {
    return MigrateOperationRunner()
  }
}

data class MigrateOperation(
  override val request: MigrateOperationParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteUnitOperation<MigrateOperationParams>