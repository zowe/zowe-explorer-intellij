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
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.fetch.UssQuery
import eu.ibagroup.formainframe.dataops.operations.DeleteOperation
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.dataops.operations.OperationRunnerFactory
import eu.ibagroup.formainframe.utils.applyIfNotNull
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.runWriteActionInEdtAndWait
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.*

class CrossSystemUssDirMoverFactory: OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return CrossSystemUssDirMover(dataOpsManager)
  }
}

class CrossSystemUssDirMover(val dataOpsManager: DataOpsManager): AbstractFileMover() {

  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.source.isDirectory &&
        operation.sourceAttributes is RemoteUssAttributes &&
        operation.destination.isDirectory &&
        operation.destination is MFVirtualFile &&
        dataOpsManager.tryToGetAttributes(operation.destination) is RemoteUssAttributes
  }

  fun proceedDirMove (operation: MoveCopyOperation, progressIndicator: ProgressIndicator): Throwable? {
    val sourceFile = operation.source
    val destFile = operation.destination
    val destAttributes = operation.destinationAttributes.castOrNull<RemoteUssAttributes>()
      ?: return IllegalStateException("No attributes found for destination directory \'${destFile.name}\'.")

    val destConnectionConfig = destAttributes.requesters.map { it.connectionConfig }.firstOrNull()
      ?: return IllegalStateException("No connection for destination directory \'${destAttributes.path}\' found.")


    if (sourceFile is MFVirtualFile) {
      val sourceAttributes = operation.sourceAttributes.castOrNull<RemoteUssAttributes>()
        ?: return IllegalStateException("No attributes found for destination directory \'${destFile.name}\'.")
      val sourceConnectionConfig = sourceAttributes.requesters.map { it.connectionConfig }.firstOrNull()
        ?: return IllegalStateException("No connection for source directory \'${sourceAttributes.path}\' found.")

      val sourceQuery = UnitRemoteQueryImpl(UssQuery(sourceAttributes.path), sourceConnectionConfig)
      val sourceFileFetchProvider = dataOpsManager
        .getFileFetchProvider<UssQuery, RemoteQuery<UssQuery, Unit>, MFVirtualFile>(
          UssQuery::class.java, RemoteQuery::class.java, MFVirtualFile::class.java
        )
      sourceFileFetchProvider.reload(sourceQuery)
    }

    val pathToDir = destAttributes.path + "/" + sourceFile.name

    if (operation.forceOverwriting) {
      destFile.children.firstOrNull { it.name == sourceFile.name }?.let {
        dataOpsManager.performOperation(DeleteOperation(it, dataOpsManager), progressIndicator)
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
        dataOpsManager.performOperation(op, progressIndicator)
      }
    }

    if (sourceFile is MFVirtualFile && operation.isMove) {
      dataOpsManager.performOperation(DeleteOperation(sourceFile, dataOpsManager), progressIndicator)
    }

    return null
  }

  override fun run(operation: MoveCopyOperation, progressIndicator: ProgressIndicator) {
    val throwable: Throwable? = try {
      proceedDirMove(operation, progressIndicator)
    } catch (t: Throwable) {
      t
    }
    if (throwable != null) {
      throw throwable
    }
  }
}