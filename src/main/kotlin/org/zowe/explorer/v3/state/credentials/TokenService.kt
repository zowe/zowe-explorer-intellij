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

package org.zowe.explorer.v3.state.credentials

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service
class TokenService {

  companion object {
    fun getService(): TokenService = service()
  }

  fun addTokenToStorage(connectionConfigUuid: String, user: String, token: String) {
    val credentials = Credentials(user, token)
    service<PasswordSafe>().set(createAttributes(connectionConfigUuid), credentials)
  }

  fun getTokenFromStorage(connectionConfigUuid: String): String {
    val credentials = service<PasswordSafe>().get(createAttributes(connectionConfigUuid))
    return credentials?.password?.toString() ?: throw RuntimeException("Credentials not found")
  }

  private fun createAttributes(key: String): CredentialAttributes {
    return CredentialAttributes(generateServiceName("TokenStorage", key))
  }

}