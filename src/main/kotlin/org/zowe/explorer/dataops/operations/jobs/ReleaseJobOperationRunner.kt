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
import org.zowe.kotlinsdk.JESApi
import org.zowe.kotlinsdk.ReleaseJobRequest
import org.zowe.kotlinsdk.ReleaseJobRequestBody
import retrofit2.Response

class ReleaseJobOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return ReleaseJobOperationRunner()
  }
}

class ReleaseJobOperationRunner : OperationRunner<ReleaseJobOperation, ReleaseJobRequest> {

  override val operationClass = ReleaseJobOperation::class.java

  override val resultClass = ReleaseJobRequest::class.java

  override fun canRun(operation: ReleaseJobOperation): Boolean {
    return true
  }

  override fun run(operation: ReleaseJobOperation, progressIndicator: ProgressIndicator): ReleaseJobRequest {
    progressIndicator.checkCanceled()

    val response : Response<ReleaseJobRequest> = when (operation.request) {
      is BasicReleaseJobParams -> {
        api<JESApi>(operation.connectionConfig).releaseJobRequest(
          basicCredentials = operation.connectionConfig.authToken,
          jobName = operation.request.jobName,
          jobId = operation.request.jobId,
          body = ReleaseJobRequestBody()
        ).cancelByIndicator(progressIndicator).execute()
      }
      is CorrelatorReleaseJobParams -> {
        api<JESApi>(operation.connectionConfig).releaseJobRequest(
          basicCredentials = operation.connectionConfig.authToken,
          jobCorrelator = operation.request.correlator,
          body = ReleaseJobRequestBody()
        ).cancelByIndicator(progressIndicator).execute()
      }
      else -> throw Exception("Method with such parameters not found")
    }
    val body = response.body()
    if (!response.isSuccessful || body == null) {
      throw CallException(
        response,
        "Cannot release job on ${operation.connectionConfig.name}"
      )
    }
    return body
  }
}

open class ReleaseJobOperationParams

class BasicReleaseJobParams(val jobName: String, val jobId: String) : ReleaseJobOperationParams()

class CorrelatorReleaseJobParams(val correlator: String) : ReleaseJobOperationParams()

data class ReleaseJobOperation(
  override val request: ReleaseJobOperationParams,
  override val connectionConfig: ConnectionConfig
) : RemoteQuery<ReleaseJobOperationParams, ReleaseJobRequest> {
  override val resultClass = ReleaseJobRequest::class.java
}
