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
import org.zowe.explorer.api.apiWithBytesConverter
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.attributes.toUssAttributes
import org.zowe.explorer.dataops.content.synchronizer.DocumentedSyncProvider
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.operations.DeleteOperation
import org.zowe.explorer.dataops.operations.OperationRunner
import org.zowe.explorer.dataops.operations.OperationRunnerFactory
import org.zowe.explorer.utils.*
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath
import org.zowe.kotlinsdk.XIBMDataType

/**
 * Factory for registering CrossSystemMemberOrUssFileToUssDirMover in Intellij IoC container.
 * @see CrossSystemMemberOrUssFileOrSequentialToUssDirMover
 * @author Valiantsin Krus
 */
class CrossSystemMemberOrUssFileOrSequentialToUssDirMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return CrossSystemMemberOrUssFileOrSequentialToUssDirMover(dataOpsManager)
  }
}

/**
 * Implements copying of member or uss file to uss directory between different systems.
 * @author Valiantsin Krus
 */
class CrossSystemMemberOrUssFileOrSequentialToUssDirMover(val dataOpsManager: DataOpsManager) : AbstractFileMover() {

  /**
   * Checks that source is member or uss file, dest is uss directory, and they are located inside different systems.
   * @see OperationRunner.canRun
   */
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return !operation.source.isDirectory &&
      operation.destination.isDirectory &&
      (operation.sourceAttributes is RemoteMemberAttributes
          || operation.sourceAttributes is RemoteUssAttributes
          || operation.sourceAttributes is RemoteDatasetAttributes) &&
      operation.destinationAttributes is RemoteUssAttributes &&
      operation.source is MFVirtualFile &&
      operation.destination is MFVirtualFile &&
      operation.commonUrls(dataOpsManager).isEmpty()
  }

  override val log = log<CrossSystemMemberOrUssFileOrSequentialToUssDirMover>()

  /**
   * Proceeds move/copy of member or uss file to uss directory between different systems.
   * @param op requested operation.
   * @param progressIndicator indicator that will show progress of copying/moving in UI.
   */
  private fun proceedCrossSystemMoveCopy(op: MoveCopyOperation, progressIndicator: ProgressIndicator): Throwable? {
    val sourceAttributes = op.sourceAttributes
    val destAttributes = op.destinationAttributes.toUssAttributes(op.destination.name)
    val destConnectionConfig = destAttributes.requesters.firstOrNull()?.connectionConfig
      ?: return IllegalArgumentException("Cannot find connection configuration for file \"${op.destination.name}\"")

    if (sourceAttributes is RemoteUssAttributes && sourceAttributes.isSymlink) {
      return IllegalArgumentException(
        "Impossible to move symlink. ${op.source.name} is symlink to ${sourceAttributes.symlinkTarget}." +
          " Please, move ${sourceAttributes.symlinkTarget} directly."
      )
    }

    val contentSynchronizer = dataOpsManager.getContentSynchronizer(op.source)
      ?: return IllegalArgumentException("Cannot find synchronizer for file ${op.source.name}")
    val syncProvider = DocumentedSyncProvider(op.source)
    contentSynchronizer.synchronizeWithRemote(syncProvider, progressIndicator)

    val contentMode = XIBMDataType(XIBMDataType.Type.BINARY)

    val newName = op.newName ?: op.source.name
    val pathToFile = destAttributes.path + "/" + newName
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
      throw CallException(response, "Cannot upload data to ${op.destination.path}${newName}")
    } else {
      setUssFileTag(op.source.charset.name(), pathToFile, destConnectionConfig)
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
      log.info("Trying to move USS file ${operation.source.name} to USS directory ${operation.destination.path}")
      proceedCrossSystemMoveCopy(operation, progressIndicator)
    } catch (t: Throwable) {
      t
    }
    if (throwable != null) {
      log.info("Failed to move USS file")
      throw throwable
    }
    log.info("USS file has been moved successfully")
  }
}
