/*
 * Copyright (c) 2024 IBA Group.
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

package org.zowe.explorer.apiml.config

import org.zowe.explorer.apiml.config.ui.ApiMLConnectionDialogState
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.dataops.exceptions.CredentialsNotFoundForConnectionException
import org.zowe.kotlinsdk.annotations.ZVersion

class ApiMLConnectionConfig : ConnectionConfig {

//  var basePath: String = ""

  var gatewayPath: String = ""

  constructor()

  constructor(
    uuid: String,
    name: String,
    url: String,
    isAllowSelfSigned: Boolean,
    zVersion: ZVersion,
    zoweConfigPath: String? = null,
    owner: String = "",
//    basePath: String = "",
    gatewayPath: String = "",
  ) : super(uuid, name, url, isAllowSelfSigned, zVersion, zoweConfigPath, owner) {
//    this.basePath = basePath
    this.gatewayPath = gatewayPath
  }

  fun toDialogState(): ApiMLConnectionDialogState {
    var username = ""
    var password = ""
    var owner = ""
    try {
      username = CredentialService.getUsername(this)
      password = CredentialService.getPassword(this)
      owner = CredentialService.getOwner(this)
    } catch (_: CredentialsNotFoundForConnectionException) {
    }
    val pair = splitUrl(this.url)
    val baseUrl = pair.first
    val basePath = pair.second
    return ApiMLConnectionDialogState(
      connectionUuid = this.uuid,
      connectionName = this.name,
      connectionUrl = baseUrl,
      username = username,
      password = password,
      owner = owner,
      isAllowSelfSigned = this.isAllowSelfSigned,
      zVersion = this.zVersion,
      zoweConfigPath = this.zoweConfigPath,
      basePath = basePath,
      gatewayPath = this.gatewayPath
    )
  }

  private fun splitUrl(url: String): Pair<String, String> {
    val regex = Regex("((http|https)://[^:/]+:\\d+)(/.*)")
    val matchResult = regex.find(url)

    return matchResult?.let {
      val baseUrl = it.groups[1]?.value ?: ""
      val basePath = it.groups[3]?.value ?: ""
      Pair(baseUrl, basePath)
    } ?: Pair("", "")
  }

}
