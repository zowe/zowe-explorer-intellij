/*
 * Copyright (c) 2024 IBA Group.
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
 *   Dzianis Lisiankou
 */

package org.zowe.explorer.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig
import org.zowe.explorer.v3.state.config.getUrlWithBasePath
import org.zowe.kotlinsdk.InfoAPI
import org.zowe.kotlinsdk.InfoResponse

/**
 * Factory class to build an instance of system info operation runner. Defined in plugin.xml
 */
class ZOSInfoOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return ZOSInfoOperationRunner()
  }
}

/**
 * Base class implementation for running system info operation.
 */
class ZOSInfoOperationRunner : OperationRunner<ZOSInfoOperation, InfoResponse> {
  override val operationClass = ZOSInfoOperation::class.java
  override val resultClass = InfoResponse::class.java
  override val log = log<ZOSInfoOperationRunner>()

  /**
   * Method determines if an operation can be run
   * @param operation - represents an operation
   */
  override fun canRun(operation: ZOSInfoOperation) = true

  /**
   * Method for running system info operation and return response body to caller
   * @param operation - represents an operation to be run
   * @param progressIndicator - represents a progress indicator object
   * @throws CallException if any error occurred or response body is null
   * @return InfoResponse serialized object
   */
  override fun run(operation: ZOSInfoOperation, progressIndicator: ProgressIndicator): InfoResponse {
    val api = when(operation.connectionConfig) {
      is ConnectionConfig -> {
        api<InfoAPI>(connectionConfig = operation.connectionConfig as ConnectionConfig)
      }

      is HttpConnectionConfig -> {
        val httpConnectionConfig = operation.connectionConfig as HttpConnectionConfig
        api<InfoAPI>(getUrlWithBasePath(httpConnectionConfig), !httpConnectionConfig.rejectUnauthorized)
      }

      else -> throw UnsupportedOperationException()
    }
    val response = api
          .getSystemInfo()
          .cancelByIndicator(progressIndicator)
          .execute()
    if (!response.isSuccessful) {
      throw CallException(response, "An internal error has occurred")
    }
    return response.body() ?: throw CallException(response, "Cannot parse z/OSMF info request body")
  }
}
