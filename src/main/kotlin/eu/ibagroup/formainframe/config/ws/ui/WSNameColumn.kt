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

import com.intellij.openapi.ui.ValidationInfo
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.common.ui.ValidatingCellRenderer
import eu.ibagroup.formainframe.common.ui.ValidatingColumnInfo
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import javax.swing.JComponent

/**
 * Class which represents working set name column in working set table model.
 */
class WSNameColumn<WSConfig : WorkingSetConfig>(private val wsProvider: () -> List<WSConfig>) :
  ValidatingColumnInfo<WSConfig>(message("configurable.ws.tables.ws.name")) {

  companion object {
    @JvmStatic
    private fun getDefaultError(component: JComponent) =
      ValidationInfo(message("configurable.ws.tables.ws.name.tooltip.error"), component)
  }

  /**
   * Validate working set name on input. WS name must be unique.
   * @param oldItem working set config [WorkingSetConfig].
   * @param newValue new working set name.
   * @param component validated ui component.
   * @return validation information or null if validation passed.
   */
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

  /**
   * Overloaded getter method. Gets the working set name from working set config.
   * @param item working set config [WorkingSetConfig].
   * @return current working set name.
   */
  override fun valueOf(item: WSConfig): String {
    return item.name
  }

  override fun isCellEditable(item: WSConfig?): Boolean {
    return false
  }

  /**
   * Overloaded setter method. Sets a new name in the working set config.
   * @param item working set config [WorkingSetConfig].
   * @param value new working set name.
   */
  override fun setValue(item: WSConfig, value: String) {
    item.name = value
  }

  /**
   * Gets the UI tooltip of the working set name column when mouse is hovered.
   */
  override fun getTooltipText(): String {
    return message("configurable.ws.tables.ws.name.tooltip")
  }

  /**
   * Validate entered working set name. WS name must be unique, not empty and not blank.
   * @param item working set config [WorkingSetConfig].
   * @param component validated ui component.
   * @return validation information or null if validation passed.
   */
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
