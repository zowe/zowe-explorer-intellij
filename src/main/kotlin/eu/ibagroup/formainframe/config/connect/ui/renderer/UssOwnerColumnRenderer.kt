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

package eu.ibagroup.formainframe.config.connect.ui.renderer

import com.intellij.icons.AllIcons
import com.intellij.util.ui.table.IconTableCellRenderer
import javax.swing.Icon
import javax.swing.JTable
import java.awt.Component

const val WARNING_TOOLTIP_TEXT = "The last TSO request failed. Unable to get the real USS owner. The connection username will be used as USS owner"

/**
 * Renderer class for USS Owner column in the connections table view
 */
class UssOwnerColumnRenderer : IconTableCellRenderer<String>() {

  /**
   * Function returns a warning icon or null if the value is not present
   */
  override fun getIcon(value: String, table: JTable, row: Int): Icon? {
    return if(value.isEmpty()) AllIcons.General.Warning else null
  }

  /**
   * Function returns a component which renders a cell
   */
  override fun getTableCellRendererComponent(
    table: JTable?,
    value: Any?,
    selected: Boolean,
    focus: Boolean,
    row: Int,
    column: Int
  ): Component {
    super.getTableCellRendererComponent(table, value, selected, focus, row, column)
    if (icon != null) {
      toolTipText = WARNING_TOOLTIP_TEXT
    }
    return this
  }


}
