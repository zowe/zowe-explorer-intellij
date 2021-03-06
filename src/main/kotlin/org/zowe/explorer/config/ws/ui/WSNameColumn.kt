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

import com.intellij.openapi.ui.ValidationInfo
import org.zowe.explorer.common.message
import org.zowe.explorer.common.ui.ValidatingCellRenderer
import org.zowe.explorer.common.ui.ValidatingColumnInfo
import org.zowe.explorer.config.ws.WorkingSetConfig
import javax.swing.JComponent
import javax.swing.JTable

class WSNameColumn<WSConfig : WorkingSetConfig>(private val wsProvider: () -> List<WSConfig>) :
  ValidatingColumnInfo<WSConfig>(message("configurable.ws.tables.ws.name")) {

  companion object {
    @JvmStatic
    private fun getDefaultError(component: JComponent) =
      ValidationInfo(message("configurable.ws.tables.ws.name.tooltip.error"), component)
  }

  override fun validateOnInput(oldItem: WSConfig, newValue: String, component: JComponent): ValidationInfo? {
    with(newValue.trim()) {
      return if ((oldItem.name == this && wsProvider().count { it.name == this } > 1)
        || (oldItem.name != this && wsProvider().any { it.name == this })) {
        getDefaultError(component)
      } else {
        null
      }
    }
  }

  override fun valueOf(item: WSConfig): String {
    return item.name
  }

  override fun isCellEditable(item: WSConfig?): Boolean {
    return false
  }

  override fun getWidth(table: JTable?): Int {
    return 200
  }

  override fun setValue(item: WSConfig, value: String) {
    item.name = value
  }

  override fun getTooltipText(): String {
    return message("configurable.ws.tables.ws.name.tooltip")
  }

  override fun validateEntered(item: WSConfig, component: JComponent): ValidationInfo? {
    return if (wsProvider().count { it.name == item.name } > 1) {
      getDefaultError(component)
    } else {
      when {
        item.name.isEmpty() -> ValidationInfo("Can't be empty", component)
        item.name.isBlank() -> ValidationInfo("Can't be blank", component)
        else -> null
      }
    }
  }

  override fun getValidatingCellRenderer(item: WSConfig): ValidatingCellRenderer<WSConfig> {
    return ValidatingCellRenderer()
  }
}
