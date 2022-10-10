/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package org.zowe.explorer.dataops.operations.mover

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileSystemEntry
import org.zowe.explorer.api.apiWithBytesConverter
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.content.synchronizer.DocumentedSyncProvider
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.operations.OperationRunner
import org.zowe.explorer.dataops.operations.OperationRunnerFactory
import org.zowe.explorer.utils.applyIfNotNull
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.utils.runWriteActionInEdtAndWait
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath
import org.zowe.kotlinsdk.XIBMDataType

/**
 * Factory for registering LocalFileToUssDirMover in Intellij IoC container.
 * @see LocalFileToUssDirMover
 * @author Valiantsin Krus
 */
class LocalFileToUssDirMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return LocalFileToUssDirMover(dataOpsManager)
  }
}

/**
 * Implements copying of file from local file system to remote uss directory.
 * @author Valiantsin Krus
 */
class LocalFileToUssDirMover(val dataOpsManager: DataOpsManager) : AbstractFileMover() {

  /**
   * Checks that source is local file, dest is uss directory, and destination
   * file is located on remote system (by fetching its attributes).
   * @see OperationRunner.canRun
   */
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.source is VirtualFileSystemEntry &&
            !operation.source.isDirectory &&
            operation.destination.isDirectory &&
            operation.destination is MFVirtualFile &&
            dataOpsManager.tryToGetAttributes(operation.destination) is RemoteUssAttributes
  }

  /**
   * Proceeds move/copy of file from local file system to remote uss directory.
   * @param operation requested operation.
   * @param progressIndicator indicator that will show progress of copying/moving in UI.
   */
  private fun proceedLocalUpload(
    operation: MoveCopyOperation,
    progressIndicator: ProgressIndicator
  ): Throwable? {
    var throwable: Throwable? = null
    val sourceFile = operation.source
    val destFile = operation.destination
    val destAttributes = operation.destinationAttributes.castOrNull<RemoteUssAttributes>()
      ?: return IllegalStateException("No attributes found for destination directory \'${destFile.name}\'.")

    val destConnectionConfig = destAttributes.requesters.map { it.connectionConfig }.firstOrNull()
      ?: return Throwable("No connection for destination directory found.")

    val pathToFile = destAttributes.path + "/" + sourceFile.name

    val contentToUpload = sourceFile.contentsToByteArray().toMutableList()
    val xIBMDataType =
      if (sourceFile.fileType.isBinary) XIBMDataType(XIBMDataType.Type.BINARY) else XIBMDataType(XIBMDataType.Type.TEXT)


    val response = apiWithBytesConverter<DataAPI>(destConnectionConfig).writeToUssFile(
      authorizationToken = destConnectionConfig.authToken,
      filePath = FilePath(pathToFile).toString(),
      body = contentToUpload.toByteArray(),
      xIBMDataType = xIBMDataType
    ).applyIfNotNull(progressIndicator) { indicator ->
      cancelByIndicator(indicator)
    }.execute()

    if (!response.isSuccessful) {
      throwable = CallException(response, "Cannot upload data to ${destAttributes.path}${sourceFile.name}")
    } else {
      destFile.children.firstOrNull { it.name == sourceFile.name }?.let { file ->
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

  /**
   * Starts operation execution. Throws throwable if something went wrong.
   * @throws Throwable
   * @see OperationRunner.run
   */
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
