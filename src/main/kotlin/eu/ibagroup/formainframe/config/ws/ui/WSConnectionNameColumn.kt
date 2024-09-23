/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.config.ws.ui

import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ComboBoxCellEditor
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.*
import eu.ibagroup.formainframe.utils.nullable
import eu.ibagroup.formainframe.utils.toMutableList
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
