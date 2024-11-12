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

package org.zowe.explorer.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.UnitOperation
import org.zowe.explorer.dataops.attributes.*
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.findAnyNullable
import org.zowe.explorer.utils.log
import org.zowe.explorer.utils.runWriteActionInEdt
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath
import org.zowe.kotlinsdk.XIBMOption
import retrofit2.Call

class DeleteRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return DeleteOperationRunner(dataOpsManager)
  }
}

/**
 * Send delete operation call and delete the respective element from the virtual file system
 * @param operation the delete operation instance
 * @param opRunner the operation runner
 * @param progressIndicator the progress indicator to cancel the operation by on the call end
 * @param requesters the requesters to perform the operation for
 * @param deleteOperationCallBuilder the operation call builder to build the call
 * @param exceptionMsg the exception message to put in the [CallException] if the operation was not successful
 */
private fun processDeleteForRequesters(
  operation: DeleteOperation,
  opRunner: DeleteOperationRunner,
  progressIndicator: ProgressIndicator,
  requesters: List<Requester<ConnectionConfig>>,
  deleteOperationCallBuilder: (ConnectionConfig) -> Call<Void>,
  exceptionMsg: String = "Cannot delete the element"
) {
  var throwable: Throwable? = null
  requesters
    .stream()
    .map { requester ->
      try {
        progressIndicator.checkCanceled()
        val response = deleteOperationCallBuilder(requester.connectionConfig)
          .cancelByIndicator(progressIndicator)
          .execute()
        if (response.isSuccessful) {
          runWriteActionInEdt { operation.file.delete(opRunner) }
          true
        } else {
          throwable = CallException(response, exceptionMsg)
          false
        }
      } catch (t: Throwable) {
        throwable = t
        false
      }
    }
    .filter { it }
    .findAnyNullable()
    ?: throw (throwable ?: Throwable("Unknown"))
}

class DeleteOperationRunner(
  private val dataOpsManager: DataOpsManager
) : OperationRunner<DeleteOperation, Unit> {
  override val operationClass = DeleteOperation::class.java
  override val log = log<DeleteOperationRunner>()

  /**
   * Run "Delete" operation.
   * Runs the action depending on the type of the element to remove.
   * After the element is removed, removes it from the mainframe virtual file system
   * @param operation the operation instance to get the file, attributes and requesters to delete the file
   * @param progressIndicator the progress indicatior for the operation
   */
  override fun run(
    operation: DeleteOperation,
    progressIndicator: ProgressIndicator
  ) {
    when (val attr = operation.attributes) {
      is RemoteDatasetAttributes -> {
        if (operation.file.children != null) {
          operation.file.children.forEach { it.isWritable = false }
        } else {
          operation.file.isWritable = false
        }
        val deleteOperationCallBuilder = { connectionConfig: ConnectionConfig ->
          api<DataAPI>(connectionConfig).deleteDataset(
            authorizationToken = connectionConfig.authToken,
            datasetName = attr.name
          )
        }
        processDeleteForRequesters(
          operation,
          this,
          progressIndicator,
          attr.requesters,
          deleteOperationCallBuilder,
          "Cannot delete data set"
        )
      }

      is RemoteMemberAttributes -> {
        operation.file.isWritable = false
        val libraryAttributes = attr.getLibraryAttributes(dataOpsManager)
        if (libraryAttributes != null) {
          val deleteOperationCallBuilder = { connectionConfig: ConnectionConfig ->
            api<DataAPI>(connectionConfig).deleteDatasetMember(
              authorizationToken = connectionConfig.authToken,
              datasetName = libraryAttributes.name,
              memberName = attr.name
            )
          }
          processDeleteForRequesters(
            operation,
            this,
            progressIndicator,
            libraryAttributes.requesters,
            deleteOperationCallBuilder,
            "Cannot delete data set member"
          )
        }
      }

      is RemoteUssAttributes -> {
        if (operation.file.isDirectory) {
          operation.file.children.forEach { it.isWritable = false }
        } else {
          operation.file.isWritable = false
        }
        val deleteOperationCallBuilder = { connectionConfig: ConnectionConfig ->
          api<DataAPI>(connectionConfig).deleteUssFile(
            authorizationToken = connectionConfig.authToken,
            filePath = FilePath(attr.path),
            xIBMOption = XIBMOption.RECURSIVE
          )
        }
        processDeleteForRequesters(
          operation,
          this,
          progressIndicator,
          attr.requesters,
          deleteOperationCallBuilder,
          "Cannot delete USS File/Directory"
        )
      }
    }
  }

  override val resultClass = Unit::class.java

  override fun canRun(operation: DeleteOperation): Boolean {
    val attr = operation.attributes
    return !(attr is RemoteDatasetAttributes && !attr.hasDsOrg)
  }

}

data class DeleteOperation(
  val file: VirtualFile,
  val attributes: FileAttributes
) : UnitOperation {
  constructor(file: VirtualFile, dataOpsManager: DataOpsManager) : this(
    file = file,
    attributes = dataOpsManager.tryToGetAttributes(file)
      ?: throw IllegalArgumentException("Deleting file should have attributes")
  )
}
