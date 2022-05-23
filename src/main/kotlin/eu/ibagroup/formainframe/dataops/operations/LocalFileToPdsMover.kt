package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileSystemEntry
import eu.ibagroup.formainframe.api.apiWithBytesConverter
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.dataops.content.synchronizer.addNewLine
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.applyIfNotNull
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.runWriteActionInEdtAndWait
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.XIBMDataType

class LocalFileToPdsMoverFactory: OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return LocalFileToPdsMover(dataOpsManager)
  }
}

class LocalFileToPdsMover(val dataOpsManager: DataOpsManager): AbstractFileMover() {
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.source is VirtualFileSystemEntry &&
        !operation.source.isDirectory &&
        operation.destination.isDirectory &&
        operation.destination is MFVirtualFile &&
        dataOpsManager.tryToGetAttributes(operation.destination) is RemoteDatasetAttributes
  }

  private fun proceedLocalUpload (
    operation: MoveCopyOperation,
    progressIndicator: ProgressIndicator
  ): Throwable? {
    var throwable: Throwable? = null
    val sourceFile = operation.source
    val destFile = operation.destination
    val destAttributes = operation.destinationAttributes.castOrNull<RemoteDatasetAttributes>()
      ?: return IllegalStateException("No attributes found for destination directory \'${destFile.name}\'.")

    val destConnectionConfig = destAttributes.requesters.map { it.connectionConfig }.firstOrNull()
      ?: return Throwable("No connection for destination directory found.")

    var memberName = sourceFile.name.filter { it.isLetterOrDigit() }.take(8)
    if (memberName.isEmpty()) {
      memberName = "empty"
    }

    val xIBMDataType = if (sourceFile.fileType.isBinary) XIBMDataType(XIBMDataType.Type.BINARY) else XIBMDataType(XIBMDataType.Type.TEXT)
    val sourceContent = sourceFile.contentsToByteArray()
    val contentToUpload = if (sourceFile.fileType.isBinary) sourceContent else sourceContent.filter { it != '\r'.code.toByte() }.toByteArray()

    val response = apiWithBytesConverter<DataAPI>(destConnectionConfig).writeToDatasetMember(
      authorizationToken = destConnectionConfig.authToken,
      datasetName = destAttributes.name,
      memberName = memberName,
      content = contentToUpload.addNewLine(),
      xIBMDataType = xIBMDataType
    ).applyIfNotNull(progressIndicator) { indicator ->
      cancelByIndicator(indicator)
    }.execute()

    if (!response.isSuccessful &&
      response.errorBody()?.string()?.contains("Truncation of a record occurred during an I/O operation.") != true) {
      throwable = CallException(response, "Cannot upload data to '${destAttributes.name}(${memberName})'")
    } else {
      destFile.children.firstOrNull { it.name.uppercase() == memberName.uppercase() }?.let { file ->
        runWriteActionInEdtAndWait {
          val syncProvider = DocumentedSyncProvider(file, { _, _, _ -> false }, { th -> throwable = th })
          val contentSynchronizer = dataOpsManager.getContentSynchronizer(file)

          if (contentSynchronizer == null) {
            throwable = IllegalArgumentException("Cannot get content synchronizer for file '${file.name}'")
          } else {
            contentSynchronizer.synchronizeWithRemote(syncProvider, progressIndicator)
          }
        }
      }
    }

    return throwable
  }
  override fun run(operation: MoveCopyOperation, progressIndicator: ProgressIndicator) {
    val throwable = try {
      proceedLocalUpload(operation, progressIndicator)
    } catch (t: Throwable) {
      t
    }
    if (throwable != null) {
      throw throwable
    }
  }
}