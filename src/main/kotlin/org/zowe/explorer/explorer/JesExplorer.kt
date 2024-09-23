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
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.utils.crudable.getByUniqueKey
import org.zowe.explorer.utils.rwLocked
import java.util.stream.Collectors


/** Factory to register [JesExplorer] in Intellij IoC container. */
class JesExplorerFactory : ExplorerFactory<ConnectionConfig, JesWorkingSetImpl, JesExplorer> {
  override fun buildComponent(): JesExplorer = JesExplorer()
}

/** JES Explorer implementation */
class JesExplorer : AbstractExplorerBase<ConnectionConfig, JesWorkingSetImpl, JesWorkingSetConfig>() {

  override val unitClass = JesWorkingSetImpl::class.java
  override val unitConfigClass = JesWorkingSetConfig::class.java

  override val units by rwLocked(
    configCrudable.getAll(unitConfigClass).map { it.toUnit(disposable) }.collect(Collectors.toSet()).toMutableSet(),
    lock
  )

  /**
   * Creates a unit class [JesWorkingSetImpl] from config class [JesWorkingSetConfig].
   * @param parentDisposable disposable parent.
   * @return desired instance of [JesWorkingSetImpl].
   */
  override fun JesWorkingSetConfig.toUnit(parentDisposable: Disposable): JesWorkingSetImpl {
    return JesWorkingSetImpl(
      uuid = uuid,
      jesExplorer = this@JesExplorer,
      workingSetConfigProvider = { configCrudable.getByUniqueKey(it) },
      parentDisposable = parentDisposable
    )
  }

  init {
    doInit()
  }
}
