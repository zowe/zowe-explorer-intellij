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

package org.zowe.explorer.v3.operations.apiml

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.v3.operations.UnitOperationRunner
import org.zowe.explorer.v3.retrofit.extractCookieValue
import org.zowe.explorer.v3.state.config.connection.ApiMlConnectionConfig
import org.zowe.explorer.v3.state.config.connection.getApiMlGatewayUrl
import org.zowe.explorer.v3.state.credentials.TokenService
import org.zowe.kotlinsdk.ApiMLGetawayApi
import org.zowe.kotlinsdk.LoginRequest

private const val COOKIE_HEADER = "Set-Cookie"
private const val TOKEN_NAME_IN_COOKIES = "apimlAuthenticationToken"

class ApiMlLoginOperationRunner
  : UnitOperationRunner<ApiMlConnectionConfig, ApiMlLoginOperationData<ApiMlConnectionConfig>>() {

  override val operationDataClass = ApiMlLoginOperationData::class.java

  override fun canRun(operationData: ApiMlLoginOperationData<ApiMlConnectionConfig>): Boolean {
    return true
  }

  override fun run(
    operationData: ApiMlLoginOperationData<ApiMlConnectionConfig>,
    progressIndicator: ProgressIndicator
  ) {
    try {
      progressIndicator.checkCanceled()
      val connectionConfig = operationData.origin.connectionConfig
      //val username = CredentialService.getService().getUsernameByKey(connectionConfig.uuid) ?: ""
      //val password = CredentialService.getService().getPasswordByKey(connectionConfig.uuid)?.toString() ?: ""
      val response = api<ApiMLGetawayApi>(
        url = getApiMlGatewayUrl(connectionConfig),
        isAllowSelfSigned = !connectionConfig.rejectUnauthorized
      )
        .login(LoginRequest(operationData.username, String(operationData.password)))
        .cancelByIndicator(progressIndicator)
        .execute()
      if (response.isSuccessful) {
        val cookies = response.headers()[COOKIE_HEADER] ?: ""
        val token = extractCookieValue(cookies, TOKEN_NAME_IN_COOKIES) ?: throw CallException(response, "Failed to retrieve API ML token")
        TokenService.getService().addTokenToStorage(connectionConfig.uuid, operationData.username, token)
      } else {
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