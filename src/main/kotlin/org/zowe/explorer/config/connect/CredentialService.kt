/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.connect

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic
import org.zowe.explorer.dataops.exceptions.CredentialsNotFoundForConnection
import okhttp3.Credentials

/**
 * Interface which represents objects that track changes in credentials
 */
fun interface CredentialsListener {
  /**
   * Does something when credentials were changed
   * @param connectionConfigUuid id of connection config
   */
  fun onChanged(connectionConfigUuid: String)
}

@JvmField
val CREDENTIALS_CHANGED = Topic.create("credentialChanges", CredentialsListener::class.java)

/**
 * Interface which represents objects which perform action on credentials
 * stored in connection config
 */
interface CredentialService {

  /**
   * Returns username of particular connection
   * @param connectionConfigUuid id of connection config
   * @return username of config
   */
  fun getUsernameByKey(connectionConfigUuid: String): String?

  /**
   * Returns password of particular connection
   * @param connectionConfigUuid id of connection config
   * @return password of config
   */
  fun getPasswordByKey(connectionConfigUuid: String): String?

  /**
   * Sets user and password for particular connection config
   * @param connectionConfigUuid id of connection config
   * @param username username to set in config
   * @param password password to set in config
   */
  fun setCredentials(connectionConfigUuid: String, username: String, password: String)

  /**
   * Resets user and password in particular connection config
   * @param connectionConfigUuid id of connection config
   */
  fun clearCredentials(connectionConfigUuid: String)

  companion object {
    @JvmStatic
    val instance: CredentialService
      get() = ApplicationManager.getApplication().getService(CredentialService::class.java)
  }

}

/**
 * Returns instance of credentials service
 * which perform actions on credentials stored in connection config
 */
fun getInstance(): CredentialService = ApplicationManager.getApplication().getService(CredentialService::class.java)

/**
 * Returns username of particular connection config.
 * @param connectionConfig connection config instance.
 * @return username of connection config.
 */
fun <Connection: ConnectionConfigBase> getUsername(connectionConfig: Connection): String {
  return CredentialService.instance.getUsernameByKey(connectionConfig.uuid) ?: throw CredentialsNotFoundForConnection(
    connectionConfig
  )
}

/**
 * Returns password of particular connection config.
 * @param connectionConfig connection config instance.
 * @return password of particular connection config.
 */
fun <Connection: ConnectionConfigBase> getPassword(connectionConfig: Connection): String {
  return CredentialService.instance.getPasswordByKey(connectionConfig.uuid) ?: throw CredentialsNotFoundForConnection(
    connectionConfig
  )
}

val ConnectionConfig.authToken: String
  get() = Credentials.basic(getUsername(this), getPassword(this))
