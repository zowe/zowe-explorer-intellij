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
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.operations.ForceRenameOperation
import org.zowe.explorer.dataops.operations.OperationRunner
import org.zowe.explorer.dataops.operations.OperationRunnerFactory
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.explorer.vfs.sendVfsChangesTopic
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath

/**
 * Factory class for building an instance of the runner class on runtime. Defined in plugin.xml
 */
class ForceRenameOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return ForceRenameOperationRunner(dataOpsManager)
  }
}

/**
 * Base class implementation for running a force rename operation
 */
class ForceRenameOperationRunner(private val dataOpsManager: DataOpsManager) :
  OperationRunner<ForceRenameOperation, Unit> {

  override val operationClass = ForceRenameOperation::class.java
  override val resultClass = Unit::class.java

  /**
   * Determines if an operation can be run on selected object
   * @param operation specifies a force rename operation object
   */
  override fun canRun(operation: ForceRenameOperation): Boolean {
    return (when (operation.attributes) {
      is RemoteUssAttributes -> true
      else -> false
    })
  }

  /**
   * Runs an operation and returns a wrapped instance of the response
   * @param operation specifies a force rename operation object
   * @param progressIndicator
   * @return a wrapped instance of the response
   */
  override fun run(operation: ForceRenameOperation, progressIndicator: ProgressIndicator) {
    val sourceFile = operation.file as MFVirtualFile
    val fileName = sourceFile.filenameInternal
    val attributes = operation.attributes as RemoteUssAttributes
    val parentDirPath = attributes.parentDirPath
    val children = sourceFile.parent?.children
    attributes.requesters.map { requester ->
      try {
        progressIndicator.checkCanceled()
        if (!sourceFile.isDirectory) {
          sourceFile.parent?.let {
            dataOpsManager.performOperation(
              MoveCopyOperation(
                sourceFile,
                sourceFile.parent!!,
                true,
                operation.override,
                operation.newName,
                dataOpsManager,
                operation.explorer
              ), progressIndicator
            )
          }
          sendVfsChangesTopic()
        } else {
          children?.forEach {
            if (it.isDirectory && it.name == operation.newName) {
              if (it.children?.size == 0) {
                val resp = api<DataAPI>(requester.connectionConfig).deleteUssFile(
                  authorizationToken = requester.connectionConfig.authToken,
                  filePath = FilePath("$parentDirPath/${operation.newName}")
                ).cancelByIndicator(progressIndicator).execute()
                if (!resp.isSuccessful) {
                  throw CallException(
                    resp,
                    "Remote exception occurred. Unable to rename source directory $fileName"
                  )
                }
                sourceFile.parent?.let {
                  dataOpsManager.performOperation(
                    MoveCopyOperation(
                      sourceFile,
                      sourceFile.parent!!,
                      true,
                      operation.override,
                      operation.newName,
                      dataOpsManager,
                      operation.explorer
                    ), progressIndicator
                  )
                  sendVfsChangesTopic()
                }
              } else {
                throw RuntimeException(
                  "Can't rename source directory $fileName".plus(". Destination directory is not empty.")
                )
              }
            }
          }
        }
      } catch (e: Throwable) {
        throw RuntimeException(e)
      }
    }
  }
}
