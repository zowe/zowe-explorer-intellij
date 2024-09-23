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

package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.JesWorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.rwLocked
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
    ConfigService.getService().crudable
      .getAll(unitConfigClass)
      .map { it.toUnit(disposable) }
      .collect(Collectors.toSet())
      .toMutableSet(),
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
      workingSetConfigProvider = { ConfigService.getService().crudable.getByUniqueKey(it) },
      parentDisposable = parentDisposable
    )
  }

  init {
    doInit()
  }
}
