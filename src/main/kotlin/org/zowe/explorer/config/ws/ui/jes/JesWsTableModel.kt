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

package org.zowe.explorer.config.ws.ui.jes

import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.config.ws.ui.AbstractWsTableModel
import org.zowe.explorer.utils.crudable.Crudable

/**
 * Table model for JES Working Set configuration table.
 * @see AbstractWsTableModel
 * @author Valiantsin Krus
 */
class JesWsTableModel(crudable: Crudable)
  : AbstractWsTableModel<ConnectionConfig, JesWorkingSetConfig>(crudable, ConnectionConfig::class.java) {

  override fun set(row: Int, item: JesWorkingSetConfig) {
    get(row).jobsFilters = item.jobsFilters
    super.set(row, item)
  }

  override val clazz = JesWorkingSetConfig::class.java

  init {
    initialize()
  }

}
