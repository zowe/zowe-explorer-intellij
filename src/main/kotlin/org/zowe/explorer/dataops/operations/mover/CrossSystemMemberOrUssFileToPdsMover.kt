/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */
package org.zowe.explorer.dataops.operations.mover

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.apiWithBytesConverter
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.content.synchronizer.DEFAULT_TEXT_CHARSET
import org.zowe.explorer.dataops.content.synchronizer.DocumentedSyncProvider
import org.zowe.explorer.dataops.content.synchronizer.addNewLine
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.operations.DeleteOperation
import org.zowe.explorer.dataops.operations.OperationRunner
import org.zowe.explorer.dataops.operations.OperationRunnerFactory
import org.zowe.explorer.utils.*
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.XIBMDataType

/**
 * Factory for registering CrossSystemMemberOrUssFileToPdsMover in Intellij IoC container.
 * @see CrossSystemMemberOrUssFileToPdsMover
 * @author Valiantsin Krus
 */
class CrossSystemMemberOrUssFileToPdsMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return CrossSystemMemberOrUssFileToPdsMover(dataOpsManager)
  }
}

/**
 * Implements copying of member or uss file to partitioned data set between different systems.
 * @author Valiantsin Krus
 */
class CrossSystemMemberOrUssFileToPdsMover(val dataOpsManager: DataOpsManager) : AbstractFileMover() {

  /**
   * Checks that source is member or uss file, dest is partitioned data set, and they are located inside different systems.
   * @see OperationRunner.canRun
   */
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return !operation.source.isDirectory &&
        operation.destination.isDirectory &&
        operation.destinationAttributes is RemoteDatasetAttributes &&
        operation.destination is MFVirtualFile &&
        (operation.source !is MFVirtualFile || operation.commonUrls(dataOpsManager).isEmpty())
  }

  override val log = log<CrossSystemMemberOrUssFileToPdsMover>()

  /**
   * Proceeds move/copy of member or uss file to partitioned data set between different systems.
   * @param operation requested operation.
   * @param progressIndicator indicator that will show progress of copying/moving in UI.
   */
  private fun proceedCrossSystemMoveCopy(
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

    if (sourceFile is MFVirtualFile) {
      val contentSynchronizer = dataOpsManager.getContentSynchronizer(sourceFile)
      val syncProvider = DocumentedSyncProvider(sourceFile)
      contentSynchronizer?.synchronizeWithRemote(syncProvider, progressIndicator)
    }

    var memberName = operation.newName ?: dataOpsManager.getNameResolver(sourceFile, destFile).resolve(sourceFile, listOf(sourceFile), destFile)
    if (memberName.isEmpty()) {
      memberName = "empty"
    }

    val xIBMDataType = if (sourceFile.fileType.isBinary) XIBMDataType(XIBMDataType.Type.BINARY)
    else XIBMDataType(XIBMDataType.Type.TEXT)

    val sourceContent = sourceFile.contentsToByteArray()
    val contentToUpload =
      if (sourceFile.fileType.isBinary) sourceContent
      else sourceContent.toString(sourceFile.charset).replace("\r\n", "\n")
        .toByteArray(DEFAULT_TEXT_CHARSET).addNewLine()

    val response = apiWithBytesConverter<DataAPI>(destConnectionConfig).writeToDatasetMember(
      authorizationToken = destConnectionConfig.authToken,
      datasetName = destAttributes.name,
      memberName = memberName,
      content = contentToUpload,
      xIBMDataType = xIBMDataType
    ).applyIfNotNull(progressIndicator) { indicator ->
      cancelByIndicator(indicator)
    }.execute()

    if (!response.isSuccessful &&
      response.errorBody()?.string()?.contains("Truncation of a record occurred during an I/O operation.") != true
    ) {
      throwable = CallException(response, "Cannot upload data to '${destAttributes.name}(${memberName})'")
    } else {
      destFile.children.firstOrNull { it.name.uppercase() == memberName.uppercase() }?.let { file ->
        val syncProvider = DocumentedSyncProvider(file, { _, _, _ -> false }, { th -> throwable = th })
        val contentSynchronizer = dataOpsManager.getContentSynchronizer(file)

        if (contentSynchronizer == null) {
          throwable = IllegalArgumentException("Cannot get content synchronizer for file '${file.name}'")
        } else {
          contentSynchronizer.synchronizeWithRemote(syncProvider, progressIndicator)
        }
      }
      if (operation.isMove) {
        log.info("Trying to delete source file")
        val sourceAttributes = operation.sourceAttributes
        runCatching {
          if (sourceAttributes != null) {
            dataOpsManager.performOperation(DeleteOperation(operation.source, sourceAttributes))
          }
        }
          .onFailure { t ->
            log.warn("Can't delete source file $sourceFile")
            val rollbackResponse = apiWithBytesConverter<DataAPI>(destConnectionConfig).deleteDatasetMember(
              authorizationToken = destConnectionConfig.authToken,
              datasetName = destAttributes.name,
              memberName = memberName
            ).execute()
            if (!rollbackResponse.isSuccessful) {
              log.warn("Cannot delete ${destAttributes.name}. Rollback failed.")
            }
            throwable = t
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
    val throwable: Throwable? = try {
      log.info("Trying to move USS file ${operation.source.name} to PDS ${operation.destination.name}")
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
