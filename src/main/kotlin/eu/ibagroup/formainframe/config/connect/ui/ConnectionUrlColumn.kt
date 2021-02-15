package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.util.ui.ColumnInfo

class ConnectionUrlColumn : ColumnInfo<ConnectionDialogState, String>("zOSMF URL") {

  override fun valueOf(item: ConnectionDialogState): String {
    return item.connectionUrl
  }

  override fun setValue(item: ConnectionDialogState, value: String) {
    item.connectionUrl = value
  }

}