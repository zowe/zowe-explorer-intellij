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
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.log
import org.zowe.kotlinsdk.DataAPI

/**
 * Class which represents factory for dataset member delete operation runner. Defined in plugin.xml.
 */
class DeleteMemberOperationRunnerFactory: OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return DeleteMemberOperationRunner()
  }
}

/**
 * Class which represents dataset member delete operation runner.
 */
class DeleteMemberOperationRunner: OperationRunner<DeleteMemberOperation, Unit> {

  override val operationClass = DeleteMemberOperation::class.java

  override val resultClass = Unit::class.java

  override val log = log<DeleteMemberOperationRunner>()

  /**
   * Runs a dataset member delete operation.
   *
   * Sends a request to mainframe and checks response.
   * @param operation specifies the object of the dataset member delete operation [DeleteMemberOperation].
   * @param progressIndicator interrupts operation if the computation is canceled.
   * @throws CallException if request is not successful.
   */
  override fun run(operation: DeleteMemberOperation, progressIndicator: ProgressIndicator) {
    progressIndicator.checkCanceled()

    val response = api<DataAPI>(operation.connectionConfig).deleteDatasetMember(
      authorizationToken = operation.connectionConfig.authToken,
      datasetName = operation.request.datasetName,
      memberName = operation.request.memberName
    ).cancelByIndicator(progressIndicator).execute()

    if (!response.isSuccessful) {
      throw CallException(
        response,
        "Cannot delete member ${operation.request.datasetName}(${operation.request.memberName}) " +
            "on ${operation.connectionConfig.name}"
      )
    }
  }

  /**
   * Determines if an operation can be run on selected object.
   * @param operation specifies the object of the dataset member delete operation [DeleteMemberOperation].
   */
  override fun canRun(operation: DeleteMemberOperation): Boolean {
    return true
  }
}

/**
 * Data class which contains parameters for dataset member delete operation.
 * @param datasetName dataset name.
 * @param memberName dataset member name,
 */
data class DeleteMemberOperationParams(val datasetName: String, val memberName: String)

/**
 * Data class that represents all information needed to send a request to delete dataset member.
 * @property request parameters for deleting a member [DeleteMemberOperationParams].
 * @property connectionConfig credentials for connection to mainframe [ConnectionConfig].
 */
data class DeleteMemberOperation(
  override val request: DeleteMemberOperationParams,
  override val connectionConfig: ConnectionConfig
) : RemoteUnitOperation<DeleteMemberOperationParams>
