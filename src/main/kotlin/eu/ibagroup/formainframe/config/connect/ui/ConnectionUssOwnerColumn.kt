/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.util.ui.ColumnInfo
import eu.ibagroup.formainframe.config.connect.ui.renderer.UssOwnerColumnRenderer
import javax.swing.table.TableCellRenderer

/**
 * Class which represents column of USS Owner in connections GUI
 */
class ConnectionUssOwnerColumn<ConnectionState : ConnectionDialogStateBase<*>>
  : ColumnInfo<ConnectionState, String>("Owner") {

  /**
   * Returns name of particular owner
   * @param item all info about particular connection to mainframe
   * @return name of particular owner
   */
  override fun valueOf(item: ConnectionState): String {
    return item.owner
  }

  /**
   * Sets owner to particular user
   * @param item all info about particular connection to mainframe
   * @param value new owner of the user
   */
  override fun setValue(item: ConnectionState, value: String) {
    item.owner = value
  }

  /**
   * Specifies a renderer for this cell
   */
  override fun getRenderer(o: ConnectionState): TableCellRenderer {
    return UssOwnerColumnRenderer()
  }

}