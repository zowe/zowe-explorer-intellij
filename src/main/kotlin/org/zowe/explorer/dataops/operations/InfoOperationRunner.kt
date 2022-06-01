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
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.kotlinsdk.SystemsApi
import org.zowe.kotlinsdk.SystemsResponse
import okhttp3.Credentials

class InfoOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return InfoOperationRunner()
  }
}

class InfoOperationRunner : OperationRunner<InfoOperation, SystemsResponse> {
  override val operationClass = InfoOperation::class.java
  override val resultClass = SystemsResponse::class.java

  override fun canRun(operation: InfoOperation) = true

  override fun run(operation: InfoOperation, progressIndicator: ProgressIndicator): SystemsResponse {
    val response = api<SystemsApi>(connectionConfig = operation.connectionConfig)
      .getSystems(operation.connectionConfig.authToken)
      .cancelByIndicator(progressIndicator)
      .execute()
    if (!response.isSuccessful) {
      throw CallException(response, "Credentials are not valid")
    }
    return response?.body() ?: throw CallException(response, "Cannot parse z/OSMF info request body")
  }

}
