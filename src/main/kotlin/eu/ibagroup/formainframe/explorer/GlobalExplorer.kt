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
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import java.util.stream.Collectors

const val EXPLORER_NOTIFICATION_GROUP_ID = "eu.ibagroup.formainframe.explorer.ExplorerNotificationGroup"

class GlobalExplorerFactory : ExplorerFactory<GlobalFilesWorkingSet, GlobalExplorer> {
  override fun buildComponent(): GlobalExplorer = GlobalExplorer()
}

class GlobalExplorer : AbstractExplorerBase<GlobalFilesWorkingSet, FilesWorkingSetConfig>() {
  override fun FilesWorkingSetConfig.toUnit(parentDisposable: Disposable): GlobalFilesWorkingSet {
    return GlobalFilesWorkingSet(
      uuid = uuid,
      globalExplorer = this@GlobalExplorer,
      workingSetConfigProvider = { configCrudable.getByUniqueKey(it) },
      parentDisposable = parentDisposable
    )
  }

  override val unitClass = GlobalFilesWorkingSet::class.java
  override val unitConfigClass = FilesWorkingSetConfig::class.java

  override val units by rwLocked(
    configCrudable.getAll(unitConfigClass).map { it.toUnit(disposable) }.collect(Collectors.toSet()).toMutableSet(),
    lock
  )

  init {
    doInit()
  }
}
