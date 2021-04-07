package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.findAnyNullable
import eu.ibagroup.formainframe.utils.runWriteActionOnWriteThread
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.FilePath
import eu.ibagroup.r2z.MoveUssFile
import eu.ibagroup.r2z.RenameData

class RenameOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return RenameOperationRunner(dataOpsManager)
  }
}

class RenameOperationRunner(private val dataOpsManager: DataOpsManager) : OperationRunner<RenameOperation, Unit> {

  override val operationClass = RenameOperation::class.java

  override val resultClass = Unit::class.java

  override fun canRun(operation: RenameOperation): Boolean {
    return with(operation.attributes) {
      this is RemoteMemberAttributes || this is RemoteDatasetAttributes || this is RemoteUssAttributes
    }
  }

  override fun run(
    operation: RenameOperation,
    progressIndicator: ProgressIndicator
  ) {
    when (val attributes = operation.attributes) {
      is RemoteDatasetAttributes -> {
        attributes.requesters.stream().map {
          try {
            progressIndicator.checkCanceled()
            val response = api<DataAPI>(it.connectionConfig).renameDataset(
              authorizationToken = it.connectionConfig.token,
              body = RenameData(
                fromDataset = RenameData.FromDataset(
                  oldDatasetName = attributes.name
                )
              ),
              toDatasetName = operation.newName
            ).cancelByIndicator(progressIndicator).execute()
            if (response.isSuccessful) {
              runWriteActionOnWriteThread {
                operation.file.rename(this@RenameOperationRunner, operation.newName)
              }
              true
            } else {
              false
            }
          } catch (e: Throwable) {
            false
          }
        }.filter { it }.findAnyNullable() ?: throw UnknownError("")
      }
      is RemoteMemberAttributes -> {
        val parentAttributes = dataOpsManager.tryToGetAttributes(attributes.libraryFile) as RemoteDatasetAttributes
        parentAttributes.requesters.stream().map {
          try {
            progressIndicator.checkCanceled()
            val response = api<DataAPI>(it.connectionConfig).renameDatasetMember(
              authorizationToken = it.connectionConfig.token,
              body = RenameData(
                fromDataset = RenameData.FromDataset(
                  oldDatasetName = parentAttributes.datasetInfo.name,
                  oldMemberName = attributes.memberInfo.name
                )
              ),
              toDatasetName = parentAttributes.datasetInfo.name,
              memberName = operation.newName
            ).cancelByIndicator(progressIndicator).execute()
            if (response.isSuccessful) {
              runWriteActionOnWriteThread {
                operation.file.rename(this@RenameOperationRunner, operation.newName)
              }
              true
            } else {
              false
            }
          } catch (e: Throwable) {
            false
          }
        }.filter { it }.findAnyNullable() ?: throw UnknownError("")
      }
      is RemoteUssAttributes -> {
        val parentDirPath = attributes.parentDirPath
        attributes.requesters.stream().map {
          try {
            progressIndicator.checkCanceled()
            val response = api<DataAPI>(it.connectionConfig).moveUssFile(
              authorizationToken = it.connectionConfig.token,
              body = MoveUssFile(
                from = attributes.path
              ),
              filePath = FilePath("$parentDirPath/${operation.newName}")
            ).cancelByIndicator(progressIndicator).execute()
            if (response.isSuccessful) {
              runWriteActionOnWriteThread {
                operation.file.rename(this@RenameOperationRunner, operation.newName)
              }
              true
            } else {
              false
            }
          } catch (e: Throwable) {
            false
          }
        }.filter { it }.findAnyNullable() ?: throw UnknownError("")
      }
    }
  }
}