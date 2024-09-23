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
import org.zowe.kotlinsdk.HoldJobRequest
import org.zowe.kotlinsdk.HoldJobRequestBody
import org.zowe.kotlinsdk.JESApi
import retrofit2.Response

/**
 * Class which represents factory for hold job operation runner. Defined in plugin.xml
 */
class HoldJobOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return HoldJobOperationRunner()
  }
}

/**
 * Class which represents hold job operation runner
 */
class HoldJobOperationRunner : OperationRunner<HoldJobOperation, HoldJobRequest> {

  override val operationClass = HoldJobOperation::class.java

  override val resultClass = HoldJobRequest::class.java

  override val log = log<HoldJobOperationRunner>()

  /**
   * Determines if an operation can be run on selected object
   * @param operation specifies a hold job operation object [HoldJobOperation]
   */
  override fun canRun(operation: HoldJobOperation): Boolean {
    return true
  }

  /**
   * Runs a job hold operation
   *
   * Sends a request to mainframe and checks response
   * @param operation specifies a hold job operation object [HoldJobOperation]
   * @param progressIndicator interrupts operation if the computation is canceled
   * @throws Exception if method with the requested parameters is not found
   * @throws CallException if request is not successful or no response body
   * @return [HoldJobRequest] body of response
   */
  override fun run(operation: HoldJobOperation, progressIndicator: ProgressIndicator): HoldJobRequest {
    progressIndicator.checkCanceled()

    val response: Response<HoldJobRequest> = when (operation.request) {
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

/**
 * Base class which contains parameters for hold job operation
 *
 * Class is the parent of [BasicHoldJobParams] and [CorrelatorHoldJobParams]
 */
open class HoldJobOperationParams

/**
 * Class which contains parameters job name and job id for hold job operation
 */
class BasicHoldJobParams(val jobName: String, val jobId: String) : HoldJobOperationParams() {
  override fun toString(): String {
    return "BasicHoldJobParams(jobName='$jobName', jobId='$jobId')"
  }
}

/**
 * Class which contains parameter job correlator for hold job operation
 */
class CorrelatorHoldJobParams(val correlator: String) : HoldJobOperationParams() {
  override fun toString(): String {
    return "CorrelatorHoldJobParams(correlator='$correlator')"
  }
}

/**
 * Data class that represents all information needed to send hold job request
 * @property request parameters for holding a job [HoldJobOperationParams]
 * @property connectionConfig credentials for connection to mainframe [ConnectionConfig]
 */
data class HoldJobOperation(
  override val request: HoldJobOperationParams,
  override val connectionConfig: ConnectionConfig
) : RemoteQuery<ConnectionConfig, HoldJobOperationParams, HoldJobRequest> {
  override val resultClass = HoldJobRequest::class.java
}
