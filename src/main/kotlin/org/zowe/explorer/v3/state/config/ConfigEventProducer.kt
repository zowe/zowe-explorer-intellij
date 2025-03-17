/*
 * Copyright (c) 2024 IBA Group.
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

package org.zowe.explorer.v3.state.config

/** Base interface to provide updates on any config specific action happened */
interface ConfigEventProducer {
  /**
   * Produce an event when a new config type is registered
   * @param configType the registered config type
   */
  fun onRegister(configType: ConfigType)

  /**
   * Produce an event when a new config is added
   * @param config the added config
   */
  fun onAdd(config: Config)

  /**
   * Produce an event when an old config is updated
   * @param oldConfig the old config that is changed
   * @param newConfig the new config that provides the updates
   */
  fun onUpdate(oldConfig: Config, newConfig: Config)

  /**
   * Produce an event when a config is deleted
   * @param config the deleted config
   */
  fun onDelete(config: Config)

  /**
   * Produce an event when configs by the [configType] are reloaded
   * @param configType the config type for configs that are reloaded
   * @param reloadedConfigs the configs that are reloaded
   */
  fun onReload(configType: ConfigType, reloadedConfigs: List<Config>)
}
