/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package eu.ibagroup.formainframe.dataops.operations.mover

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileSystemEntry
import com.intellij.util.LineSeparator
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.dataops.operations.OperationRunnerFactory
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import org.zowe.kotlinsdk.XIBMDataType
import java.io.File
import java.nio.file.Paths

/**
 * Factory for registering RemoteToLocalFileMover in Intellij IoC container.
 * @see RemoteToLocalFileMover
 * @author Valiantsin Krus
 */
class RemoteToLocalFileMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return RemoteToLocalFileMover(dataOpsManager)
  }
}

/**
 * Implements copying (downloading) of remote uss file to local file system.
 * @author Valiantsin Krus
 */
class RemoteToLocalFileMover(val dataOpsManager: DataOpsManager) : AbstractFileMover() {

  /**
   * Checks that source is remote uss file and destination is local directory.
   * @see OperationRunner.canRun
   */
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return !operation.source.isDirectory &&
            operation.source is MFVirtualFile &&
            operation.destination is VirtualFileSystemEntry &&
            operation.destination.isDirectory
  }

  override val log = log<RemoteToLocalFileMover>()

  /**
   * Proceeds download of remote uss file to local file system.
   * @param operation requested operation.
   * @param progressIndicator indicator that will show progress of copying/moving in UI.
   */
  private fun proceedLocalMoveCopy(
    operation: MoveCopyOperation,
    progressIndicator: ProgressIndicator
  ): Throwable? {
    val sourceFile = operation.source
    val destFile = operation.destination
    val newFileName = operation.newName ?: sourceFile.name
    val sourceFileAttributes = dataOpsManager.tryToGetAttributes(sourceFile)
      ?: return IllegalArgumentException("Cannot find attributes for file ${sourceFile.name}")

    if (sourceFileAttributes is RemoteUssAttributes && sourceFileAttributes.isSymlink) {
      return IllegalArgumentException(
        "Impossible to download symlink. ${sourceFile.name} is symlink to ${sourceFileAttributes.symlinkTarget}." +
                " Please, download ${sourceFileAttributes.symlinkTarget} directly."
      )
    }

    val contentSynchronizer = dataOpsManager.getContentSynchronizer(sourceFile)
      ?: return IllegalArgumentException("Cannot find synchronizer for file ${sourceFile.name}")
    val syncProvider = DocumentedSyncProvider(sourceFile)

    if (sourceFile.fileType.isBinary || sourceFileAttributes is RemoteUssAttributes) {
      sourceFileAttributes.contentMode = XIBMDataType(XIBMDataType.Type.BINARY)
    } else {
      sourceFileAttributes.contentMode = XIBMDataType(XIBMDataType.Type.TEXT)
    }
    contentSynchronizer.synchronizeWithRemote(syncProvider, progressIndicator)
    val sourceContent = contentSynchronizer.successfulContentStorage(syncProvider)

    val createdFileJava = Paths.get(destFile.path, newFileName).toFile().apply { createNewFile() }
    if (!sourceFile.fileType.isBinary) {
      setCreatedFileParams(createdFileJava, sourceFile)
    }
    createdFileJava.writeBytes(sourceContent)
    runInEdtAndWait {
      destFile.refresh(false, false)
    }
    return null
  }

  /**
   * Sets parameters (charset, line separator) for created file to be the same as the parameters of source file.
   * @param createdFileJava created java file
   * @param sourceFile source virtual file
   */
  private fun setCreatedFileParams(createdFileJava: File, sourceFile: VirtualFile) {
    val createdVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(createdFileJava)
    createdVirtualFile?.let {
      val lineSeparator = sourceFile.detectedLineSeparator ?: LineSeparator.LF.separatorString
      it.detectedLineSeparator = lineSeparator
      runWriteActionInEdtAndWait { changeEncodingTo(it, sourceFile.charset) }
    }
  }

  /**
   * Starts operation execution. Throws throwable if something went wrong.
   * @throws Throwable
   * @see OperationRunner.run
   */
  override fun run(operation: MoveCopyOperation, progressIndicator: ProgressIndicator) {
    var throwable: Throwable?
    try {
      log.info("Trying to move remote file ${operation.source.name} to local file ${operation.destination.name}")
      throwable = proceedLocalMoveCopy(operation, progressIndicator)
    } catch (t: Throwable) {
      throwable = t
    }
    if (throwable != null) {
      log.info("Failed to move remote file")
      throw throwable
    }
    log.info("Remote file has been moved successfully")
  }
}
