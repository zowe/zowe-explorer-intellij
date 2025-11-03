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

package org.zowe.explorer.v3.apiml.ui

import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.common.ui.DialogState
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.connect.Credentials
import org.zowe.explorer.dataops.exceptions.CredentialsNotFoundForConnectionException
import org.zowe.explorer.v3.SupportedSchemes
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.connection.ApiMlConnectionConfig
import org.zowe.explorer.v3.ui.table.getOwner
import org.zowe.kotlinsdk.annotations.ZVersion
import java.util.*

class ApiMlConnectionDialogState(
  var uuid: String = UUID.randomUUID().toString(),
  val configType: ConfigType = ConfigType.API_ML_CONNECTION_CONFIG_V1,
  var name: String = "",
  var scheme: SupportedSchemes = SupportedSchemes.HTTPS,
  var host: String = "",
  var port: Int = 443,
  var basePath: String = "/",
  var username: String = "",
  var password: CharArray = charArrayOf(),
  var zVersion: ZVersion = ZVersion.ZOS_2_3,
  var rejectUnauthorized: Boolean = true,
  var ussOwner: String = "",
  var gatewayPath: String = "/gateway/api/v1",
  override var mode: DialogMode = DialogMode.CREATE
) : DialogState {

  val apiMlConnectionConfig: ApiMlConnectionConfig
    get() = ApiMlConnectionConfig(
      uuid = uuid,
      name = name,
      scheme = scheme,
      host = host,
      port = port,
      zVersion = zVersion,
      ussOwner = ussOwner,
      basePath = basePath,
      rejectUnauthorized = rejectUnauthorized,
      gatewayPath = gatewayPath
    )

  var credentials: Credentials
    get() = Credentials(uuid, username, password)
    set(value) {
      username = value.username
      password = value.password
    }

}

fun ApiMlConnectionConfig.toDialogState(): ApiMlConnectionDialogState {
  var username = ""
  var password = charArrayOf()
  try {
    username = CredentialService.getService().getUsernameByKey(this.uuid) ?: ""
    password = CredentialService.getService().getPasswordByKey(this.uuid) ?: charArrayOf()
  } catch (_: CredentialsNotFoundForConnectionException) {
  }

  return ApiMlConnectionDialogState(
    uuid = this.uuid,
    configType = this.configType,
    name = this.name,
    scheme = this.scheme,
    host = this.host,
    port = this.port,
    basePath = this.basePath,
    username = username,
    password = password,
    zVersion = this.zVersion,
    rejectUnauthorized = this.rejectUnauthorized,
    ussOwner = getOwner(this.ussOwner),
    gatewayPath = this.gatewayPath
  )
}
