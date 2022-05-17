/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.connect.ui

import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.common.ui.DialogState
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.Credentials
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.getByUniqueKey
import org.zowe.explorer.utils.crudable.nextUniqueValue
import org.zowe.kotlinsdk.CodePage
import org.zowe.kotlinsdk.annotations.ZVersion
import java.util.*

data class ConnectionDialogState(
    var connectionUuid: String = "",
    var connectionName: String = "",
    var connectionUrl: String = "",
    /*var apiMeditationLayer: String = "",*/
    var username: String = "",
    var password: String = "",
    var isAllowSsl: Boolean = false,
    var codePage: CodePage = CodePage.IBM_1047,
    var zVersion: ZVersion = ZVersion.ZOS_2_1,
    var zoweConfigPath: String? = null,
    override var mode: DialogMode = DialogMode.CREATE
) : DialogState, Cloneable {

  var connectionConfig
    get() = ConnectionConfig(connectionUuid, connectionName, connectionUrl, isAllowSsl, codePage, zVersion, zoweConfigPath)
    set(value) {
      connectionUuid = value.uuid
      connectionName = value.name
      connectionUrl = value.url
      isAllowSsl = value.isAllowSelfSigned
      codePage = value.codePage
      zVersion = value.zVersion
    }


  var credentials
    get() = Credentials(connectionUuid, username, password)
    set(value) {
      username = value.username
      password = value.password
    }

  override fun equals(other: Any?): Boolean {
    if (other == null) {
      return false
    }
    if (other === this) {
      return true
    }
    if (other !is ConnectionDialogState) {
      return false
    }
    return this.connectionConfig == other.connectionConfig && this.mode == other.mode
  }

  override fun hashCode(): Int = Objects.hash(connectionConfig, mode)

  public override fun clone(): ConnectionDialogState {
    return ConnectionDialogState(
        connectionUuid = connectionUuid,
        connectionName = connectionName,
        connectionUrl = connectionUrl,
        username = username,
        password = password,
        isAllowSsl = isAllowSsl,
        zoweConfigPath = zoweConfigPath
    )
  }
}

fun ConnectionDialogState.initEmptyUuids(crudable: Crudable): ConnectionDialogState {
  this.connectionUuid = crudable.nextUniqueValue<ConnectionConfig, String>()
  return this
}

fun ConnectionConfig.toDialogState(crudable: Crudable): ConnectionDialogState {
  val credentials = crudable.getByUniqueKey<Credentials>(this.uuid) ?: Credentials().apply {
    this.connectionConfigUuid = this@toDialogState.uuid
  }
  return ConnectionDialogState(
    connectionUuid = this.uuid,
    connectionName = this.name,
    connectionUrl = this.url,
    username = credentials.username,
    password = credentials.password,
    isAllowSsl = this.isAllowSelfSigned,
    codePage = this.codePage,
    zVersion = this.zVersion,
    zoweConfigPath = this.zoweConfigPath
  )
}
