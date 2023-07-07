/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.ws.ui

import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ComboBoxCellEditor
import org.zowe.explorer.common.message
import org.zowe.explorer.config.connect.ConnectionConfigBase
import org.zowe.explorer.config.ws.WorkingSetConfig
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.find
import org.zowe.explorer.utils.crudable.getAll
import org.zowe.explorer.utils.crudable.getByUniqueKey
import org.zowe.explorer.utils.nullable
import org.zowe.explorer.utils.toMutableList
import javax.swing.table.TableCellEditor

/**
 * Class which represents working set connection name column in working set table model
 */
class WSConnectionNameColumn<Connection: ConnectionConfigBase, WSConfig : WorkingSetConfig>(private val crudable: Crudable, val connectionClass: Class<out Connection>) :
  ColumnInfo<WSConfig, String>(message("configurable.ws.tables.ws.connection.name")) {

  inner class ConnectionTableCellEditor : ComboBoxCellEditor() {
    override fun getComboBoxItems(): MutableList<String> {
      return crudable.getAll(connectionClass)
        .map { it.name }
        .toMutableList()
    }
  }

  override fun setValue(item: WSConfig, value: String) {
    crudable.find(connectionClass) { it.name == value }.findAny().nullable?.let {
      item.connectionConfigUuid = it.uuid
    }
  }

  override fun valueOf(item: WSConfig): String {
    return crudable.getByUniqueKey(connectionClass, item.connectionConfigUuid).nullable?.name ?: ""
  }

  override fun isCellEditable(item: WSConfig): Boolean {
    return false
  }

  override fun getEditor(item: WSConfig): TableCellEditor {
    return ConnectionTableCellEditor()
  }

  override fun getTooltipText(): String {
    return message("configurable.ws.tables.ws.connection.tooltip")
  }

}
