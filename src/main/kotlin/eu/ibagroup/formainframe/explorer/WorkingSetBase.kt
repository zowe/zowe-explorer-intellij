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

import com.intellij.util.containers.orNull
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.runIfTrue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Working set base abstraction to describe all the basic interactions with working sets
 * @param uuid the UUID of the working set for config
 * @param definedExplorer the explorer object defined by an implementation
 */
abstract class WorkingSetBase<Connection : ConnectionConfigBase, MaskType, WSConfig : WorkingSetConfig>(
  override val uuid: String,
  definedExplorer: AbstractExplorerBase<Connection, out WorkingSet<Connection, *>, WSConfig>,
  private val workingSetConfigProvider: (String) -> WSConfig?,
) : WorkingSet<Connection, MaskType> {

  abstract val wsConfigClass: Class<out WSConfig>
  abstract val connectionConfigClass: Class<out Connection>

  override val explorer = definedExplorer

  private val isDisposed = AtomicBoolean(false)

  protected val lock = ReentrantLock()

  protected val workingSetConfig: WSConfig?
    get() = lock.withLock {
      (isDisposed.compareAndSet(false, false)).runIfTrue { workingSetConfigProvider(uuid) }
    }

  abstract fun WSConfig.masks(): MutableCollection<MaskType>

  override val masks: Collection<MaskType>
    get() = lock.withLock { workingSetConfig?.masks() ?: listOf() }

  override val name
    get() = workingSetConfig?.name ?: ""

  override val connectionConfig: Connection?
    get() = lock.withLock {
      workingSetConfig
        ?.let {
          return@withLock ConfigService.getService().crudable
            .getByForeignKey(it, connectionConfigClass)
            ?.orNull()
        }
    }

  /**
   * Add mask and update config
   * @param mask the mask to add
   */
  override fun addMask(mask: MaskType) {
    val newWsConfig = workingSetConfig?.clone(wsConfigClass) ?: return
    if (newWsConfig.masks().add(mask)) {
      ConfigService.getService().crudable.update(newWsConfig)
    }
  }

  /**
   * Remove mask and update config
   * @param mask the mask to remove
   */
  override fun removeMask(mask: MaskType) {
    val newWsConfig = workingSetConfig?.clone(wsConfigClass) ?: return
    if (newWsConfig.masks().remove(mask)) {
      ConfigService.getService().crudable.update(newWsConfig)
    }
  }

  override fun dispose() {
    isDisposed.set(true)
  }
}
