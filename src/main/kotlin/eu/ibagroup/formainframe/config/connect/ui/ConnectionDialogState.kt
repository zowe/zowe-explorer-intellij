package eu.ibagroup.formainframe.config.connect.ui

import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.getByForeignKey
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.crudable.nextUniqueValue
import eu.ibagroup.r2z.CodePage
import eu.ibagroup.r2z.annotations.ZVersion

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
    override var mode: DialogMode = DialogMode.CREATE
) : DialogState, Cloneable {

  var connectionConfig
    get() = ConnectionConfig(connectionUuid, connectionName, connectionUrl, isAllowSsl, codePage, zVersion)
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
      codePage = this.codePage,
      zVersion = this.zVersion
  )
}