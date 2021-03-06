/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.operations

import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileImpl
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.content.synchronizer.DocumentedSyncProvider
import org.zowe.explorer.utils.runWriteActionInEdtAndWait
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.XIBMDataType

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
      ?: return IllegalArgumentException("Cannot find attributes for file ${sourceFile.name}")

    if (sourceFileAttributes is RemoteUssAttributes && sourceFileAttributes.isSymlink) {
      return IllegalArgumentException("Impossible to download symlink. ${sourceFile.name} is symlink to ${sourceFileAttributes.symlinkTarget}." +
          " Please, download ${sourceFileAttributes.symlinkTarget} directly.")
    }

    val contentSynchronizer = dataOpsManager.getContentSynchronizer(sourceFile)
      ?: return IllegalArgumentException("Cannot find synchronizer for file ${sourceFile.name}")
    val syncProvider = DocumentedSyncProvider(sourceFile)

    if (!sourceFile.fileType.isBinary) {
      sourceFileAttributes.contentMode = XIBMDataType(XIBMDataType.Type.TEXT)
    } else {
      sourceFileAttributes.contentMode = XIBMDataType(XIBMDataType.Type.BINARY)
    }
    contentSynchronizer.synchronizeWithRemote(syncProvider, progressIndicator)
    val createdFile = runWriteActionAndWait {
      if (operation.forceOverwriting) {
        destFile.children.filter { it.name === sourceFile.name && !it.isDirectory }.forEach { it.delete(this) }
      }
      destFile.createChildData(this, sourceFile.name)
    }
    runWriteActionInEdtAndWait {
      createdFile.setBinaryContent(sourceFile.contentsToByteArray())
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
