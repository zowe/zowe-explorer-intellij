package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileSystemEntry
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.applyIfNotNull
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.runWriteActionInEdtAndWait
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.*

class LocalDirToUssDirMoverFactory: OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return LocalDirToUssDirMover(dataOpsManager)
  }
}

class LocalDirToUssDirMover(val dataOpsManager: DataOpsManager): AbstractFileMover()  {
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.source is VirtualFileSystemEntry &&
        operation.source.isDirectory &&
        operation.destination.isDirectory &&
        operation.destination is MFVirtualFile &&
        dataOpsManager.tryToGetAttributes(operation.destination) is RemoteUssAttributes
  }

  fun proceedLocalDirUpload(
    operation: MoveCopyOperation,
    progressIndicator: ProgressIndicator?
  ): Throwable? {
    var throwable: Throwable? = null
    val sourceFile = operation.source
    val destFile = operation.destination
    val destAttributes = operation.destinationAttributes.castOrNull<RemoteUssAttributes>()
      ?: return IllegalStateException("No attributes found for destination directory \'${destFile.name}\'.")

    val destConnectionConfig = destAttributes.requesters.map { it.connectionConfig }.firstOrNull()
      ?: return Throwable("No connection for destination directory found.")

    val pathToDir = destAttributes.path + "/" + sourceFile.name

    if (operation.forceOverwriting) {
      destFile.children.firstOrNull { it.name == sourceFile.name }?.let {
        if (progressIndicator != null) {
          dataOpsManager.performOperation(DeleteOperation(it, dataOpsManager), progressIndicator)
        } else {
          dataOpsManager.performOperation(DeleteOperation(it, dataOpsManager))
        }
      }
    }
    val response = api<DataAPI>(destConnectionConfig).createUssFile(
      authorizationToken = destConnectionConfig.authToken,
      filePath = FilePath(pathToDir),
      CreateUssFile(FileType.DIR, FileMode(7, 7, 7))
    ).applyIfNotNull(progressIndicator) {
      cancelByIndicator(it)
    }.execute()

    if (!response.isSuccessful) {
      return CallException(response, "Cannot upload directory '$pathToDir'.")
    }

    val attributesService =
      dataOpsManager.getAttributesService(RemoteUssAttributes::class.java, MFVirtualFile::class.java)
    var createdDirFile: VirtualFile? = null
    runWriteActionInEdtAndWait {
      createdDirFile = attributesService.getOrCreateVirtualFile(
        RemoteUssAttributes(
          destAttributes.path,
          UssFile(sourceFile.name, "drwxrwxrwx"),
          destConnectionConfig.url,
          destConnectionConfig
        )
      )
    }

    createdDirFile?.let { createdDirFileNotNull ->
      sourceFile.children.forEach {
        val op = MoveCopyOperation(
          it, createdDirFileNotNull, isMove = false, forceOverwriting = false, newName = null, dataOpsManager = dataOpsManager
        )
        if (progressIndicator != null) {
          dataOpsManager.performOperation(op, progressIndicator)
        } else {
          dataOpsManager.performOperation(op)
        }
      }
    }

    return throwable
  }

  override fun run(operation: MoveCopyOperation, progressIndicator: ProgressIndicator) {
    val throwable = try {
      proceedLocalDirUpload(operation, progressIndicator)
    } catch (t: Throwable) {
      t
    }
    if (throwable != null) {
      throw throwable
    }
  }
}