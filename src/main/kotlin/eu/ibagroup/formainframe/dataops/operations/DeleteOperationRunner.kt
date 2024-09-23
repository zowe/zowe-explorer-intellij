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

package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.UnitOperation
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.attributes.getLibraryAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.findAnyNullable
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.utils.runWriteActionInEdt
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath
import org.zowe.kotlinsdk.XIBMOption

class DeleteRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return DeleteOperationRunner(dataOpsManager)
  }
}

class DeleteOperationRunner(private val dataOpsManager: DataOpsManager) :
  OperationRunner<DeleteOperation, Unit> {
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
        AnalyticsService.getService().trackAnalyticsEvent(FileEvent(attr, FileAction.DELETE))

        if (operation.file.children != null) {
          operation.file.children.forEach { it.isWritable = false }
        } else {
          operation.file.isWritable = false
        }
        var throwable: Throwable? = null
        attr.requesters.stream().map {
          try {
            progressIndicator.checkCanceled()
            val response = api<DataAPI>(it.connectionConfig).deleteDataset(
              authorizationToken = it.connectionConfig.authToken,
              datasetName = attr.name
            ).cancelByIndicator(progressIndicator).execute()
            if (response.isSuccessful) {
              runWriteActionInEdt { operation.file.delete(this@DeleteOperationRunner) }
              true
            } else {
              throwable = CallException(response, "Cannot delete data set")
              false
            }
          } catch (t: Throwable) {
            throwable = t
            false
          }
        }.filter { it }.findAnyNullable() ?: throw (throwable ?: Throwable("Unknown"))
      }

      is RemoteMemberAttributes -> {
        AnalyticsService.getService().trackAnalyticsEvent(FileEvent(attr, FileAction.DELETE))

        operation.file.isWritable = false
        val libraryAttributes = attr.getLibraryAttributes(dataOpsManager)
        if (libraryAttributes != null) {
          var throwable: Throwable? = null
          libraryAttributes.requesters.stream().map {
            try {
              progressIndicator.checkCanceled()
              val response = api<DataAPI>(it.connectionConfig).deleteDatasetMember(
                authorizationToken = it.connectionConfig.authToken,
                datasetName = libraryAttributes.name,
                memberName = attr.name
              ).cancelByIndicator(progressIndicator).execute()
              if (response.isSuccessful) {
                runWriteActionInEdt { operation.file.delete(this@DeleteOperationRunner) }
                true
              } else {
                throwable = CallException(response, "Cannot delete data set member")
                false
              }
            } catch (t: Throwable) {
              throwable = t
              false
            }
          }.filter { it }.findAnyNullable() ?: throw (throwable ?: Throwable("Unknown"))
        }
      }

      is RemoteUssAttributes -> {
        AnalyticsService.getService().trackAnalyticsEvent(FileEvent(attr, FileAction.DELETE))

        if (operation.file.isDirectory) {
          operation.file.children.forEach { it.isWritable = false }
        } else {
          operation.file.isWritable = false
        }
        var throwable: Throwable? = null
        attr.requesters.stream().map {
          try {
            progressIndicator.checkCanceled()
            val response = api<DataAPI>(it.connectionConfig).deleteUssFile(
              authorizationToken = it.connectionConfig.authToken,
              filePath = FilePath(attr.path),
              xIBMOption = XIBMOption.RECURSIVE
            ).cancelByIndicator(progressIndicator).execute()
            if (response.isSuccessful) {
              // TODO: clarify issue with removing from MF Virtual file system
              // runWriteActionInEdt { operation.file.delete(this@DeleteOperationRunner) }
              true
            } else {
              throwable = CallException(response, "Cannot delete USS File/Directory")
              false
            }
          } catch (t: Throwable) {
            throwable = t
            false
          }
        }.filter { it }.findAnyNullable() ?: throw (throwable ?: Throwable("Unknown"))
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
