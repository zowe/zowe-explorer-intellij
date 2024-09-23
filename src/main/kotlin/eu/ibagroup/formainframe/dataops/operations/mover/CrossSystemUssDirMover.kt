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
package eu.ibagroup.formainframe.dataops.operations.mover

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
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
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import org.zowe.kotlinsdk.*

/**
 * Factory for registering CrossSystemUssDirMover in Intellij IoC container.
 * @see CrossSystemUssDirMover
 * @author Valiantsin Krus
 */
class CrossSystemUssDirMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return CrossSystemUssDirMover(dataOpsManager)
  }
}

/**
 * Implements copying of uss directory to uss directory between different systems.
 * @author Valiantsin Krus
 */
class CrossSystemUssDirMover(val dataOpsManager: DataOpsManager) : AbstractFileMover() {

  /**
   * Checks that source and dest files are uss directories, and they are located inside different systems.
   * @see OperationRunner.canRun
   */
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.source.isDirectory &&
            operation.sourceAttributes is RemoteUssAttributes &&
            operation.destination.isDirectory &&
            operation.destination is MFVirtualFile &&
            dataOpsManager.tryToGetAttributes(operation.destination) is RemoteUssAttributes &&
            operation.commonUrls(dataOpsManager).isEmpty()
  }

  override val log = log<CrossSystemUssDirMover>()

  /**
   * Proceeds move/copy of uss directory to uss directory between different systems.
   * @param operation requested operation
   * @param progressIndicator indicator that will show progress of copying/moving in UI.
   */
  private fun proceedDirMove(operation: MoveCopyOperation, progressIndicator: ProgressIndicator): Throwable? {
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
        .getFileFetchProvider<UssQuery, RemoteQuery<ConnectionConfig, UssQuery, Unit>, MFVirtualFile>(
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
      body = CreateUssFile(FileType.DIR, FileMode(7, 7, 7))
    ).applyIfNotNull(progressIndicator) {
      cancelByIndicator(it)
    }.execute()

    if (!response.isSuccessful) {
      return CallException(response, "Cannot upload directory '$pathToDir'.")
    }

    val attributesService =
      dataOpsManager.getAttributesService(RemoteUssAttributes::class.java, MFVirtualFile::class.java)
    val createdDirFile = attributesService.getOrCreateVirtualFile(
      RemoteUssAttributes(
        destAttributes.path,
        UssFile(sourceFile.name, "drwxrwxrwx"),
        destConnectionConfig.url,
        destConnectionConfig
      )
    )

    sourceFile.children.forEach {
      val op = MoveCopyOperation(
        it,
        createdDirFile,
        isMove = false,
        forceOverwriting = false,
        newName = null,
        dataOpsManager = dataOpsManager
      )
      dataOpsManager.performOperation(op, progressIndicator)
    }

    if (sourceFile is MFVirtualFile && operation.isMove) {
      dataOpsManager.performOperation(DeleteOperation(sourceFile, dataOpsManager), progressIndicator)
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
      log.info("Trying to move USS directory ${operation.source.name} to ${operation.destination.path}")
      proceedDirMove(operation, progressIndicator)
    } catch (t: Throwable) {
      t
    }
    if (throwable != null) {
      log.info("Failed to move USS directory")
      throw throwable
    }
    log.info("USS directory has been moved successfully")
  }
}
