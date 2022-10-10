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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.UnitRemoteQueryImpl
import org.zowe.explorer.dataops.attributes.MFRemoteFileAttributes
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.fetch.LibraryQuery
import org.zowe.explorer.dataops.fetch.UssQuery
import org.zowe.explorer.dataops.operations.OperationRunner
import org.zowe.explorer.dataops.operations.OperationRunnerFactory
import org.zowe.explorer.utils.runWriteActionInEdtAndWait
import org.zowe.explorer.vfs.MFVirtualFile

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
        .getFileFetchProvider<LibraryQuery, RemoteQuery<LibraryQuery, Unit>, VFile>(
          LibraryQuery::class.java, RemoteQuery::class.java, vFileClass
        )
      sourceFileFetchProvider.reload(sourceQuery, progressIndicator)
      return sourceFileFetchProvider.isCacheValid(sourceQuery)
    } else if (attributes is RemoteUssAttributes) {
      val sourceQuery = UnitRemoteQueryImpl(UssQuery(attributes.path), connectionConfig)
      val sourceFileFetchProvider = dataOpsManager
        .getFileFetchProvider<UssQuery, RemoteQuery<UssQuery, Unit>, VFile>(
          UssQuery::class.java, RemoteQuery::class.java, vFileClass
        )
      sourceFileFetchProvider.reload(sourceQuery, progressIndicator)
      return sourceFileFetchProvider.isCacheValid(sourceQuery)
    }
    throw IllegalArgumentException("Children of file ${file.name} cannot be fetched.")
  }

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
        runWriteActionInEdtAndWait {
          if (operation.forceOverwriting) {
            destFile.children.filter { it.name == sourceFile.name && it.isDirectory }.forEach { it.delete(this) }
          }
          createdDir = destFile.createChildDirectory(this, sourceFile.name)
        }
        val createdDirNotNull =
          createdDir ?: return IllegalArgumentException("Cannot create directory ${sourceFile.name}")
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
            operation.explorer?.reportThrowable(it, operation.explorer.nullableProject)
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
      val attributes = dataOpsManager.tryToGetAttributes(operation.source) as MFRemoteFileAttributes<*>
      if (attributes.requesters.isEmpty()) {
        throw IllegalArgumentException("Cannot get system information of file ${operation.source.name}")
      }
      for (requester in attributes.requesters) {
        throwable = proceedLocalMoveCopy(operation, requester.connectionConfig, progressIndicator)
        if (throwable != null) {
          throw throwable
        }
      }
    } catch (t: Throwable) {
      throwable = t
    }
    if (throwable != null) {
      throw throwable
    }
  }
}
