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
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.utils.clone
import kotlin.concurrent.withLock

/** USS files and datasets working set implementation */
class FilesWorkingSetImpl(
  override val uuid: String,
  fileExplorer: AbstractExplorerBase<FilesWorkingSetImpl, FilesWorkingSetConfig>,
  private val workingSetConfigProvider: (String) -> FilesWorkingSetConfig?,
  parentDisposable: Disposable
) : WorkingSetBase<DSMask, WorkingSet<*>, FilesWorkingSetConfig>(
  uuid,
  fileExplorer,
  workingSetConfigProvider,
  parentDisposable
), FilesWorkingSet {

  override val wsConfigClass = FilesWorkingSetConfig::class.java

  override fun FilesWorkingSetConfig.masks() = this.dsMasks

  override val ussPaths: Collection<UssPath>
    get() = lock.withLock { workingSetConfig?.ussPaths ?: listOf() }

  /**
   * Add USS path mask to the config
   * @param ussPath the USS path to add
   */
  override fun addUssPath(ussPath: UssPath) {
    val newWsConfig = workingSetConfig?.clone() ?: return
    if (newWsConfig.ussPaths.add(ussPath)) {
      configCrudable.update(newWsConfig)
    }
  }

  /**
   * Remove USS path from the config
   * @param ussPath the USS path to delete
   */
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
