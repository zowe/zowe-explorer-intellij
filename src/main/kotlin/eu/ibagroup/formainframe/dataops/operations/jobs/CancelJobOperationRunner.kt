/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.operations.jobs

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.dataops.operations.OperationRunnerFactory
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.r2z.CancelJobRequest
import eu.ibagroup.r2z.CancelJobRequestBody
import eu.ibagroup.r2z.JESApi
import retrofit2.Response

// TODO: doc Denis
class CancelJobOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return CancelJobOperationRunner()
  }
}

class CancelJobOperationRunner : OperationRunner<CancelJobOperation, CancelJobRequest> {

  override val operationClass = CancelJobOperation::class.java

  override val resultClass = CancelJobRequest::class.java

  override fun canRun(operation: CancelJobOperation): Boolean {
    return true
  }

  override fun run(operation: CancelJobOperation, progressIndicator: ProgressIndicator): CancelJobRequest {
    progressIndicator.checkCanceled()

    val response: Response<CancelJobRequest> = when (operation.request) {
      is BasicCancelJobParams -> {
        api<JESApi>(operation.connectionConfig).cancelJobRequest(
          basicCredentials = operation.connectionConfig.authToken,
          jobId = operation.request.jobId,
          jobName = operation.request.jobName,
          body = CancelJobRequestBody()
        ).cancelByIndicator(progressIndicator).execute()
      }
      is CorrelatorCancelJobParams -> {
        api<JESApi>(operation.connectionConfig).cancelJobRequest(
          basicCredentials = operation.connectionConfig.authToken,
          jobCorrelator = operation.request.correlator,
          body = CancelJobRequestBody()
        ).cancelByIndicator(progressIndicator).execute()
      }
      else -> throw Exception("Method with such parameters not found")
    }
    val body = response.body()
    if (!response.isSuccessful || body == null) {
      throw CallException(
        response,
        "Cannot cancel job on ${operation.connectionConfig.name}"
      )
    }
    return body
  }
}

open class CancelJobOperationParams

class BasicCancelJobParams(val jobName: String, val jobId: String) : CancelJobOperationParams()

class CorrelatorCancelJobParams(val correlator: String) : CancelJobOperationParams()

data class CancelJobOperation(
  override val request: CancelJobOperationParams,
  override val connectionConfig: ConnectionConfig
) : RemoteQuery<CancelJobOperationParams, CancelJobRequest> {
  override val resultClass = CancelJobRequest::class.java
}