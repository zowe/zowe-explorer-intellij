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

package org.zowe.explorer.config.connect.ui.zosmf

import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.connect.Credentials
import org.zowe.explorer.config.connect.ui.ConnectionDialogStateBase
import org.zowe.explorer.dataops.exceptions.CredentialsNotFoundForConnectionException
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.nextUniqueValue
import org.zowe.kotlinsdk.annotations.ZVersion
import java.util.*

/**
 * Data class which represents state for connection dialog
 */
data class ConnectionDialogState(
  override var connectionUuid: String = "",
  override var connectionName: String = "",
  override var connectionUrl: String = "",
  /*var apiMeditationLayer: String = "",*/
  override var username: String = "",
  override var password: CharArray = charArrayOf(),
  override var owner: String = "",
  var isAllowSsl: Boolean = false,
  var isAllowCleartext: Boolean = false,
  var zVersion: ZVersion = ZVersion.ZOS_2_1,
  var zoweConfigPath: String? = null,
  override var mode: DialogMode = DialogMode.CREATE
) : ConnectionDialogStateBase<ConnectionConfig>() {

  override var connectionConfig
    get() = ConnectionConfig(
      connectionUuid,
      connectionName,
      connectionUrl,
      isAllowSsl,
      zVersion,
      zoweConfigPath,
      owner,
      isAllowCleartext = isAllowCleartext
    )
    set(value) {
      connectionUuid = value.uuid
      connectionName = value.name
      connectionUrl = value.url
      isAllowSsl = value.isAllowSelfSigned
      isAllowCleartext = value.isAllowCleartext
      zVersion = value.zVersion
      owner = value.owner
    }


  override var credentials
    get() = Credentials(connectionUuid, username, password)
    set(value) {
      username = value.username
      password = value.password
    }

  public override fun clone(): ConnectionDialogState {
    return ConnectionDialogState(
      connectionUuid = connectionUuid,
      connectionName = connectionName,
      connectionUrl = connectionUrl,
      username = username,
      password = password,
      isAllowSsl = isAllowSsl,
      isAllowCleartext = isAllowCleartext,
      zoweConfigPath = zoweConfigPath,
      owner = owner
    )
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ConnectionDialogState) return false

    if (isAllowSsl != other.isAllowSsl) return false
    if (isAllowCleartext != other.isAllowCleartext) return false
    if (connectionUuid != other.connectionUuid) return false
    if (connectionName != other.connectionName) return false
    if (connectionUrl != other.connectionUrl) return false
    if (username != other.username) return false
    if (!password.contentEquals(other.password)) return false
    if (owner != other.owner) return false
    if (zVersion != other.zVersion) return false
    if (zoweConfigPath != other.zoweConfigPath) return false
    if (mode != other.mode) return false

    return true
  }

  override fun hashCode(): Int {
    var result = isAllowSsl.hashCode()
    result = 31 * result + isAllowCleartext.hashCode()
    result = 31 * result + connectionUuid.hashCode()
    result = 31 * result + connectionName.hashCode()
    result = 31 * result + connectionUrl.hashCode()
    result = 31 * result + username.hashCode()
    result = 31 * result + password.contentHashCode()
    result = 31 * result + owner.hashCode()
    result = 31 * result + zVersion.hashCode()
    result = 31 * result + (zoweConfigPath?.hashCode() ?: 0)
    result = 31 * result + mode.hashCode()
    return result
  }
}

fun ConnectionDialogState.initEmptyUuids(crudable: Crudable): ConnectionDialogState {
  this.connectionUuid = crudable.nextUniqueValue<ConnectionConfig, String>()
  return this
}

fun ConnectionConfig.toDialogState(crudable: Crudable): ConnectionDialogState {
  var username = ""
  var password = charArrayOf()
  var owner = ""
  try {
    username = CredentialService.getUsername(this)
    password = CredentialService.getPassword(this)
    owner = CredentialService.getOwner(this)
  } catch (_: CredentialsNotFoundForConnectionException) {
  }

// TODO: investigate the change
//  val credentials = crudable.getByUniqueKey<Credentials>(this.uuid) ?: Credentials().apply {
//    this.configUuid = this@toDialogState.uuid
//  }
  return ConnectionDialogState(
    connectionUuid = this.uuid,
    connectionName = this.name,
    connectionUrl = this.url,
    username = username,
    password = password,
    isAllowSsl = this.isAllowSelfSigned,
    isAllowCleartext = this.isAllowCleartext,
    zVersion = this.zVersion,
    zoweConfigPath = this.zoweConfigPath,
    owner = owner
  )
}
