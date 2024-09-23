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
import eu.ibagroup.formainframe.utils.log
import org.zowe.kotlinsdk.JESApi
import org.zowe.kotlinsdk.Job
import retrofit2.Response

/**
 * Class which represents get job status operation runner
 */
class GetJobStatusOperationRunner : OperationRunner<GetJobStatusOperation, Job> {

  override val operationClass = GetJobStatusOperation::class.java

  override val log = log<GetJobStatusOperationRunner>()

  /**
   * Sends get job status request to mainframe and checks return code of request
   * @param operation describes the job info of which should be gotten and the connection configuration
   * @param progressIndicator interrupts operation if the computation is canceled
   * @return [SubmitJobRequest] body of reply from server
   */
  override fun run(operation: GetJobStatusOperation, progressIndicator: ProgressIndicator): Job {
    progressIndicator.checkCanceled()

    val response: Response<Job> = when (operation.request) {
      is GetJobStatusOperationParams.BasicStatusParams -> {
        api<JESApi>(operation.connectionConfig).getJob(
          basicCredentials = operation.connectionConfig.authToken,
          jobName = operation.request.jobName,
          jobId = operation.request.jobId
        ).cancelByIndicator(progressIndicator).execute()
      }
      is GetJobStatusOperationParams.CorrelatorStatusParams -> {
        api<JESApi>(operation.connectionConfig).getJob(
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

  override val resultClass = Job::class.java

  /**
   * Determines if operation can be run on selected object
   * @param operation object which contains all info to execute get job status request
   */
  override fun canRun(operation: GetJobStatusOperation): Boolean {
    return true
  }
}

/**
 * Class which represents factory for get job status operation runner
 */
class GetJobStatusOperationFactory : OperationRunnerFactory {

  /**
   * Creates instance of get job status operation
   */
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return GetJobStatusOperationRunner()
  }
}

/**
 * Sealed class which contains parameters for get job status operation
 */
sealed class GetJobStatusOperationParams {

  /**
   * Class which contains parameters for get job status operation,
   * consists of jobName and jobID, 1st of 2 options to set particular job
   */
  class BasicStatusParams(val jobName: String, val jobId: String) : GetJobStatusOperationParams() {
    override fun toString(): String {
      return "BasicStatusParams(jobName='$jobName', jobId='$jobId')"
    }
  }

  /**
   * Class which contains parameters for get job status operation,
   * consists of job correlator, 2nd of 2 options to set particular job
   */
  class CorrelatorStatusParams(val correlator: String) : GetJobStatusOperationParams() {
    override fun toString(): String {
      return "CorrelatorStatusParams(correlator='$correlator')"
    }
  }
}

/**
 * Data class which represents all info that is needed to execute get job status operation
 * @param request job on mainframe info of which should be gotten
 * @param connectionConfig credentials to mainframe on which request should be executed
 */
data class GetJobStatusOperation(
  override val request: GetJobStatusOperationParams,
  override val connectionConfig: ConnectionConfig
) : RemoteQuery<ConnectionConfig, GetJobStatusOperationParams, Job> {
  override val resultClass = Job::class.java
}
