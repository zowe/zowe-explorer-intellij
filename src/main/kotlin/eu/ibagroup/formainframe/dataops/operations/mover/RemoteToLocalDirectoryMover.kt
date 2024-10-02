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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.MFRemoteFileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.fetch.LibraryQuery
import eu.ibagroup.formainframe.dataops.fetch.UssQuery
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.dataops.operations.OperationRunnerFactory
import eu.ibagroup.formainframe.telemetry.NotificationsService
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.utils.runWriteActionInEdtAndWait
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/**
 * Factory for registering RemoteToLocalDirectoryMover in Intellij IoC container.
 * @see RemoteToLocalDirectoryMover
 * @author Valiantsin Krus
 */
class RemoteToLocalDirectoryMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return RemoteToLocalDirectoryMover(dataOpsManager, MFVirtualFile::class.java)
  }
}

/**
 * Implements copying (downloading) of remote uss directory to local file system.
 * @author Valiantsin Krus
 */
class RemoteToLocalDirectoryMover<VFile : VirtualFile>(
  val dataOpsManager: DataOpsManager,
  val vFileClass: Class<out VFile>
) : AbstractFileMover() {

  /**
   * Checks that source is remote uss directory and destination is local directory.
   * @see OperationRunner.canRun
   */
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.source.isDirectory &&
      operation.source is MFVirtualFile &&
      operation.destination is VirtualDirectoryImpl &&
      operation.destination.isDirectory
  }

  /**
   * Fetches all children of remote directory to download (copy to local file system) each of them.
   * @param file source directory.
   * @param connectionConfig connection configuration of the system on which the source directory is located.
   * @param progressIndicator indicator that will show progress of downloading in UI.
   */
  private fun fetchRemoteChildren(
    file: VirtualFile,
    connectionConfig: ConnectionConfig,
    progressIndicator: ProgressIndicator
  ): Boolean {
    val attributes = dataOpsManager.tryToGetAttributes(file)
      ?: throw IllegalArgumentException("Cannot attributes for file ${file.name}.")
    if (!file.isDirectory) {
      throw IllegalArgumentException("File ${file.name} is not a directory.")
    }
    if (attributes is RemoteDatasetAttributes) {
      val sourceQuery = UnitRemoteQueryImpl(LibraryQuery(file as MFVirtualFile), connectionConfig)
      val sourceFileFetchProvider = dataOpsManager
        .getFileFetchProvider<LibraryQuery, RemoteQuery<ConnectionConfig, LibraryQuery, Unit>, VFile>(
          LibraryQuery::class.java, RemoteQuery::class.java, vFileClass
        )
      sourceFileFetchProvider.reload(sourceQuery, progressIndicator)
      return sourceFileFetchProvider.isCacheValid(sourceQuery)
    } else if (attributes is RemoteUssAttributes) {
      val sourceQuery = UnitRemoteQueryImpl(UssQuery(attributes.path), connectionConfig)
      val sourceFileFetchProvider = dataOpsManager
        .getFileFetchProvider<UssQuery, RemoteQuery<ConnectionConfig, UssQuery, Unit>, VFile>(
          UssQuery::class.java, RemoteQuery::class.java, vFileClass
        )
      sourceFileFetchProvider.reload(sourceQuery, progressIndicator)
      return sourceFileFetchProvider.isCacheValid(sourceQuery)
    }
    throw IllegalArgumentException("Children of file ${file.name} cannot be fetched.")
  }

  override val log = log<RemoteToLocalDirectoryMover<VFile>>()

  /**
   * Proceeds download of remote uss directory to local file system.
   * @param operation requested operation.
   * @param connectionConfig connection configuration of the system on which the source directory is located.
   * @param progressIndicator indicator that will show progress of copying/moving in UI.
   */
  private fun proceedLocalMoveCopy(
    operation: MoveCopyOperation,
    connectionConfig: ConnectionConfig,
    progressIndicator: ProgressIndicator
  ): Throwable? {
    val sourceFile = operation.source
    val destFile = operation.destination
    try {
      if (fetchRemoteChildren(sourceFile, connectionConfig, progressIndicator)) {
        var createdDir: VirtualFile? = null
        val newName = operation.newName ?: sourceFile.name
        runWriteActionInEdtAndWait {
          if (operation.forceOverwriting) {
            destFile.children.filter { it.name == newName && it.isDirectory }.forEach { it.delete(this) }
          }
          createdDir = destFile.createChildDirectory(this, newName)
        }
        val createdDirNotNull =
          createdDir ?: return IllegalArgumentException("Cannot create directory $newName")
        sourceFile.children?.forEach {
          runCatching {
            dataOpsManager.performOperation(
              MoveCopyOperation(
                it,
                createdDirNotNull,
                isMove = false,
                forceOverwriting = true,
                newName = null,
                dataOpsManager = dataOpsManager,
                explorer = operation.explorer
              ),
              progressIndicator
            )
          }.onFailure {
            NotificationsService.errorNotification(it, operation.explorer?.nullableProject)
          }
        }
      }
    } catch (t: Throwable) {
      return t
    }
    return null
  }

  /**
   * Starts operation execution. Throws throwable if something went wrong.
   * @throws Throwable
   * @see OperationRunner.run
   */
  override fun run(operation: MoveCopyOperation, progressIndicator: ProgressIndicator) {
    var throwable: Throwable? = null
    try {
      log.info("Trying to move remote file ${operation.source.name} to local directory ${operation.destination.path}")
      val attributes = dataOpsManager.tryToGetAttributes(operation.source) as MFRemoteFileAttributes<*, *>
      if (attributes.requesters.isEmpty()) {
        throw IllegalArgumentException("Cannot get system information of file ${operation.source.name}")
      }
      for (requester in attributes.requesters) {
        val connectionConfig = requester.connectionConfig as ConnectionConfig
        throwable = proceedLocalMoveCopy(operation, connectionConfig, progressIndicator)
        if (throwable != null) {
          throw throwable
        }
      }
    } catch (t: Throwable) {
      throwable = t
    }
    if (throwable != null) {
      log.info("Failed to move remote file")
      throw throwable
    }
    log.info("Remote ile has been moved successfully")
  }
}
