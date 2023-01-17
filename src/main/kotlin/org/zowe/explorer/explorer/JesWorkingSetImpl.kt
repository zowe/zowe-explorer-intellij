/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.zowe.explorer.config.configCrudable
import org.zowe.explorer.config.ws.JobsFilter
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.utils.clone

/** JES working set implementation */
class JesWorkingSetImpl(
  override val uuid: String,
  jesExplorer: AbstractExplorerBase<JesWorkingSetImpl, JesWorkingSetConfig>,
  workingSetConfigProvider: (String) -> JesWorkingSetConfig?,
  parentDisposable: Disposable
) : WorkingSetBase<JobsFilter, WorkingSet<*>, JesWorkingSetConfig>(
  uuid,
  jesExplorer,
  workingSetConfigProvider
), JesWorkingSet {
  override val wsConfigClass = JesWorkingSetConfig::class.java

  override fun JesWorkingSetConfig.masks(): MutableCollection<JobsFilter> = this.jobsFilters

  init {
    Disposer.register(parentDisposable, this)
  }

  /**
   * Remove JES filter from the config
   * @param jobsFilter the filter to delete
   */
  override fun removeFilter(jobsFilter: JobsFilter) {
    val newWsConfig = workingSetConfig?.clone() ?: return
    if (newWsConfig.jobsFilters.remove(jobsFilter)) {
      configCrudable.update(newWsConfig)
    }
  }
}
