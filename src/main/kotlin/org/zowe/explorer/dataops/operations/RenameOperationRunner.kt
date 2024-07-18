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
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.explorer.actions.DuplicateMemberAction
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.explorer.utils.runWriteActionInEdtAndWait
import org.zowe.kotlinsdk.CopyDataZOS
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath
import org.zowe.kotlinsdk.MoveUssFile
import org.zowe.kotlinsdk.RenameData

/**
 * Class which represents factory for rename operation runner. Defined in plugin.xml
 */
class RenameOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return RenameOperationRunner(dataOpsManager)
  }
}


/**
 * Class which represents rename operation runner
 */
class RenameOperationRunner(private val dataOpsManager: DataOpsManager) : OperationRunner<RenameOperation, Unit> {

  override val operationClass = RenameOperation::class.java

  override val resultClass = Unit::class.java

  override val log = log<RenameOperationRunner>()

  /**
   * Determined if operation can be run on selected object
   * @param operation - specifies a rename operation object
   */
  override fun canRun(operation: RenameOperation): Boolean {
    return with(operation.attributes) {
      this is RemoteMemberAttributes || this is RemoteDatasetAttributes || this is RemoteUssAttributes
    }
  }

  /**
   * Runs a rename operation
   * @param operation - rename operation to be run
   * @param progressIndicator - progress indicator object
   * @throws CallException if request is not successful
   * @return Void
   */
  override fun run(
    operation: RenameOperation,
    progressIndicator: ProgressIndicator
  ) {
    when (val attributes = operation.attributes) {
      is RemoteDatasetAttributes -> {
        attributes.requesters.map {
          try {
            progressIndicator.checkCanceled()
            val response = api<DataAPI>(it.connectionConfig).renameDataset(
              authorizationToken = it.connectionConfig.authToken,
              body = RenameData(
                fromDataset = RenameData.FromDataset(
                  oldDatasetName = attributes.name
                )
              ),
              toDatasetName = operation.newName
            ).cancelByIndicator(progressIndicator).execute()
            if (response.isSuccessful) {
              runWriteActionInEdtAndWait {
                operation.file.rename(this, operation.newName)
              }
            } else {
              throw CallException(response, "Unable to rename the selected dataset")
            }
          } catch (e: Throwable) {
            if (e is CallException) {
              throw e
            } else {
              throw RuntimeException(e)
            }
          }
        }
      }

      is RemoteMemberAttributes -> {
        val parentAttributes = dataOpsManager.tryToGetAttributes(attributes.parentFile) as RemoteDatasetAttributes
        parentAttributes.requesters.map {
          try {
            progressIndicator.checkCanceled()
            log.info("Checking for duplicate names in dataset ${parentAttributes.datasetInfo.name}")
            if (operation.requester is DuplicateMemberAction) {
              val response = api<DataAPI>(it.connectionConfig).copyToDatasetMember(
                authorizationToken = it.connectionConfig.authToken,
                body = CopyDataZOS.CopyFromDataset(
                  dataset = CopyDataZOS.CopyFromDataset.Dataset(
                    parentAttributes.datasetInfo.name,
                    attributes.info.name
                  ),
                  replace = true
                ),
                toDatasetName = parentAttributes.datasetInfo.name,
                memberName = operation.newName
              ).cancelByIndicator(progressIndicator).execute()
              if (!response.isSuccessful) {
                throw CallException(response, "Unable to duplicate the selected member")
              }
            } else {
              val response = api<DataAPI>(it.connectionConfig).renameDatasetMember(
                authorizationToken = it.connectionConfig.authToken,
                body = RenameData(
                  fromDataset = RenameData.FromDataset(
                    oldDatasetName = parentAttributes.datasetInfo.name,
                    oldMemberName = attributes.info.name
                  )
                ),
                toDatasetName = parentAttributes.datasetInfo.name,
                memberName = operation.newName
              ).cancelByIndicator(progressIndicator).execute()
              if (response.isSuccessful) {
                runWriteActionInEdtAndWait {
                  operation.file.rename(this, operation.newName)
                }
              } else {
                throw CallException(response, "Unable to rename the selected member")
              }
            }
          } catch (e: Throwable) {
            if (e is CallException) {
              throw e
            } else {
              throw RuntimeException(e)
            }
          }
        }
      }

      is RemoteUssAttributes -> {
        val parentDirPath = attributes.parentDirPath
        attributes.requesters.map {
          try {
            progressIndicator.checkCanceled()
            val response = api<DataAPI>(it.connectionConfig).moveUssFile(
              authorizationToken = it.connectionConfig.authToken,
              body = MoveUssFile(
                from = attributes.path
              ),
              filePath = FilePath("$parentDirPath/${operation.newName}")
            ).cancelByIndicator(progressIndicator).execute()
            if (response.isSuccessful) {
              runWriteActionInEdtAndWait {
                operation.file.rename(this, operation.newName)
              }
            } else {
              throw CallException(response, "Unable to rename the selected file or directory")
            }
          } catch (e: Throwable) {
            if (e is CallException) {
              throw e
            } else {
              throw RuntimeException(e)
            }
          }
        }
      }
    }
  }
}
