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
import eu.ibagroup.r2z.JESApi
import eu.ibagroup.r2z.ReleaseJobRequest
import eu.ibagroup.r2z.ReleaseJobRequestBody
import retrofit2.Response

// TODO: doc Nikita
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

    val response: Response<ReleaseJobRequest> = when (operation.request) {
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
