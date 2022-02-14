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
import org.zowe.explorer.config.configCrudable
import org.zowe.explorer.config.ws.JobsWorkingSetConfig
import org.zowe.explorer.utils.crudable.getByUniqueKey
import org.zowe.explorer.utils.rwLocked
import java.util.stream.Collectors


class JesExplorerFactory : ExplorerFactory<GlobalJesWorkingSet, JesExplorer> {
  override fun buildComponent(): JesExplorer = JesExplorer()
}

class JesExplorer() : AbstractExplorerBase<GlobalJesWorkingSet, JobsWorkingSetConfig>() {
  override val unitClass = GlobalJesWorkingSet::class.java
  override val unitConfigClass = JobsWorkingSetConfig::class.java

  override val units by rwLocked(
    configCrudable.getAll(unitConfigClass).map { it.toUnit(disposable) }.collect(Collectors.toSet()).toMutableSet(),
    lock
  )

  override fun JobsWorkingSetConfig.toUnit(parentDisposable: Disposable): GlobalJesWorkingSet {
    return GlobalJesWorkingSet(
      uuid = uuid,
      globalExplorer = this@JesExplorer,
      workingSetConfigProvider = { configCrudable.getByUniqueKey(it) },
      parentDisposable = parentDisposable
    )
  }

  init {
    doInit()
  }
}
