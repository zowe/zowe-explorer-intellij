package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.util.ui.ColumnInfo

@Suppress("DialogTitleCapitalization")
class ConnectionUrlColumn : ColumnInfo<ConnectionDialogState, String>("z/OSMF URL") {

  override fun valueOf(item: ConnectionDialogState): String {
    return item.connectionUrl
  }

  override fun setValue(item: ConnectionDialogState, value: String) {
    item.connectionUrl = value
  }

}