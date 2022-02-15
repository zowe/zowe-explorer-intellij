package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.ColumnInfo
import javax.swing.table.TableCellRenderer

class ConnectionUsernameColumn : ColumnInfo<ConnectionDialogState, String>("Username") {

  override fun valueOf(item: ConnectionDialogState): String {
    return item.username
  }

  override fun setValue(item: ConnectionDialogState, value: String) {
    item.username = value
  }

  override fun getRenderer(item: ConnectionDialogState?): TableCellRenderer? {
    return TableCellRenderer { _, _, _, _, _, _ ->
      item?.let {
        JBLabel(if (it.zoweConfigPath == null) it.username else "*".repeat(it.username.length))
      } ?: JBLabel("")
    }
  }
}
