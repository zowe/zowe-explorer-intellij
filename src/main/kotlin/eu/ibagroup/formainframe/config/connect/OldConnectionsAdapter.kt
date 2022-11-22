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

/**
 * Factory for creating adapter for old connections.
 * @author Valentine Krus
 */
class OldConnectionsAdapterFactory : OldConfigAdapterFactory {
  override fun buildAdapter(document: Document): OldConfigAdapter<*> {
    return OldConnectionsAdapter(document)
  }
}

/**
 * Implementation of OldConfigAdapter for connections
 * Changes in new format:<br>
 * 1) All data from UrlConnection moved to ConnectionConfig;<br>
 * 2) UrlConnection removed.
 * @author Valentine Krus
 */
class OldConnectionsAdapter(private val document: Document) : OldConfigAdapter<ConnectionConfig> {

  /**
   * @see OldConfigAdapter.configClass
   */
  override val configClass = ConnectionConfig::class.java

  /**
   * @see OldConfigAdapter.getOldConfigsIds
   */
  override fun getOldConfigsIds(): List<String> {
    return document.documentElement
      .getApplicationOption("connections")
      ?.get("list")
      ?.firstOrNull()
      ?.get("ConnectionConfig")
      ?.filter { it.getOptionValue("urlConnectionUuid") != "" }
      ?.map { it.getOptionValue("urlConnectionUuid") }
      ?: emptyList()
  }

  /**
   * @see OldConfigAdapter.castOldConfigs
   */
  override fun castOldConfigs(): List<ConnectionConfig> {
    val connectionsList = document.documentElement
      .getApplicationOption("connections")?.get("list")?.firstOrNull() ?: return emptyList()
    val urlsList = document.documentElement
      .getApplicationOption("urls")?.get("list")?.firstOrNull() ?: return emptyList()

    val urlElements = urlsList["UrlConnection"]
    val oldConnectionElements =
      connectionsList["ConnectionConfig"].filter { it.getOptionValue("urlConnectionUuid") != "" }

    return oldConnectionElements.mapNotNull { connElement ->
      val oldUrlUuid = connElement.getOptionValue("urlConnectionUuid")
      val correspondingUrlElement = urlElements.firstOrNull { it.getOptionValue("uuid") == oldUrlUuid }
      if (correspondingUrlElement == null) {
        null
      } else {

        val uuid = connElement.getOptionValue("uuid")
        val name = connElement.getOptionValue("name")
        val url = correspondingUrlElement.getOptionValue("url")
        val isAllowSelfSign = correspondingUrlElement.getOptionValue("allowSelfSigned")
        val credentialService = CredentialService.instance
        val username = credentialService.getUsernameByKey(uuid) ?: credentialService.getUsernameByKey(oldUrlUuid) ?: ""
        val password = credentialService.getPasswordByKey(uuid) ?: credentialService.getPasswordByKey(oldUrlUuid) ?: ""

        if (uuid.isEmpty() || name.isEmpty() || url.isEmpty() || isAllowSelfSign.isEmpty() || username.isEmpty() || password.isEmpty()) {
          null
        } else {
          credentialService.setCredentials(uuid, username, password)
          ConnectionConfig(uuid, name, url, isAllowSelfSign.toBoolean(), ZVersion.ZOS_2_1)
        }
      }
    }
  }
}
