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

package eu.ibagroup.formainframe.tso.config.ui.table

import com.intellij.util.ui.ColumnInfo
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.tso.config.ui.TSOSessionDialogState
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.find
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey

/**
 * Class represents a connection name column in TSO session table model.
 * It extends ColumnInfo abstract class and overloads getter and setter methods as values for this column
 */
class ConnectionNameColumn(
  private val crudable: Crudable
) : ColumnInfo<TSOSessionDialogState, String>("Connection Name") {

  /**
   * Overloaded getter method of ColumnInfo abstract class
   */
  override fun valueOf(item: TSOSessionDialogState): String {
    return crudable.getByUniqueKey<ConnectionConfig>(item.connectionConfigUuid)?.name ?: ""
  }

  /**
   * Overloaded setter method of ColumnInfo abstract class
   */
  override fun setValue(item: TSOSessionDialogState, value: String) {
    crudable.find<ConnectionConfig> { it.name == value }.findAny().ifPresent {
      item.connectionConfigUuid = it.uuid
    }
  }

}
