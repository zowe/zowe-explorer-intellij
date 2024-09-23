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
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.log
import org.zowe.kotlinsdk.ChangePassword
import org.zowe.kotlinsdk.ChangePasswordResponse
import org.zowe.kotlinsdk.ServiceAPI
import retrofit2.Response

/**
 * Class which represents factory for change password operation runner. Defined in plugin.xml
 */
class ChangePasswordOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return ChangePasswordOperationRunner()
  }
}

/**
 * Class which represents change password operation runner
 */
class ChangePasswordOperationRunner : OperationRunner<ChangePasswordOperation, ChangePasswordResponse> {

  override val operationClass = ChangePasswordOperation::class.java

  override val resultClass = ChangePasswordResponse::class.java

  override val log = log<ChangePasswordOperationRunner>()

  /**
   * Determines if an operation can be run on selected object
   * @param operation specifies a change password operation object [ChangePasswordOperation]
   */
  override fun canRun(operation: ChangePasswordOperation): Boolean {
    return true
  }

  /**
   * Runs a change password operation
   *
   * Sends a request to mainframe and checks response
   * @param operation specifies a change password operation object [ChangePasswordOperation]
   * @param progressIndicator interrupts operation if the computation is canceled
   * @throws Exception if method with the requested parameters is not found
   * @throws CallException if request is not successful or no response body
   * @return [ChangePasswordResponse] body of response
   */
  override fun run(operation: ChangePasswordOperation, progressIndicator: ProgressIndicator): ChangePasswordResponse {
    progressIndicator.checkCanceled()

    val response: Response<ChangePasswordResponse> = api<ServiceAPI>(operation.connectionConfig).changeUserPassword(
      body = operation.request
    ).cancelByIndicator(progressIndicator).execute()

    val body = response.body()
    if (!response.isSuccessful || body == null) {
      throw CallException(
        response,
        "Cannot change password on ${operation.connectionConfig.url}"
      )
    }
    return body
  }
}

/**
 * Data class that represents all information needed to send change password request
 * @property request parameters for changing user password [ChangePassword]
 * @property connectionConfig credentials for connection to mainframe [ConnectionConfig]
 */
data class ChangePasswordOperation(
  override val request: ChangePassword,
  override val connectionConfig: ConnectionConfig
) : RemoteQuery<ConnectionConfig, ChangePassword, ChangePasswordResponse> {
  override val resultClass = ChangePasswordResponse::class.java
}
