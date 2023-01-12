/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.config.ws.JesWorkingSetConfig
import eu.ibagroup.formainframe.utils.clone

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
