package eu.ibagroup.formainframe.config.connect.ui

import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.getByForeignKey
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.crudable.nextUniqueValue

data class ConnectionDialogState(
  var connectionUuid: String = "",
  var urlConnectionUuid: String = "",
  var connectionName: String = "",
  var connectionUrl: String = "",
  /*var apiMeditationLayer: String = "",*/
  var username: String = "",
  var password: String = "",
  var isAllowSsl: Boolean = false,
  override var mode: DialogMode = DialogMode.CREATE
) : DialogState, Cloneable {

  var connectionConfig
    get() = ConnectionConfig(connectionUuid, connectionName, urlConnectionUuid)
    set(value) {
      connectionUuid = value.uuid
      connectionName = value.name
      urlConnectionUuid = value.urlConnectionUuid
    }

  var urlConnection
    get() = UrlConnection(urlConnectionUuid, connectionUrl, isAllowSsl)
    set(value) {
      urlConnectionUuid = value.uuid
      connectionUrl = value.url
      isAllowSsl = value.isAllowSelfSigned
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
  this.urlConnectionUuid = crudable.nextUniqueValue<UrlConnection, String>()
  return this
}

fun ConnectionConfig.toDialogState(crudable: Crudable): ConnectionDialogState {
  val urlConnection = crudable.getByForeignKey(this) ?: UrlConnection().apply {
    uuid = crudable.nextUniqueValue<UrlConnection, String>()
  }
  val credentials = crudable.getByUniqueKey<Credentials>(this.uuid) ?: Credentials().apply {
    this.connectionConfigUuid = this@toDialogState.uuid
  }
  return ConnectionDialogState(
    connectionUuid = this.uuid,
    urlConnectionUuid = urlConnection.uuid,
    connectionName = this.name,
    connectionUrl = urlConnection.url,
    username = credentials.username,
    password = credentials.password,
    isAllowSsl = urlConnection.isAllowSelfSigned
  )
}