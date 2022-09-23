/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.r2z.InfoAPI
import eu.ibagroup.r2z.InfoResponse
import eu.ibagroup.r2z.SystemsApi
import eu.ibagroup.r2z.SystemsResponse

// TODO: doc
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
    return response.body() ?: throw CallException(response, "Cannot parse z/OSMF info request body")
  }

}