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
import org.zowe.explorer.config.ws.JobsFilter
import org.zowe.explorer.config.ws.JobsWorkingSetConfig

class GlobalJesWorkingSet(
  override val uuid: String,
  globalExplorer: AbstractExplorerBase<GlobalJesWorkingSet, JobsWorkingSetConfig>,
  workingSetConfigProvider: (String) -> JobsWorkingSetConfig?,
  parentDisposable: Disposable
) : WorkingSetBase<JobsFilter, WorkingSet<*>, JobsWorkingSetConfig>(
  uuid,
  globalExplorer,
  workingSetConfigProvider,
  parentDisposable
), JesWorkingSet {
  override val wsConfigClass = JobsWorkingSetConfig::class.java

  override fun JobsWorkingSetConfig.masks(): MutableCollection<JobsFilter> = this.jobsFilters

  init {
    Disposer.register(parentDisposable, this)
  }
}
