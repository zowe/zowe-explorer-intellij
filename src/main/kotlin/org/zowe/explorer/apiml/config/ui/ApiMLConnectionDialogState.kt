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

package org.zowe.explorer.apiml.config.ui

import org.zowe.explorer.apiml.config.ApiMLConnectionConfig
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.config.connect.Credentials
import org.zowe.explorer.config.connect.ui.ConnectionDialogStateBase
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.nextUniqueValue
import org.zowe.kotlinsdk.annotations.ZVersion

const val DEFAULT_GATEWAY_PATH = "/gateway/api/v1"

data class ApiMLConnectionDialogState(
  override var connectionUuid: String = "",
  override var connectionName: String = "",
  override var connectionUrl: String = "",
  override var username: String = "",
  override var password: String = "",
  override var owner: String = "",
  var isAllowSelfSigned: Boolean = false,
  var zVersion: ZVersion = ZVersion.ZOS_2_1,
  var zoweConfigPath: String? = null,
  var basePath: String = "",
  var gatewayPath: String = DEFAULT_GATEWAY_PATH,
//  var saveCredentials: Boolean = false,
  override var mode: DialogMode = DialogMode.CREATE
) : ConnectionDialogStateBase<ApiMLConnectionConfig>() {

  override var connectionConfig
    get() = ApiMLConnectionConfig(
      connectionUuid,
      connectionName,
      "$connectionUrl$basePath",
      isAllowSelfSigned,
      zVersion,
      zoweConfigPath,
      owner,
      gatewayPath
    )
    set(value) {
      connectionUuid = value.uuid
      connectionName = value.name
      connectionUrl = value.url
      isAllowSelfSigned = value.isAllowSelfSigned
      zVersion = value.zVersion
      owner = value.owner
      gatewayPath = value.gatewayPath
    }

  override var credentials
    get() = Credentials(connectionUuid, username, password)
    set(value) {
      username = value.username
      password = value.password
    }

  public override fun clone(): ApiMLConnectionDialogState {
    return this.copy()
  }

  fun initEmptyUuids(crudable: Crudable): ApiMLConnectionDialogState {
    this.connectionUuid = crudable.nextUniqueValue<ApiMLConnectionConfig, String>()
    return this
  }

}