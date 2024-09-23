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
import org.zowe.kotlinsdk.SubmitFileNameBody
import org.zowe.kotlinsdk.SubmitJobRequest
import retrofit2.Response

/**
 * Class which represents submit operation runner
 */
class SubmitOperationRunner : OperationRunner<SubmitJobOperation, SubmitJobRequest> {

  override val operationClass = SubmitJobOperation::class.java

  override val log = log<SubmitOperationRunner>()

  /**
   * Sends submit request to mainframe and checks return code of request
   * @param operation describes the code to be submitted on mainframe and the connection configuration
   * @param progressIndicator interrupts operation if the computation is canceled
   * @return [SubmitJobRequest] body of reply from server
   */
  override fun run(operation: SubmitJobOperation, progressIndicator: ProgressIndicator): SubmitJobRequest {
    progressIndicator.checkCanceled()

    val response: Response<SubmitJobRequest> = when (operation.request) {
      is SubmitFilePathOperationParams -> {
        api<JESApi>(operation.connectionConfig).submitJobRequest(
          basicCredentials = operation.connectionConfig.authToken,
          body = SubmitFileNameBody(operation.request.submitFilePath)
        ).cancelByIndicator(progressIndicator).execute()
      }
      is SubmitJobJclOperationParams -> {
        api<JESApi>(operation.connectionConfig).submitJobRequest(
          basicCredentials = operation.connectionConfig.authToken,
          body = operation.request.jobJcl
        ).cancelByIndicator(progressIndicator).execute()
      }
      else -> throw Exception("Method with such parameters not found")
    }
    val body = response.body()
    if (!response.isSuccessful || body == null) {
      throw CallException(
        response,
        "Cannot submit file on ${operation.connectionConfig.name}"
      )
    }
    return body
  }

  override val resultClass = SubmitJobRequest::class.java

  /**
   * Determines if operation can be run on selected object
   * @param operation object which contains all info to execute submit request
   */
  override fun canRun(operation: SubmitJobOperation): Boolean {
    return true
  }
}

/**
 * Class which represents factory for submit job operation runner
 */
class SubmitJobOperationFactory : OperationRunnerFactory {

  /**
   * Creates instance of Submit operation
   */
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return SubmitOperationRunner()
  }
}


/**
 *  Base class which contains parameters for submit job operation
 */
open class SubmitOperationParams

/**
 * Class which contains parameters for submit operation by file path
 * @param submitFilePath path to file which contains code that should be submitted on mainframe
 */
class SubmitFilePathOperationParams(val submitFilePath: String) : SubmitOperationParams() {
  override fun toString(): String {
    return "SubmitFilePathOperationParams(submitFilePath='$submitFilePath')"
  }
}

/**
 * Class which contains parameters for submit operation by job jcl
 * @param jobJcl code that should be submitted on mainframe
 */
class SubmitJobJclOperationParams(val jobJcl: String) : SubmitOperationParams() {
  override fun toString(): String {
    return "SubmitJobJclOperationParams(jobJcl='$jobJcl')"
  }
}

/**
 * Data class which represents all info that is needed to execute submit operation
 * @param request code that should be submitted on mainframe
 * @param connectionConfig credentials to mainframe on which request should be executed
 */
data class SubmitJobOperation(
  override val request: SubmitOperationParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteQuery<ConnectionConfig, SubmitOperationParams, SubmitJobRequest> {
  override val resultClass = SubmitJobRequest::class.java
}
