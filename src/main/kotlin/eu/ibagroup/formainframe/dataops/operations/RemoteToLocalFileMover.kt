package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileImpl
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.utils.runWriteActionInEdtAndWait
import eu.ibagroup.formainframe.vfs.MFVirtualFile

class RemoteToLocalFileMoverFactory: OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return RemoteToLocalFileMover(dataOpsManager)
  }
}

class RemoteToLocalFileMover(val dataOpsManager: DataOpsManager): AbstractFileMover() {
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return !operation.source.isDirectory &&
        operation.source is MFVirtualFile &&
        operation.destination !is VirtualFileImpl &&
        operation.destination.isDirectory
  }

  private fun proceedLocalMoveCopy (
    operation: MoveCopyOperation,
    progressIndicator: ProgressIndicator
  ): Throwable? {
    val sourceFile = operation.source
    val destFile = operation.destination
    val sourceFileAttributes = dataOpsManager.tryToGetAttributes(sourceFile)

    if (sourceFileAttributes is RemoteUssAttributes && sourceFileAttributes.isSymlink) {
      return IllegalArgumentException("Impossible to download symlink. ${sourceFile.name} is symlink to ${sourceFileAttributes.symlinkTarget}." +
          " Please, download ${sourceFileAttributes.symlinkTarget} directly.")
    }

    val contentSynchronizer = dataOpsManager.getContentSynchronizer(sourceFile)
      ?: return IllegalArgumentException("Cannot find synchronizer for file ${sourceFile.name}")
    val syncProvider = DocumentedSyncProvider(sourceFile)
    contentSynchronizer.synchronizeWithRemote(syncProvider, progressIndicator)

    val createdFile = runWriteActionAndWait {
      if (operation.forceOverwriting) {
        destFile.children.filter { it.name === sourceFile.name && !it.isDirectory }.forEach { it.delete(this) }
      }
      destFile.createChildData(this, sourceFile.name)
    }
    runWriteActionInEdtAndWait {
      createdFile.setBinaryContent(syncProvider.retrieveCurrentContent())
    }

    return null
  }

  override fun run(operation: MoveCopyOperation, progressIndicator: ProgressIndicator) {
    var throwable: Throwable?
    try {
      throwable = proceedLocalMoveCopy(operation, progressIndicator)
    } catch (t: Throwable) {
      throwable = t
    }
    if (throwable != null) {
      throw throwable
    }
  }
}
