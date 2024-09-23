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

package eu.ibagroup.formainframe.config.connect

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import eu.ibagroup.formainframe.utils.sendTopic

/**
 * Function to create credential attributes with own key.
 * @param key string key identifying credential attributes.
 * @return generated credential attributes [CredentialAttributes].
 */
private fun createCredentialAttributes(key: String): CredentialAttributes {
  return CredentialAttributes(generateServiceName("MySystem", key))
}

/**
 * Class that implements a credential service to work with user credentials.
 */
class CredentialServiceImpl : CredentialService {

  /**
   * Get user credentials by connection config UUID.
   * @param connectionConfigUuid connection configuration universally unique identifier.
   * @return user credentials [Credentials] if they are.
   */
  private fun getCredentials(connectionConfigUuid: String): Credentials? {
    return service<PasswordSafe>().get(createCredentialAttributes(connectionConfigUuid))
  }

  /**
   * Get username by connection config UUID.
   * @see CredentialService.getUsernameByKey
   */
  override fun getUsernameByKey(connectionConfigUuid: String): String? {
    val credentials = getCredentials(connectionConfigUuid)
    return credentials?.userName
  }

  /**
   * Get user password by connection config UUID.
   * @see CredentialService.getPasswordByKey
   */
  override fun getPasswordByKey(connectionConfigUuid: String): String? {
    val credentials = getCredentials(connectionConfigUuid)
    return credentials?.getPasswordAsString()
  }

  /**
   * Set user credentials.
   * @see CredentialService.setCredentials
   */
  override fun setCredentials(connectionConfigUuid: String, username: String, password: String) {
    val credentialAttributes = createCredentialAttributes(connectionConfigUuid)
    val credentials = Credentials(username, password)
    runBackgroundableTask("Setting user credentials") {
      service<PasswordSafe>().set(credentialAttributes, credentials)
      sendTopic(CREDENTIALS_CHANGED).onChanged(connectionConfigUuid)
    }
  }

  /**
   * Clear user credentials by connection config UUID.
   * @see CredentialService.clearCredentials
   */
  override fun clearCredentials(connectionConfigUuid: String) {
    val credentialAttributes = createCredentialAttributes(connectionConfigUuid)
    service<PasswordSafe>().set(credentialAttributes, null)
  }

}
