package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.UnitOperation
import eu.ibagroup.formainframe.dataops.attributes.*
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.findAnyNullable
import eu.ibagroup.formainframe.utils.runWriteActionInEdt
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.XIBMOption


class DeleteRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return DeleteOperationRunner(dataOpsManager)
  }
}

class DeleteOperationRunner(private val dataOpsManager: DataOpsManager) :
  OperationRunner<DeleteOperation, Unit> {
  override val operationClass = DeleteOperation::class.java

  override fun run(
    operation: DeleteOperation,
    progressIndicator: ProgressIndicator
  ) {
    when (val attr = operation.attributes) {
      is RemoteDatasetAttributes -> {
        service<AnalyticsService>().trackAnalyticsEvent(FileEvent(attr, FileAction.DELETE))

        var throwable: Throwable? = null
        attr.requesters.stream().map {
          try {
            progressIndicator.checkCanceled()
            val response = api<DataAPI>(it.connectionConfig).deleteDataset(
              authorizationToken = it.connectionConfig.authToken,
              datasetName = attr.name
            ).cancelByIndicator(progressIndicator).execute()
            if (response.isSuccessful) {
              runWriteActionInEdt { operation.file.delete(this@DeleteOperationRunner) }
              true
            } else {
              throwable =  CallException(response, "Cannot delete data set")
              false
            }
          } catch (t: Throwable) {
            throwable = t
            false
          }
        }.filter { it }.findAnyNullable() ?: throw (throwable ?: Throwable("Unknown"))
      }
      is RemoteMemberAttributes -> {
        service<AnalyticsService>().trackAnalyticsEvent(FileEvent(attr, FileAction.DELETE))

        val libraryAttributes = attr.getLibraryAttributes(dataOpsManager)
        if (libraryAttributes != null) {
          var throwable: Throwable? = null
          libraryAttributes.requesters.stream().map {
            try {
              progressIndicator.checkCanceled()
              val response = api<DataAPI>(it.connectionConfig).deleteDatasetMember(
                authorizationToken = it.connectionConfig.authToken,
                datasetName = libraryAttributes.name,
                memberName = attr.name
              ).cancelByIndicator(progressIndicator).execute()
              if (response.isSuccessful) {
                runWriteActionInEdt { operation.file.delete(this@DeleteOperationRunner) }
                true
              } else {
                throwable =  CallException(response, "Cannot delete data set member")
                false
              }
            } catch (t: Throwable) {
              throwable = t
              false
            }
          }.filter { it }.findAnyNullable() ?: throw (throwable ?: Throwable("Unknown"))
        }
      }
      is RemoteUssAttributes -> {
        service<AnalyticsService>().trackAnalyticsEvent(FileEvent(attr, FileAction.DELETE))

        var throwable: Throwable? = null
        attr.requesters.stream().map {
          try {
            progressIndicator.checkCanceled()
            val response = api<DataAPI>(it.connectionConfig).deleteUssFile(
              authorizationToken = it.connectionConfig.authToken,
              filePath = attr.path.substring(1),
              xIBMOption = XIBMOption.RECURSIVE
            ).cancelByIndicator(progressIndicator).execute()
            if (response.isSuccessful) {
              runWriteActionInEdt { operation.file.delete(this@DeleteOperationRunner) }
              true
            } else {
              throwable = CallException(response, "Cannot delete USS File/Directory")
              false
            }
          } catch (t: Throwable) {
            throwable = t
            false
          }
        }.filter { it }.findAnyNullable() ?: throw (throwable ?: Throwable("Unknown"))
      }
    }
  }

  override val resultClass = Unit::class.java

  override fun canRun(operation: DeleteOperation): Boolean {
    return true
  }

}

data class DeleteOperation(
  val file: VirtualFile,
  val attributes: FileAttributes
) : UnitOperation {
  constructor(file: VirtualFile, dataOpsManager: DataOpsManager) : this(
    file = file,
    attributes = dataOpsManager.tryToGetAttributes(file)
      ?: throw IllegalArgumentException("Deleting file should have attributes")
  )
}