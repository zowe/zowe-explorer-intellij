package eu.ibagroup.formainframe.config.ws.ui

import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ComboBoxCellEditor
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.find
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.findAnyNullable
import eu.ibagroup.formainframe.utils.toMutableList
import javax.swing.table.TableCellEditor

class WSConnectionNameColumn(private val crudable: Crudable) :
  ColumnInfo<WorkingSetConfig, String>(message("configurable.ws.tables.ws.connection.name")) {

  inner class ConnectionTableCellEditor : ComboBoxCellEditor() {
    override fun getComboBoxItems(): MutableList<String> {
      return crudable.getAll<ConnectionConfig>()
        .map { it.name }
        .toMutableList()
    }
  }

  override fun setValue(item: WorkingSetConfig, value: String) {
    crudable.find<ConnectionConfig> { it.name == value }.findAnyNullable()?.let {
      item.connectionConfigUuid = it.uuid
    }
  }

  override fun valueOf(item: WorkingSetConfig): String {
    return crudable.getByUniqueKey<ConnectionConfig>(item.connectionConfigUuid)?.name ?: ""
  }

  override fun isCellEditable(item: WorkingSetConfig): Boolean {
    return false
  }

  override fun getEditor(item: WorkingSetConfig): TableCellEditor {
    return ConnectionTableCellEditor()
  }

  override fun getTooltipText(): String {
    return message("configurable.ws.tables.ws.connection.tooltip")
  }

}