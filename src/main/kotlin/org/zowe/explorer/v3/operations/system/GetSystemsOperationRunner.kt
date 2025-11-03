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
import org.zowe.explorer.v3.operations.UnitOperationRunner
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig
import org.zowe.explorer.v3.state.config.getUrlWithBasePath
import org.zowe.explorer.v3.state.credentials.TokenService
import org.zowe.kotlinsdk.SystemsApi

class GetSystemsOperationRunner<ConnectionConfigType : HttpConnectionConfig> :
  UnitOperationRunner<ConnectionConfigType, GetSystemsOperationData<ConnectionConfigType>>() {

  override val operationDataClass = GetSystemsOperationData::class.java

  override fun canRun(operationData: GetSystemsOperationData<ConnectionConfigType>): Boolean {
    return true
  }

  override fun run(
    operationData: GetSystemsOperationData<ConnectionConfigType>,
    progressIndicator: ProgressIndicator
  ) {
    try {
      progressIndicator.checkCanceled()
      val connectionConfig = operationData.origin.connectionConfig
      val response = api<SystemsApi>(
        url = getUrlWithBasePath(connectionConfig),
        isAllowSelfSigned = !connectionConfig.rejectUnauthorized
      )
        .getSystems(TokenService.getService().getTokenFromStorage(connectionConfig.uuid))
        .cancelByIndicator(progressIndicator)
        .execute()
      if (!response.isSuccessful) {
        // TODO: handle response message
        throw CallException(response, "Unsuccessful response")
      }
    } catch (e: Throwable) {
      if (e is CallException) {
        throw e
      } else {
        throw RuntimeException(e)
      }
    }
  }

}