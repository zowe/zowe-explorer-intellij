package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.sendVfsChangesTopic
import eu.ibagroup.r2z.DataAPI

class ForceRenameOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return ForceRenameOperationRunner(dataOpsManager)
  }
}

class ForceRenameOperationRunner(private val dataOpsManager: DataOpsManager) :
  OperationRunner<ForceRenameOperation, Unit> {

  override val operationClass = ForceRenameOperation::class.java
  override val resultClass = Unit::class.java

  override fun canRun(operation: ForceRenameOperation): Boolean {
    return (when (operation.attributes) {
      is RemoteUssAttributes -> true
      else -> false
    })
  }

  override fun run(operation: ForceRenameOperation, progressIndicator: ProgressIndicator) {
    val sourceFile = operation.file as MFVirtualFile
    val fileName = sourceFile.filenameInternal
    val attributes = operation.attributes as RemoteUssAttributes
    val parentDirPath = attributes.parentDirPath
    val children = sourceFile.parent?.children
    attributes.requesters.map { requester ->
      try {
        progressIndicator.checkCanceled()
        if (!sourceFile.isDirectory) {
          sourceFile.parent?.let {
            dataOpsManager.performOperation(
              MoveCopyOperation(
                sourceFile,
                sourceFile.parent!!,
                true,
                operation.override,
                operation.newName,
                dataOpsManager,
                operation.explorer
              ), progressIndicator
            )
          }
          sendVfsChangesTopic()
        } else {
          children?.forEach {
            if (it.isDirectory && it.name == operation.newName) {
              if (it.children?.size == 0) {
                val resp = api<DataAPI>(requester.connectionConfig).deleteUssFile(
                  authorizationToken = requester.connectionConfig.authToken,
                  filePath = "$parentDirPath/${operation.newName}"
                ).cancelByIndicator(progressIndicator).execute()
                if (!resp.isSuccessful) {
                  throw CallException(
                    resp,
                    "Remote exception occurred. Unable to rename source directory $fileName"
                  )
                }
                sourceFile.parent?.let {
                  dataOpsManager.performOperation(
                    MoveCopyOperation(
                      sourceFile,
                      sourceFile.parent!!,
                      true,
                      operation.override,
                      operation.newName,
                      dataOpsManager,
                      operation.explorer
                    ), progressIndicator
                  )
                  sendVfsChangesTopic()
                }
              } else {
                throw RuntimeException(
                  "Can't rename source directory $fileName".plus(". Destination directory is not empty.")
                )
              }
            }
          }
        }
      } catch (e: Throwable) {
        throw RuntimeException(e)
      }
    }
  }
}
