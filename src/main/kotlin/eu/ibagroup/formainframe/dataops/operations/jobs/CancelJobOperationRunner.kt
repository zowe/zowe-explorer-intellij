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
import org.zowe.kotlinsdk.CancelJobRequest
import org.zowe.kotlinsdk.CancelJobRequestBody
import org.zowe.kotlinsdk.JESApi
import retrofit2.Response

/**
 * Class which represents factory for cancel job operation runner. Defined in plugin.xml
 */
class CancelJobOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return CancelJobOperationRunner()
  }
}

/**
 * Class which represents cancel job operation runner
 */
class CancelJobOperationRunner : OperationRunner<CancelJobOperation, CancelJobRequest> {

  override val operationClass = CancelJobOperation::class.java

  override val resultClass = CancelJobRequest::class.java

  override val log = log<CancelJobOperationRunner>()

  /**
   * Determines if an operation can be run on selected object
   * @param operation specifies a cancel job operation object [CancelJobOperation]
   */
  override fun canRun(operation: CancelJobOperation): Boolean {
    return true
  }

  /**
   * Runs a job cancel operation
   *
   * Sends a request to mainframe and checks response
   * @param operation specifies a cancel job operation object [CancelJobOperation]
   * @param progressIndicator interrupts operation if the computation is canceled
   * @throws Exception if method with the requested parameters is not found
   * @throws CallException if request is not successful or no response body
   * @return [CancelJobRequest] body of response
   */
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

/**
 * Base class which contains parameters for cancel job operation
 *
 * Class is the parent of [BasicCancelJobParams] and [CorrelatorCancelJobParams]
 */
open class CancelJobOperationParams

/**
 * Class which contains parameters job name and job id for cancel job operation
 */
class BasicCancelJobParams(val jobName: String, val jobId: String) : CancelJobOperationParams() {
  override fun toString(): String {
    return "BasicCancelJobParams(jobName='$jobName', jobId='$jobId')"
  }
}

/**
 * Class which contains parameter job correlator for cancel job operation
 */
class CorrelatorCancelJobParams(val correlator: String) : CancelJobOperationParams() {
  override fun toString(): String {
    return "CorrelatorCancelJobParams(correlator='$correlator')"
  }
}

/**
 * Data class that represents all information needed to send cancel job request
 * @property request parameters for canceling a job [CancelJobOperationParams]
 * @property connectionConfig credentials for connection to mainframe [ConnectionConfig]
 */
data class CancelJobOperation(
  override val request: CancelJobOperationParams,
  override val connectionConfig: ConnectionConfig
) : RemoteQuery<ConnectionConfig, CancelJobOperationParams, CancelJobRequest> {
  override val resultClass = CancelJobRequest::class.java
}
