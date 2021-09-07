package eu.ibagroup.formainframe.config.ws.ui

import com.intellij.util.ui.ColumnInfo
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.common.ui.ErrorableTableCellRenderer
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import javax.swing.table.TableCellRenderer

private val NO_USERNAME_MESSAGE = message("configurable.ws.tables.ws.username.error.empty")

class WSUsernameColumn<WSConfig: WorkingSetConfig>(
  private val getUsername: (WSConfig) -> String?
) : ColumnInfo<WSConfig, String>(message("configurable.ws.tables.ws.username.name")) {

  override fun valueOf(item: WSConfig): String {
    return getUsername(item) ?: NO_USERNAME_MESSAGE
  }

  override fun getRenderer(item: WSConfig): TableCellRenderer {
    return ErrorableTableCellRenderer(
      errorMessage = message("configurable.ws.tables.ws.url.error.empty.tooltip")
    ) {
      getUsername(item) == null
    }
  }

  override fun isCellEditable(item: WSConfig?): Boolean {
    return false
  }

  override fun getTooltipText(): String {
    return message("configurable.ws.tables.ws.username.tooltip")
  }

}
