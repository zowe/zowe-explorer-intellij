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

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.dataops.operations.OperationRunnerFactory
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.r2z.JESApi
import eu.ibagroup.r2z.JobStatus
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
  override val connectionConfig: ConnectionConfig,
  override val urlConnection: UrlConnection
) : RemoteQuery<GetJobStatusOperationParams, JobStatus> {
  override val resultClass = JobStatus::class.java
}