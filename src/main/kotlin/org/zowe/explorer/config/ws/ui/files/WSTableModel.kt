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

package org.zowe.explorer.config.ws.ui.files

import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.ui.AbstractWsTableModel
import org.zowe.explorer.utils.crudable.Crudable

/**
 * Table model for Files Working Set configuration table.
 * @see AbstractWsTableModel
 * @author Valiantsin Krus
 * @author Viktar Mushtsin
 */
class WSTableModel(crudable: Crudable)
  : AbstractWsTableModel<ConnectionConfig, FilesWorkingSetConfig>(crudable, ConnectionConfig::class.java) {

  override val clazz = FilesWorkingSetConfig::class.java

  override operator fun set(row: Int, item: FilesWorkingSetConfig) {
    get(row).dsMasks = item.dsMasks
    get(row).ussPaths = item.ussPaths
    super.set(row, item)
  }

  init {
    initialize()
  }
}
