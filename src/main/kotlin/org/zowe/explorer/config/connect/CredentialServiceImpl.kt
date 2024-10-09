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

package org.zowe.explorer.config.connect

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import org.zowe.explorer.utils.sendTopic

private const val USER_PASSWORD_SUB = "MySystem"

private const val USER_TOKEN_SUB = "User&Token"

/**
 * Function to create credential attributes with own key.
 * @param key string key identifying credential attributes.
 * @return generated credential attributes [CredentialAttributes].
 */
private fun createCredentialAttributes(subsystem: String, key: String): CredentialAttributes {
  return CredentialAttributes(generateServiceName(subsystem, key))
}

/**
 * Class that implements a credential service to work with user credentials.
 */
class CredentialServiceImpl : CredentialService {

  /**
   * Get PasswordSafe service
   * Required for test purposes
   */
  private fun getPasswordSafeService(): PasswordSafe {
    return service<PasswordSafe>()
  }

  /**
   * Get user credentials by connection config UUID.
   * @param connectionConfigUuid connection configuration universally unique identifier.
   * @return user credentials [Credentials] if they are.
   */
  private fun getCredentials(subsystem: String, connectionConfigUuid: String): Credentials? {
    val credentialAttributes = createCredentialAttributes(subsystem, connectionConfigUuid)
    var ret = service<PasswordSafe>().get(credentialAttributes)
    // Another attempt to read the password was added, since the saving of credentials occurs in a separate thread
    // and depends on the operating system. If we saved the credentials and try to read them in another thread,
    // but they have not yet actually been saved in storage.
    if (ret == null) {
      Thread.sleep(10)
      ret = getPasswordSafeService().get(credentialAttributes)
    }
    return ret
  }

  /**
   * Get username by connection config UUID.
   * @see CredentialService.getUsernameByKey
   */
  override fun getUsernameByKey(connectionConfigUuid: String): String? {
    val credentials = getCredentials(USER_PASSWORD_SUB, connectionConfigUuid)
    val tokenCredentials = getCredentials(USER_TOKEN_SUB, connectionConfigUuid)
    return credentials?.userName ?: tokenCredentials?.userName
  }

  /**
   * Get user password by connection config UUID.
   * @see CredentialService.getPasswordByKey
   */
  override fun getPasswordByKey(connectionConfigUuid: String): String? {
    val credentials = getCredentials(USER_PASSWORD_SUB, connectionConfigUuid)
    return credentials?.getPasswordAsString()
  }

  override fun getTokenByKey(connectionConfigUuid: String): String? {
    val tokenCredentials = getCredentials(USER_TOKEN_SUB, connectionConfigUuid)
    return tokenCredentials?.getPasswordAsString()
  }

  /**
   * Set user credentials.
   * @see CredentialService.setCredentials
   */
  override fun setCredentials(connectionConfigUuid: String, username: String, password: String, token: String?) {
    val credentialAttributes = createCredentialAttributes(USER_PASSWORD_SUB, connectionConfigUuid)
    val credentials = Credentials(username, password)
    val tokenCredentialAttributes = createCredentialAttributes(USER_TOKEN_SUB, connectionConfigUuid)
    val tokenCredentials = Credentials(username, token)
    runBackgroundableTask("Setting user credentials") {
      getPasswordSafeService().set(credentialAttributes, credentials)
      getPasswordSafeService().set(tokenCredentialAttributes, tokenCredentials)
      sendTopic(CREDENTIALS_CHANGED).onChanged(connectionConfigUuid)
    }
  }

  /**
   * Clear user credentials by connection config UUID.
   * @see CredentialService.clearCredentials
   */
  override fun clearCredentials(connectionConfigUuid: String) {
    val credentialAttributes = createCredentialAttributes(USER_PASSWORD_SUB, connectionConfigUuid)
    val tokenCredentialAttributes = createCredentialAttributes(USER_TOKEN_SUB, connectionConfigUuid)
    getPasswordSafeService().set(credentialAttributes, null)
    getPasswordSafeService().set(tokenCredentialAttributes, null)
  }

}
