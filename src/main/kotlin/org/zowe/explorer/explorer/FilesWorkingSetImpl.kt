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

package org.zowe.explorer.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.UssPath
import org.zowe.explorer.utils.clone
import kotlin.concurrent.withLock

/** USS files and datasets working set implementation */
class FilesWorkingSetImpl(
  override val uuid: String,
  fileExplorer: AbstractExplorerBase<ConnectionConfig, FilesWorkingSetImpl, FilesWorkingSetConfig>,
  workingSetConfigProvider: (String) -> FilesWorkingSetConfig?,
  parentDisposable: Disposable
) : WorkingSetBase<ConnectionConfig, DSMask, FilesWorkingSetConfig>(
  uuid,
  fileExplorer,
  workingSetConfigProvider
), FilesWorkingSet {

  override val wsConfigClass = FilesWorkingSetConfig::class.java
  override val connectionConfigClass = ConnectionConfig::class.java

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
      ConfigService.getService().crudable.update(newWsConfig)
    }
  }

  /**
   * Remove USS path from the config
   * @param ussPath the USS path to delete
   */
  override fun removeUssPath(ussPath: UssPath) {
    val newWsConfig = workingSetConfig?.clone() ?: return
    if (newWsConfig.ussPaths.remove(ussPath)) {
      ConfigService.getService().crudable.update(newWsConfig)
    }
  }

  init {
    Disposer.register(parentDisposable, this)
  }
}
