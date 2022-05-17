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
import org.zowe.kotlinsdk.JobStatus
import retrofit2.Response

class GetJobStatusOperationRunner : OperationRunner<GetJobStatusOperation, JobStatus> {

  override val operationClass = GetJobStatusOperation::class.java

  override fun run(operation: GetJobStatusOperation, progressIndicator: ProgressIndicator): JobStatus {
    progressIndicator.checkCanceled()

    val response : Response<JobStatus> = when (operation.request) {
      is GetJobStatusOperationParams.BasicStatusParams -> {
        api<JESApi>(operation.connectionConfig).getJobStatus(
          basicCredentials = operation.connectionConfig.authToken,
          jobName = operation.request.jobName,
          jobId = operation.request.jobId
        ).cancelByIndicator(progressIndicator).execute()
      }
      is GetJobStatusOperationParams.CorrelatorStatusParams -> {
        api<JESApi>(operation.connectionConfig).getJobStatus(
          basicCredentials = operation.connectionConfig.authToken,
          jobCorrelator = operation.request.correlator
        ).cancelByIndicator(progressIndicator).execute()
      }
    }

    val body = response.body()
    if (!response.isSuccessful || body == null) {
      throw CallException(
        response,
        "Cannot print job status on ${operation.connectionConfig.name}"
      )
    }
    return body
  }

  override val resultClass = JobStatus::class.java

  override fun canRun(operation: GetJobStatusOperation): Boolean {
    return true
  }
}

class GetJobStatusOperationFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return GetJobStatusOperationRunner()
  }
}

sealed class GetJobStatusOperationParams {
  class BasicStatusParams(val jobName: String, val jobId: String)  : GetJobStatusOperationParams()
  class CorrelatorStatusParams(val correlator: String) : GetJobStatusOperationParams()
}

data class GetJobStatusOperation(
  override val request: GetJobStatusOperationParams,
  override val connectionConfig: ConnectionConfig
) : RemoteQuery<GetJobStatusOperationParams, JobStatus> {
  override val resultClass = JobStatus::class.java
}
