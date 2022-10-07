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
<<<<<<<< HEAD:src/main/kotlin/org/zowe/explorer/explorer/GlobalExplorer.kt
import org.zowe.explorer.config.configCrudable
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.utils.*
import org.zowe.explorer.utils.crudable.getByUniqueKey
========
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.rwLocked
>>>>>>>> release/v0.7.0:src/main/kotlin/org/zowe/explorer/explorer/FileExplorer.kt
import java.util.stream.Collectors

const val EXPLORER_NOTIFICATION_GROUP_ID = "org.zowe.explorer.explorer.ExplorerNotificationGroup"

class FileExplorerFactory : ExplorerFactory<FilesWorkingSetImpl, FileExplorer> {
  override fun buildComponent(): FileExplorer = FileExplorer()
}

/** USS files and datasets explorer implementation */
class FileExplorer : AbstractExplorerBase<FilesWorkingSetImpl, FilesWorkingSetConfig>() {
  override fun FilesWorkingSetConfig.toUnit(parentDisposable: Disposable): FilesWorkingSetImpl {
    return FilesWorkingSetImpl(
      uuid = uuid,
      fileExplorer = this@FileExplorer,
      workingSetConfigProvider = { configCrudable.getByUniqueKey(it) },
      parentDisposable = parentDisposable
    )
  }

  override val unitClass = FilesWorkingSetImpl::class.java
  override val unitConfigClass = FilesWorkingSetConfig::class.java

  override val units by rwLocked(
    configCrudable
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
