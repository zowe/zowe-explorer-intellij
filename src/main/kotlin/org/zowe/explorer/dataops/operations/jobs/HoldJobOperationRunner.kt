/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.operations.jobs

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
import org.zowe.kotlinsdk.*
import retrofit2.Response

class HoldJobOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return HoldJobOperationRunner()
  }
}

class HoldJobOperationRunner : OperationRunner<HoldJobOperation, HoldJobRequest> {

  override val operationClass = HoldJobOperation::class.java

  override val resultClass = HoldJobRequest::class.java

  override fun canRun(operation: HoldJobOperation): Boolean {
    return true
  }

  override fun run(operation: HoldJobOperation, progressIndicator: ProgressIndicator): HoldJobRequest {
    progressIndicator.checkCanceled()

    val response : Response<HoldJobRequest> = when (operation.request) {
      is BasicHoldJobParams -> {
        api<JESApi>(operation.connectionConfig).holdJobRequest(
          basicCredentials = operation.connectionConfig.authToken,
          jobName = operation.request.jobName,
          jobId = operation.request.jobId,
          body = HoldJobRequestBody()
        ).cancelByIndicator(progressIndicator).execute()
      }
      is CorrelatorHoldJobParams -> {
        api<JESApi>(operation.connectionConfig).holdJobRequest(
          basicCredentials = operation.connectionConfig.authToken,
          jobCorrelator = operation.request.correlator,
          body = HoldJobRequestBody()
        ).cancelByIndicator(progressIndicator).execute()
      }
      else -> throw Exception("Method with such parameters not found")
    }
    val body = response.body()
    if (!response.isSuccessful || body == null) {
      throw CallException(
        response,
        "Cannot hold job on ${operation.connectionConfig.name}"
      )
    }
    return body
  }
}

open class HoldJobOperationParams

class BasicHoldJobParams(val jobName: String, val jobId: String)  : HoldJobOperationParams()

class CorrelatorHoldJobParams(val correlator: String) : HoldJobOperationParams()

data class HoldJobOperation(
  override val request: HoldJobOperationParams,
  override val connectionConfig: ConnectionConfig
) : RemoteQuery<HoldJobOperationParams, HoldJobRequest> {
  override val resultClass = HoldJobRequest::class.java
}
