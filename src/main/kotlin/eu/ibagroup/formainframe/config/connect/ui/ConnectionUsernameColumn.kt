package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.util.ui.ColumnInfo

class ConnectionUsernameColumn : ColumnInfo<ConnectionDialogState, String>("Username") {

  override fun valueOf(item: ConnectionDialogState): String {
    return if (item.zoweConfigPath == null) item.username else "*".repeat(item.username.length)
  }

  override fun setValue(item: ConnectionDialogState, value: String) {
    item.username = value
  }

}
