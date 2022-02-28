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
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.UssPath
import org.zowe.explorer.utils.clone
import kotlin.concurrent.withLock

class GlobalFilesWorkingSet(
  override val uuid: String,
  globalExplorer: AbstractExplorerBase<GlobalFilesWorkingSet, FilesWorkingSetConfig>,
  private val workingSetConfigProvider: (String) -> FilesWorkingSetConfig?,
  parentDisposable: Disposable
) : WorkingSetBase<DSMask, WorkingSet<*>, FilesWorkingSetConfig>(
  uuid,
  globalExplorer,
  workingSetConfigProvider,
  parentDisposable
), FilesWorkingSet {

  override val wsConfigClass = FilesWorkingSetConfig::class.java

  override fun FilesWorkingSetConfig.masks() = this.dsMasks

  override val ussPaths: Collection<UssPath>
    get() = lock.withLock { workingSetConfig?.ussPaths ?: listOf() }

  override fun addUssPath(ussPath: UssPath) {
    val newWsConfig = workingSetConfig?.clone() ?: return
    if (newWsConfig.ussPaths.add(ussPath)) {
      configCrudable.update(newWsConfig)
    }
  }

  override fun removeUssPath(ussPath: UssPath) {
    val newWsConfig = workingSetConfig?.clone() ?: return
    if (newWsConfig.ussPaths.remove(ussPath)) {
      configCrudable.update(newWsConfig)
    }
  }

  init {
    Disposer.register(parentDisposable, this)
  }
}
