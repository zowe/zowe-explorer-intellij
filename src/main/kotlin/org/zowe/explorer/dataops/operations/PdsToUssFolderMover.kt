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

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.UnitRemoteQueryImpl
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.fetch.LibraryQuery
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.vfs.MFVirtualFile
import eu.ibagroup.r2z.*
import retrofit2.Response

class PdsToUssFolderMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return PdsToUssFolderMover(dataOpsManager, MFVirtualFile::class.java)
  }
}

class PdsToUssFolderMover<VFile : VirtualFile>(
  private val dataOpsManager: DataOpsManager,
  val vFileClass: Class<out VFile>
) : AbstractFileMover() {
  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.sourceAttributes is RemoteDatasetAttributes
        && operation.destinationAttributes is RemoteUssAttributes
        && operation.sourceAttributes.isDirectory
        && operation.destinationAttributes.isDirectory
        && operation.commonUrls(dataOpsManager).isNotEmpty()
  }

  private fun rollback(
    prevResponse: Response<Void>? = null,
    sourceName: String,
    destinationPath: String,
    connectionConfig: ConnectionConfig,
    operation: MoveCopyOperation,
    progressIndicator: ProgressIndicator
  ): Throwable? {
    val opName = if (operation.isMove) "move" else "copy"
    var throwable: Throwable? = null
    val isCanceled = progressIndicator.isCanceled
    if (!isCanceled) {
      val msg = "Cannot $opName to $destinationPath."
      throwable = if (prevResponse == null) Exception(msg) else CallException(prevResponse, msg)
    }
    progressIndicator.text = "Attempt to rollback"
    val responseRollback = api<DataAPI>(connectionConfig).deleteUssFile(
      connectionConfig.authToken,
      destinationPath.substring(1),
      XIBMOption.RECURSIVE
    ).execute()

    if (isCanceled && !responseRollback.isSuccessful) {
      throwable = CallException(responseRollback, "Cannot rollback $opName $sourceName to $sourceName.")
    } else if (!isCanceled && responseRollback.isSuccessful) {
      val msg = "Cannot $opName $sourceName to $destinationPath. Rollback proceeded successfully."
      throwable = if (prevResponse == null) Exception() else CallException(prevResponse, msg)
    } else if (!isCanceled && !responseRollback.isSuccessful) {
      throwable = CallException(responseRollback, "Cannot $opName $sourceName to ${destinationPath}. Rollback failed.")
    }
    return throwable
  }

  private fun proceedCopyCutPds(
    connectionConfig: ConnectionConfig,
    operation: MoveCopyOperation,
    progressIndicator: ProgressIndicator
  ): Throwable? {
    val sourceAttributes = (operation.sourceAttributes as RemoteDatasetAttributes)
    val destinationAttributes = (operation.destinationAttributes as RemoteUssAttributes)

    val sourceQuery = UnitRemoteQueryImpl(LibraryQuery(operation.source as MFVirtualFile), connectionConfig)

    val sourceFileFetchProvider = dataOpsManager
      .getFileFetchProvider<LibraryQuery, RemoteQuery<LibraryQuery, Unit>, VFile>(
        LibraryQuery::class.java, RemoteQuery::class.java, vFileClass
      )

    sourceFileFetchProvider.reload(sourceQuery)
    val destinationPath = "${destinationAttributes.path}/${sourceAttributes.name}"
    var throwable: Throwable? = null

    if (sourceFileFetchProvider.isCacheValid(sourceQuery)) {
      val response = api<DataAPI>(connectionConfig).createUssFile(
        connectionConfig.authToken,
        FilePath(destinationAttributes.path + "/" + sourceAttributes.name),
        CreateUssFile(FileType.DIR, destinationAttributes.fileMode ?: FileMode(7,7,7))
      ).cancelByIndicator(progressIndicator).execute()

      if (response.isSuccessful) {
        val cachedChildren = sourceFileFetchProvider.getCached(sourceQuery)

        cachedChildren?.forEach {
          var responseCopyMember: Response<Void>? = null
          runCatching {
            responseCopyMember = api<DataAPI>(connectionConfig).copyDatasetOrMemberToUss(
              connectionConfig.authToken,
              XIBMBpxkAutoCvt.OFF,
              CopyDataUSS.CopyFromDataset(
                from = CopyDataUSS.CopyFromDataset.Dataset(sourceAttributes.name, it.name.toUpperCase())
              ),
              FilePath(destinationPath)
            ).cancelByIndicator(progressIndicator).execute()
          }
          if (progressIndicator.isCanceled || responseCopyMember?.isSuccessful != true) {
            throwable = rollback(
              responseCopyMember,
              "${sourceAttributes.name}(${it.name})",
              destinationPath,
              connectionConfig,
              operation,
              progressIndicator
            )
          }
          progressIndicator.checkCanceled()
        }
      }
    }

    if (operation.isMove) {
      runCatching {
        dataOpsManager.performOperation(DeleteOperation(operation.source, sourceAttributes))
      }
    }

    return throwable
  }

  override fun run(operation: MoveCopyOperation, progressIndicator: ProgressIndicator) {
    var throwable: Throwable? = null
    for ((requester, _) in operation.commonUrls(dataOpsManager)) {
      try {
        throwable = proceedCopyCutPds(requester.connectionConfig, operation, progressIndicator)
        break
      } catch (t: Throwable) {
        throwable = t
      }
    }
    if (throwable != null) {
      throw throwable
    }
  }
}
