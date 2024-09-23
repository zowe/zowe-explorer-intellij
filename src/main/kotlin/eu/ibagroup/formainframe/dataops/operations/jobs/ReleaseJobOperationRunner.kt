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
import org.zowe.kotlinsdk.ReleaseJobRequest
import org.zowe.kotlinsdk.ReleaseJobRequestBody
import retrofit2.Response

/** Factory for release job operation runner */
class ReleaseJobOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return ReleaseJobOperationRunner()
  }
}

/** Release operation runner */
class ReleaseJobOperationRunner : OperationRunner<ReleaseJobOperation, ReleaseJobRequest> {

  override val operationClass = ReleaseJobOperation::class.java

  override val resultClass = ReleaseJobRequest::class.java

  override val log = log<ReleaseJobOperationRunner>()

  override fun canRun(operation: ReleaseJobOperation): Boolean {
    return true
  }

  /**
   * Method that sends release request to mf
   * @param operation describes the parameters to be sent and the connection configuration
   * @param progressIndicator to interrupt if the computation is canceled
   * @return [ReleaseJobRequest] body
   */
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

/** Job Name and Job Id are used */
class BasicReleaseJobParams(val jobName: String, val jobId: String) : ReleaseJobOperationParams() {
  override fun toString(): String {
    return "BasicReleaseJobParams(jobName='$jobName', jobId='$jobId')"
  }
}

/** Correlator is used */
class CorrelatorReleaseJobParams(val correlator: String) : ReleaseJobOperationParams() {
  override fun toString(): String {
    return "CorrelatorReleaseJobParams(correlator='$correlator')"
  }
}

/** Class for release job operation */
data class ReleaseJobOperation(
  override val request: ReleaseJobOperationParams,
  override val connectionConfig: ConnectionConfig
) : RemoteQuery<ConnectionConfig, ReleaseJobOperationParams, ReleaseJobRequest> {
  override val resultClass = ReleaseJobRequest::class.java
}
