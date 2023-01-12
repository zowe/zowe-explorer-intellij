/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.ws.ui.jes

import eu.ibagroup.formainframe.config.ws.JesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.AbstractWsTableModel
import eu.ibagroup.formainframe.utils.crudable.Crudable

/**
 * Table model for JES Working Set configuration table.
 * @see AbstractWsTableModel
 * @author Valiantsin Krus
 */
class JesWsTableModel(crudable: Crudable) : AbstractWsTableModel<JesWorkingSetConfig>(crudable) {

  override fun set(row: Int, item: JesWorkingSetConfig) {
    get(row).jobsFilters = item.jobsFilters
    super.set(row, item)
  }

  override val clazz = JesWorkingSetConfig::class.java

  init {
    initialize()
  }

}
