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
import org.zowe.explorer.api.apiWithBytesConverter
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.operations.OperationRunner
import org.zowe.explorer.dataops.operations.OperationRunnerFactory
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.kotlinsdk.JESApi
import retrofit2.Response

/**
 * Class which represents factory for get jcl records operation runner. Defined in plugin.xml
 */
class GetJclRecordsOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return GetJclRecordsOperationRunner()
  }
}

/**
 * Class which represents get jcl records operation runner
 */
class GetJclRecordsOperationRunner : OperationRunner<GetJclRecordsOperation, ByteArray> {

  override val operationClass = GetJclRecordsOperation::class.java

  override val resultClass = ByteArray::class.java

  override val log = log<GetJclRecordsOperationRunner>()

  /**
   * Runs a get jcl records operation
   *
   * Sends a request to mainframe and checks response
   * @param operation specifies a get jcl records operation object [GetJclRecordsOperation]
   * @param progressIndicator interrupts operation if the computation is canceled
   * @throws Exception if method with the requested parameters is not found
   * @throws CallException if request is not successful or no response body
   * @return body of response which contains jcl content for job as byte array
   */
  override fun run(operation: GetJclRecordsOperation, progressIndicator: ProgressIndicator): ByteArray {
    progressIndicator.checkCanceled()

    val response: Response<ByteArray> = when (operation.request) {
      is BasicGetJclRecordsParams -> {
        apiWithBytesConverter<JESApi>(operation.connectionConfig).getJCLRecords(
          basicCredentials = operation.connectionConfig.authToken,
          jobId = operation.request.jobId,
          jobName = operation.request.jobName
        ).cancelByIndicator(progressIndicator).execute()
      }

      is CorrelatorGetJclRecordsParams -> {
        apiWithBytesConverter<JESApi>(operation.connectionConfig).getJCLRecords(
          basicCredentials = operation.connectionConfig.authToken,
          jobCorrelator = operation.request.jobCorrelator
        ).cancelByIndicator(progressIndicator).execute()
      }

      else -> throw Exception("Method with such parameters not found")
    }
    val body = response.body()
    if (!response.isSuccessful || body == null) {
      throw CallException(
        response,
        "Cannot get JCL records for job on ${operation.connectionConfig.name}"
      )
    }
    return body
  }

  /**
   * Determines if an operation can be run on selected object
   * @param operation specifies a get jcl records operation object [GetJclRecordsOperation]
   */
  override fun canRun(operation: GetJclRecordsOperation): Boolean {
    return true
  }
}

/**
 * Base parameter class for get jcl records operation
 *
 * Class is the parent of [BasicGetJclRecordsParams] and [CorrelatorGetJclRecordsParams]
 */
open class GetJclRecordsOperationParams

/**
 * Class which contains parameters job name and job id for get jcl records operation
 */
class BasicGetJclRecordsParams(val jobName: String, val jobId: String) : GetJclRecordsOperationParams() {
  override fun toString(): String {
    return "BasicGetJclRecordsParams(jobName='$jobName', jobId='$jobId')"
  }
}

/**
 * Class which contains parameter job correlator for get jcl records operation
 */
class CorrelatorGetJclRecordsParams(val jobCorrelator: String) : GetJclRecordsOperationParams() {
  override fun toString(): String {
    return "CorrelatorGetJclRecordsParams(jobCorrelator='$jobCorrelator')"
  }
}

/**
 * Data class that represents all information needed to send get jcl records operation request
 * @property request parameters for get jcl records [GetJclRecordsOperationParams]
 * @property connectionConfig credentials for connection to mainframe [ConnectionConfig]
 */
data class GetJclRecordsOperation(
  override val request: GetJclRecordsOperationParams,
  override val connectionConfig: ConnectionConfig
) : RemoteQuery<ConnectionConfig, GetJclRecordsOperationParams, ByteArray> {
  override val resultClass = ByteArray::class.java
}
