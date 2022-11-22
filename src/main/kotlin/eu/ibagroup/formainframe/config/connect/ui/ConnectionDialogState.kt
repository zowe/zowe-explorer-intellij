/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.connect.ui

import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.crudable.nextUniqueValue
import eu.ibagroup.r2z.annotations.ZVersion

/**
 * Data class which represents state for connection dialog
 */
data class ConnectionDialogState(
  var connectionUuid: String = "",
  var connectionName: String = "",
  var connectionUrl: String = "",
  /*var apiMeditationLayer: String = "",*/
  var username: String = "",
  var password: String = "",
  var isAllowSsl: Boolean = false,
  var zVersion: ZVersion = ZVersion.ZOS_2_1,
  override var mode: DialogMode = DialogMode.CREATE
) : DialogState, Cloneable {

  var connectionConfig
    get() = ConnectionConfig(connectionUuid, connectionName, connectionUrl, isAllowSsl, zVersion)
    set(value) {
      connectionUuid = value.uuid
      connectionName = value.name
      connectionUrl = value.url
      isAllowSsl = value.isAllowSelfSigned
      zVersion = value.zVersion
    }


  var credentials
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
      isAllowSsl = isAllowSsl
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
    zVersion = this.zVersion
  )
}