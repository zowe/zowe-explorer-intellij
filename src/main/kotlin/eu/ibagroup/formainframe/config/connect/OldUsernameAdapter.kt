/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.connect

import eu.ibagroup.formainframe.config.*
import eu.ibagroup.r2z.annotations.ZVersion
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Factory for creating adapter for username in old connection configs.
 */
class OldUsernameAdapterFactory : OldConfigAdapterFactory {
  override fun buildAdapter(document: Document): OldConfigAdapter<*> {
    return OldUsernameAdapter(document)
  }
}

/**
 * Implementation of OldConfigAdapter for username in connection.
 * Changes username from lowercase to uppercase.
 */
class OldUsernameAdapter(private val document: Document) : OldConfigAdapter<ConnectionConfig> {

  /**
   * @see OldConfigAdapter.configClass
   */
  override val configClass = ConnectionConfig::class.java

  /**
   * Username can be stored in lower case.
   * That's why it is necessary to find all tags with such username.
   * @return list of connection config elements in old config format.
   */
  private fun getOldConnectionElements(): List<Element> {
    return document.documentElement
      .getApplicationOption("connections")
      ?.get("list")
      ?.firstOrNull()
      ?.get("ConnectionConfig")
      ?.filter {
        CredentialService.instance.getUsernameByKey(it.getOptionValue("uuid"))?.let { username ->
          username != username.uppercase()
        } ?: false
      } ?: emptyList()
  }

  /**
   * @see OldConfigAdapter.getOldConfigsIds
   */
  override fun getOldConfigsIds(): List<String> {
    return getOldConnectionElements().map { it.getOptionValue("uuid") }
  }

  /**
   * @see OldConfigAdapter.castOldConfigs
   */
  override fun castOldConfigs(): List<ConnectionConfig> {
    return getOldConnectionElements().mapNotNull { connElement ->
      val uuid = connElement.getOptionValue("uuid")
      val name = connElement.getOptionValue("name")
      val url = connElement.getOptionValue("url")
      var isAllowSelfSign = connElement.getOptionValue("allowSelfSigned")
      if (isAllowSelfSign.isEmpty()) {
        isAllowSelfSign = true.toString()
      }
      val credentialService = CredentialService.instance
      val username = credentialService.getUsernameByKey(uuid)?.uppercase()  ?: ""
      val password = credentialService.getPasswordByKey(uuid) ?: ""

      if (uuid.isEmpty() || name.isEmpty() || url.isEmpty() || username.isEmpty() || password.isEmpty()) {
        null
      } else {
        credentialService.setCredentials(uuid, username, password)
        ConnectionConfig(uuid, name, url, isAllowSelfSign.toBoolean(), ZVersion.ZOS_2_1)
      }
    }
  }
}