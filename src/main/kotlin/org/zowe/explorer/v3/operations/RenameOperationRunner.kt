/*
 * Copyright (c) 2024 IBA Group.
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

package org.zowe.explorer.v3.operations

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.attributes.Requester
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.runWriteActionInEdtAndWait
import org.zowe.explorer.v3.ConnectionConfigOldStruct
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath
import org.zowe.kotlinsdk.MoveUssFile
import org.zowe.kotlinsdk.RenameData
import retrofit2.Call

typealias ConnectionConfigOld = org.zowe.explorer.config.connect.ConnectionConfig
typealias UssRequesterOld = org.zowe.explorer.dataops.attributes.UssRequester

/** [RenameOperationData] runner */
class RenameOperationRunner<ConnectionConfigType : ConnectionConfigOldStruct> :
  UnitOperationRunner<ConnectionConfigType, RenameOperationData<ConnectionConfigType>>() {

  override val operationDataClass = RenameOperationData::class.java

  // TODO: introduce other entities rename and change canRun respectively
  /**
   * Allow operation run only for datasets, members and USS files / folders
   * @see [OperationRunner.canRun]
   */
  override fun canRun(operationData: RenameOperationData<ConnectionConfigType>): Boolean {
    return with(operationData.attributes) {
      this is RemoteUssAttributes
    }
  }

  /**
   * Run the [RenameOperationData] with the provided parameters
   * @see [OperationRunner.run]
   */
  override fun run(
    operationData: RenameOperationData<ConnectionConfigType>,
    progressIndicator: ProgressIndicator
  ) {
    when (val attributes = operationData.attributes) {
//      TODO: rework entirely
//      is RemoteDatasetAttributes -> {
//        // TODO: requesters.forEach - remove
//        attributes.requesters.forEach {
//          val renameOperationCallBuilder = { connectionConfig: ConnectionConfigOld ->
//            api<DataAPI>(connectionConfig).renameDataset(
//              authorizationToken = connectionConfig.authToken,
//              body = RenameData(
//                fromDataset = RenameData.FromDataset(
//                  oldDatasetName = attributes.name
//                )
//              ),
//              toDatasetName = operationData.newName
//            )
//          }
//          processRenameOperation(
//            operationData,
//            progressIndicator,
//            it,
//            renameOperationCallBuilder,
//            "Unable to rename the selected dataset"
//          )
//        }
//      }
//
//      TODO: rework entirely
//      is RemoteMemberAttributes -> {
//        val parentAttributes = DataOpsManager.getService()
//          .tryToGetAttributes(attributes.parentFile) as RemoteDatasetAttributes
//        // TODO: requesters.forEach - remove
//        parentAttributes.requesters.forEach {
//          val renameOperationCallBuilder = { connectionConfig: ConnectionConfigOld ->
//            api<DataAPI>(connectionConfig).renameDatasetMember(
//              authorizationToken = connectionConfig.authToken,
//              body = RenameData(
//                fromDataset = RenameData.FromDataset(
//                  oldDatasetName = parentAttributes.datasetInfo.name,
//                  oldMemberName = attributes.info.name
//                )
//              ),
//              toDatasetName = parentAttributes.datasetInfo.name,
//              memberName = operationData.newName
//            )
//          }
//          processRenameOperation(
//            operationData,
//            progressIndicator,
//            it,
//            renameOperationCallBuilder,
//            "Unable to rename the selected member"
//          )
//        }
//      }

      // TODO: rework
      is RemoteUssAttributes -> {
        val newRequester = operationData.origin
        val oldRequester = UssRequesterOld(
          ConnectionConfigOld(
            newRequester.connectionConfig.uuid,
            newRequester.connectionConfig.name,
            newRequester.connectionConfig.url,
            newRequester.connectionConfig.isAllowSelfSigned,
            newRequester.connectionConfig.zVersion,
            newRequester.connectionConfig.owner
          )
        )
        val parentDirPath = attributes.parentDirPath
        val renameOperationCallBuilder = { connectionConfig: ConnectionConfigOld ->
          api<DataAPI>(connectionConfig).moveUssFile(
            authorizationToken = connectionConfig.authToken,
            body = MoveUssFile(
              from = attributes.path
            ),
            filePath = FilePath("$parentDirPath/${operationData.newName}")
          )
        }
        processRenameOperation(
          operationData,
          progressIndicator,
          oldRequester,
          renameOperationCallBuilder,
          "Unable to rename the selected file or directory"
        )
      }
    }
  }

  /**
   * Send the rename operation call and rename the respective elements in the virtual file system
   * @param operationData the [RenameOperationData] instance
   * @param progressIndicator the progress indicator to cancel the operation by on the call end
   * @param requester ...
   * @param renameOperationCallBuilder the operation call builder to build the call
   * @param exceptionMsg the exception message to put in the [CallException] if the operation was not successful
   */
  private fun processRenameOperation(
    operationData: RenameOperationData<ConnectionConfigType>,
    progressIndicator: ProgressIndicator,
    requester: Requester<ConnectionConfigOld>,
    renameOperationCallBuilder: (ConnectionConfigOld) -> Call<Void>,
    exceptionMsg: String = "Unable to rename the element"
  ) {
    try {
      progressIndicator.checkCanceled()
      val response = renameOperationCallBuilder(requester.connectionConfig)
        .cancelByIndicator(progressIndicator)
        .execute()
      if (response.isSuccessful) {
        runWriteActionInEdtAndWait {
          operationData.file.rename(this, operationData.newName)
        }
      } else {
        throw CallException(response, exceptionMsg)
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
