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
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.config.ws.JobsWorkingSetConfig

/** JES working set implementation */
class JesWorkingSetImpl(
  override val uuid: String,
  jesExplorer: AbstractExplorerBase<JesWorkingSetImpl, JobsWorkingSetConfig>,
  workingSetConfigProvider: (String) -> JobsWorkingSetConfig?,
  parentDisposable: Disposable
) : WorkingSetBase<JobsFilter, WorkingSet<*>, JobsWorkingSetConfig>(
  uuid,
  jesExplorer,
  workingSetConfigProvider
), JesWorkingSet {
  override val wsConfigClass = JobsWorkingSetConfig::class.java

  override fun JobsWorkingSetConfig.masks(): MutableCollection<JobsFilter> = this.jobsFilters

  init {
    Disposer.register(parentDisposable, this)
  }
}
