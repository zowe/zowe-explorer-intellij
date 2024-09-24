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

package org.zowe.explorer.config

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.options.BoundSearchableConfigurable
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.ZOSMFConnectionConfigDeclaration

/**
 * Factory to create instance of specific implementation of [ConfigDeclaration].
 * @author Valiantsin Krus
 */
interface ConfigDeclarationFactory {

  /** Creates instance of [ConfigDeclaration]. */
  fun buildConfigDeclaration(crudable: Crudable): ConfigDeclaration<*>
}

/**
 * Declares config class, that will be stored in persisted state [ConfigService].
 * @param T template parameter that specifies config class.
 * @property crudable instance of [Crudable] through which to work with config data.
 * @author Valiantsin Krus
 */
abstract class ConfigDeclaration<T: Any>(val crudable: Crudable) {

  /** Instance of connection config class */
  abstract val clazz: Class<out T>

  /**
   *  Identifies if the config is linked with credentials
   *  (for example see [ZOSMFConnectionConfigDeclaration], [ConnectionConfig])
   */
  open val useCredentials: Boolean = false

  /** This property is only needed to sort config tabs in UI in settings. */
  abstract val configPriority: Double

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<ConfigDeclarationFactory>("org.zowe.explorer.configDeclaration")
  }

  /**
   * Class to decide is it possible to modify config data (can the config class be added or updated)
   * @param Config template parameter that specifies config class.
   * @author Valiantsin Krus
   */
  abstract class ConfigDecider<Config> {

    /**
     * Specifies if the config instance can be added in config state.
     * @param row config instance to add.
     * @return true if the config class can be addede.
     */
    abstract fun canAdd(row: Config): Boolean

    /**
     * Specifies if the config instance can be added in config state.
     * @param currentRow current state of config instance.
     * @param updatingRow new state of config instance.
     * @return true if updatingRow can be applied or false otherwise.
     */
    abstract fun canUpdate(currentRow: Config, updatingRow: Config): Boolean
  }


  /** Provider decider in for config class. */
  abstract fun getDecider(): ConfigDecider<T>

  /** Builds configurable that will be displayed in settings. */
  open fun getConfigurable(): BoundSearchableConfigurable? = null
}
