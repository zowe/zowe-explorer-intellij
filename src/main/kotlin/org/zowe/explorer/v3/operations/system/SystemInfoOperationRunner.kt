/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Dzianis Lisiankou
 */

package org.zowe.explorer.v3.operations.system

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.v3.operations.OperationRunner
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig
import org.zowe.explorer.v3.state.config.getUrlWithBasePath
import org.zowe.kotlinsdk.InfoAPI
import org.zowe.kotlinsdk.InfoResponse

class SystemInfoOperationRunner<ConnectionConfigType: HttpConnectionConfig>
  : OperationRunner<InfoResponse, ConnectionConfigType, SystemInfoOperationData<ConnectionConfigType>>() {

  override val operationDataClass = SystemInfoOperationData::class.java

  override val resultClass = InfoResponse::class.java

  override fun canRun(operationData: SystemInfoOperationData<ConnectionConfigType>): Boolean {
    return true
  }

  override fun run(
    operationData: SystemInfoOperationData<ConnectionConfigType>,
    progressIndicator: ProgressIndicator
  ): InfoResponse {
    try {
      progressIndicator.checkCanceled()
      val connectionConfig = operationData.origin.connectionConfig
      val response = api<InfoAPI>(
        url = getUrlWithBasePath(connectionConfig),
        isAllowSelfSigned = !connectionConfig.rejectUnauthorized
      )
        .getSystemInfo()
        .cancelByIndicator(progressIndicator)
        .execute()
      if (!response.isSuccessful) {
        throw CallException(response, "Unsuccessful response")
      }
      return response.body() ?: throw CallException(response, "Empty response")
    } catch (e: Throwable) {
      if (e is CallException) {
        throw e
      } else {
        throw RuntimeException(e)
      }
    }
  }

}