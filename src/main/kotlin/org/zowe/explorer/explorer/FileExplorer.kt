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
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.utils.crudable.getByUniqueKey
import org.zowe.explorer.utils.rwLocked
import java.util.stream.Collectors

const val EXPLORER_NOTIFICATION_GROUP_ID = "org.zowe.explorer.explorer.ExplorerNotificationGroup"

class FileExplorerFactory : ExplorerFactory<ConnectionConfig, FilesWorkingSetImpl, FileExplorer> {
  override fun buildComponent(): FileExplorer = FileExplorer()
}

/** USS files and datasets explorer implementation */
class FileExplorer : AbstractExplorerBase<ConnectionConfig, FilesWorkingSetImpl, FilesWorkingSetConfig>() {
  override fun FilesWorkingSetConfig.toUnit(parentDisposable: Disposable): FilesWorkingSetImpl {
    return FilesWorkingSetImpl(
      uuid = uuid,
      fileExplorer = this@FileExplorer,
      workingSetConfigProvider = { ConfigService.getService().crudable.getByUniqueKey(it) },
      parentDisposable = parentDisposable
    )
  }

  override val unitClass = FilesWorkingSetImpl::class.java
  override val unitConfigClass = FilesWorkingSetConfig::class.java

  override val units by rwLocked(
    ConfigService
      .getService()
      .crudable
      .getAll(unitConfigClass)
      .map { it.toUnit(disposable) }
      .collect(Collectors.toSet())
      .toMutableSet(),
    lock
  )

  init {
    doInit()
  }
}
