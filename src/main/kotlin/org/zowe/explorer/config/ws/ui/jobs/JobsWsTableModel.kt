/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.ws.ui.jobs

import org.zowe.explorer.config.ws.JobsWorkingSetConfig
import org.zowe.explorer.config.ws.ui.AbstractWsTableModel
import org.zowe.explorer.utils.crudable.Crudable

/**
 * Table model for Jobs Working Set configuration table.
 * @see AbstractWsTableModel
 * @author Valiantsin Krus
 */
class JobsWsTableModel(crudable: Crudable) : AbstractWsTableModel<JobsWorkingSetConfig>(crudable) {

  override fun set(row: Int, item: JobsWorkingSetConfig) {
    get(row).jobsFilters = item.jobsFilters
    super.set(row, item)
  }

  override val clazz = JobsWorkingSetConfig::class.java

  init {
    initialize()
  }

}
