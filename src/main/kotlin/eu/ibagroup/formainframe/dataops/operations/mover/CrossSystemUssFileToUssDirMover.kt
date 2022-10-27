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
import eu.ibagroup.formainframe.api.apiWithBytesConverter
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.operations.DeleteOperation
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.dataops.operations.OperationRunnerFactory
import eu.ibagroup.formainframe.utils.applyIfNotNull
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.FilePath
import eu.ibagroup.r2z.XIBMDataType

/**
 * Factory for registering CrossSystemUssFileToUssDirMover in Intellij IoC container.
 * @see CrossSystemUssFileToUssDirMover
 * @author Valiantsin Krus
 */
class CrossSystemUssFileToUssDirMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return CrossSystemUssFileToUssDirMover(dataOpsManager)
  }
}

/**
 * Implements copying of uss file to uss directory between different systems.
 * @author Valiantsin Krus
 */
class CrossSystemUssFileToUssDirMover(val dataOpsManager: DataOpsManager) : AbstractFileMover() {

  /**
   * Checks that source is uss file, dest is uss directory, and they are located inside different systems.
   * @see OperationRunner.canRun
   */
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return !operation.source.isDirectory &&
      operation.destination.isDirectory &&
      operation.destinationAttributes is RemoteUssAttributes &&
      operation.source is MFVirtualFile &&
      operation.destination is MFVirtualFile &&
      operation.commonUrls(dataOpsManager).isEmpty()
  }

  /**
   * Util function to cast FileAttributes to RemoteUssAttributes or throw exception.
   * @throws IllegalArgumentException
   * @return source attributes that was cast to RemoteUssAttributes.
   */
  private fun FileAttributes?.toUssAttributes(fileName: String): RemoteUssAttributes {
    return castOrNull<RemoteUssAttributes>()
      ?: throw IllegalArgumentException("Cannot find attributes for file \"${fileName}\"")
  }

  /**
   * Proceeds move/copy of uss file to uss directory between different systems.
   * @param op requested operation.
   * @param progressIndicator indicator that will show progress of copying/moving in UI.
   */
  private fun proceedCrossSystemMoveCopy(op: MoveCopyOperation, progressIndicator: ProgressIndicator): Throwable? {
    val sourceAttributes = op.sourceAttributes.toUssAttributes(op.source.name)
    val destAttributes = op.destinationAttributes.toUssAttributes(op.destination.name)
    val destConnectionConfig = destAttributes.requesters.firstOrNull()?.connectionConfig
      ?: return IllegalArgumentException("Cannot find connection configuration for file \"${op.destination.name}\"")

    if (sourceAttributes.isSymlink) {
      return IllegalArgumentException(
        "Impossible to move symlink. ${op.source.name} is symlink to ${sourceAttributes.symlinkTarget}." +
          " Please, move ${sourceAttributes.symlinkTarget} directly."
      )
    }

    val contentSynchronizer = dataOpsManager.getContentSynchronizer(op.source)
      ?: return IllegalArgumentException("Cannot find synchronizer for file ${op.source.name}")
    val syncProvider = DocumentedSyncProvider(op.source)
    contentSynchronizer.synchronizeWithRemote(syncProvider, progressIndicator)

    val contentMode =
      if (op.source.fileType.isBinary) XIBMDataType(XIBMDataType.Type.BINARY) else sourceAttributes.contentMode
    val pathToFile = destAttributes.path + "/" + op.source.name
    progressIndicator.text = "Uploading file '$pathToFile'"
    val response = apiWithBytesConverter<DataAPI>(destConnectionConfig).writeToUssFile(
      authorizationToken = destConnectionConfig.authToken,
      filePath = FilePath(pathToFile),
      body = op.source.contentsToByteArray(),
      xIBMDataType = contentMode
    ).applyIfNotNull(progressIndicator) { indicator ->
      cancelByIndicator(indicator)
    }.execute()

    if (!response.isSuccessful) {
      throw CallException(response, "Cannot upload data to ${op.destination.path}${op.source.name}")
    } else {
      if (op.isMove) {
        dataOpsManager.performOperation(DeleteOperation(op.source, dataOpsManager))
      }
    }

    return null
  }

  /**
   * Starts operation execution. Throws throwable if something went wrong.
   * @throws Throwable
   * @see OperationRunner.run
   */
  override fun run(operation: MoveCopyOperation, progressIndicator: ProgressIndicator) {
    val throwable: Throwable? = try {
      proceedCrossSystemMoveCopy(operation, progressIndicator)
    } catch (t: Throwable) {
      t
    }
    if (throwable != null) {
      throw throwable
    }
  }
}
