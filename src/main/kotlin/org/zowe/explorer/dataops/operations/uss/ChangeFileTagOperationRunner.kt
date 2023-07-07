/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.operations.uss

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.operations.OperationRunner
import org.zowe.explorer.dataops.operations.OperationRunnerFactory
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.kotlinsdk.*
import okhttp3.ResponseBody

/**
 * Class which represents factory for change file tag operation runner. Defined in plugin.xml.
 */
class ChangeFileTagOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return ChangeFileTagOperationRunner()
  }
}

/**
 * Class which represents change file tag operation runner.
 */
class ChangeFileTagOperationRunner: OperationRunner<ChangeFileTagOperation, ResponseBody> {
  override val operationClass = ChangeFileTagOperation::class.java

  override val resultClass = ResponseBody::class.java

  override val log = log<ChangeFileTagOperationRunner>()

  /**
   * Runs a change file tag operation.
   *
   * Sends a request to mainframe and checks response.
   * @param operation specifies a change file tag operation object [ChangeFileTagOperation].
   * @param progressIndicator interrupts operation if the computation is canceled.
   * @throws CallException if request is not successful or no response body.
   * @return [ResponseBody] body of response.
   */
  override fun run(operation: ChangeFileTagOperation, progressIndicator: ProgressIndicator): ResponseBody {
    progressIndicator.checkCanceled()

    val response = api<DataAPI>(operation.connectionConfig).changeFileTag(
      authorizationToken = operation.connectionConfig.authToken,
      body = ChangeTag(
        action = operation.request.action,
        type = operation.request.type,
        codeSet = operation.request.codeSet
      ),
      filePath = FilePath(operation.request.filePath)
    ).cancelByIndicator(progressIndicator).execute()
    val body = response.body()
    if (!response.isSuccessful || body == null) {
      throw CallException(
        response,
        "Cannot change file tag for ${operation.request.filePath} on ${operation.connectionConfig.name}"
      )
    }
    return body
  }

  /**
   * Determines if an operation can be run on selected object.
   * @param operation specifies a change file tag operation object [ChangeFileTagOperation].
   */
  override fun canRun(operation: ChangeFileTagOperation): Boolean {
    return true
  }
}

/**
 * Class which contains parameters for change file tag operation.
 * @property filePath path to the uss file.
 * @property action action to be performed with file tag.
 * @property type uss file data type.
 * @property codeSet codeSet set to uss file tag.
 */
class ChangeFileTagOperationParams(
  val filePath: String,
  val action: TagAction,
  val type: UssFileDataType? = null,
  val codeSet: String? = null
) {
  override fun toString(): String {
    return "ChangeFileTagOperationParams(filePath='$filePath', action=$action, type=$type, codeSet=$codeSet)"
  }
}

/**
 * Data class that represents all information needed to send change file tag request
 * @property request parameters for changing file tag [ChangeFileTagOperationParams]
 * @property connectionConfig credentials for connection to mainframe [ConnectionConfig]
 */
data class ChangeFileTagOperation(
  override val request: ChangeFileTagOperationParams,
  override val connectionConfig: ConnectionConfig
): RemoteQuery<ConnectionConfig, ChangeFileTagOperationParams, ResponseBody> {
  override val resultClass = ResponseBody::class.java
}
