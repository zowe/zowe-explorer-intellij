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

import com.intellij.openapi.components.service
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.dataops.exceptions.CredentialsNotFoundForConnectionException
import eu.ibagroup.formainframe.utils.runTask
import okhttp3.Credentials

const val USER_OR_OWNER_SYMBOLS_MAX_SIZE: Int = 8

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

  companion object {

    fun getService(): CredentialService = service()

    /**
     * Returns a username of a particular connection config or throws the [CredentialsNotFoundForConnectionException]
     * @param connectionConfig connection config instance to search username for
     * @return the username of the connection config
     */
    fun <ConnectionConfig : ConnectionConfigBase> getUsername(connectionConfig: ConnectionConfig): String {
      return getService().getUsernameByKey(connectionConfig.uuid)
        ?: throw CredentialsNotFoundForConnectionException(connectionConfig)
    }

    /**
     * Returns a password of a particular connection config or throws the [CredentialsNotFoundForConnectionException]
     * @param connectionConfig connection config instance to search password for
     * @return the password of the connection config
     */
    fun <ConnectionConfig : ConnectionConfigBase> getPassword(connectionConfig: ConnectionConfig): CharArray {
      return getService().getPasswordByKey(connectionConfig.uuid)
        ?: throw CredentialsNotFoundForConnectionException(connectionConfig)
    }

    /**
     * Returns owner of particular connection config if the owner field is not empty or conforms
     * the [USER_OR_OWNER_SYMBOLS_MAX_SIZE] in length, or the empty string otherwise.
     * If whoAmI function failed for some reason it could contain empty "" or error string inside owner variable.
     * If it is such case, then it returns an empty string.
     * @param connectionConfig connection config instance to get owner from
     * @return owner of the connection config if the owner is present, empty string otherwise
     */
    fun getOwner(connectionConfig: ConnectionConfig): String {
      val possibleOwner = connectionConfig.owner
      return if (possibleOwner.isNotEmpty() && possibleOwner.length <= USER_OR_OWNER_SYMBOLS_MAX_SIZE) {
        possibleOwner
      } else {
        ""
      }
    }

  }

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
  fun getPasswordByKey(connectionConfigUuid: String): CharArray?

  /**
   * Sets user and password for particular connection config
   * @param connectionConfigUuid id of connection config
   * @param username username to set in config
   * @param password password to set in config
   */
  fun setCredentials(connectionConfigUuid: String, username: String, password: CharArray)

  /**
   * Resets user and password in particular connection config
   * @param connectionConfigUuid id of connection config
   */
  fun clearCredentials(connectionConfigUuid: String)

}

val ConnectionConfig.authToken: String
  get() = runTask("Retrieving information for auth token") {
    val username = CredentialService.getUsername(this)
    val password = CredentialService.getPassword(this)
    Credentials.basic(username, String(password))
  }
