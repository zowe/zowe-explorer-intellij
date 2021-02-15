package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.util.ui.ColumnInfo


class ConnectionNameColumn : ColumnInfo<ConnectionDialogState, String>("Name") {

  override fun valueOf(item: ConnectionDialogState): String {
    return item.connectionName
  }

  override fun setValue(item: ConnectionDialogState, value: String) {
    item.connectionName = value
  }

}