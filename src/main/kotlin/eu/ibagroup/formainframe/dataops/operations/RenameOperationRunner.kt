package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.vfs.sendVfsChangesTopic
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
        attributes.requesters.map {
          try {
            progressIndicator.checkCanceled()
            val response = api<DataAPI>(it.connectionConfig).renameDataset(
              authorizationToken = it.connectionConfig.authToken,
              body = RenameData(
                fromDataset = RenameData.FromDataset(
                  oldDatasetName = attributes.name
                )
              ),
              toDatasetName = operation.newName
            ).cancelByIndicator(progressIndicator).execute()
            if (response.isSuccessful) {
              sendVfsChangesTopic()
            } else {
              throw CallException(response, "Unable to rename the selected dataset")
            }
          } catch (e: Throwable) {
            if (e is CallException) { throw e } else { throw RuntimeException(e) }
          }
        }
      }
      is RemoteMemberAttributes -> {
        val parentAttributes = dataOpsManager.tryToGetAttributes(attributes.parentFile) as RemoteDatasetAttributes
        parentAttributes.requesters.map {
          try {
            progressIndicator.checkCanceled()
            val response = api<DataAPI>(it.connectionConfig).renameDatasetMember(
              authorizationToken = it.connectionConfig.authToken,
              body = RenameData(
                fromDataset = RenameData.FromDataset(
                  oldDatasetName = parentAttributes.datasetInfo.name,
                  oldMemberName = attributes.info.name
                )
              ),
              toDatasetName = parentAttributes.datasetInfo.name,
              memberName = operation.newName
            ).cancelByIndicator(progressIndicator).execute()
            if (response.isSuccessful) {
              sendVfsChangesTopic()
            } else {
              throw CallException(response, "Unable to rename the selected member")
            }
          } catch (e: Throwable) {
            if (e is CallException) { throw e } else { throw RuntimeException(e) }
          }
        }
      }
      is RemoteUssAttributes -> {
        val parentDirPath = attributes.parentDirPath
        attributes.requesters.map {
          try {
            progressIndicator.checkCanceled()
            val response = api<DataAPI>(it.connectionConfig).moveUssFile(
              authorizationToken = it.connectionConfig.authToken,
              body = MoveUssFile(
                from = attributes.path
              ),
              filePath = FilePath("$parentDirPath/${operation.newName}")
            ).cancelByIndicator(progressIndicator).execute()
            if (response.isSuccessful) {
              sendVfsChangesTopic()
            } else {
              throw CallException(response, "Unable to rename the selected file or directory")
            }
          } catch (e: Throwable) {
            if (e is CallException) { throw e } else { throw RuntimeException(e) }
          }
        }
      }
    }
  }
}