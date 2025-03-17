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

/** Config event listener base interface. Provides basic functionality to implement for modification listeners */
interface ConfigEventListener {

  /**
   * Handle the [ConfigEventProducer.onRegister] event
   * @param configType the registered config type
   */
  fun registered(configType: ConfigType)

  /**
   * Handle the [ConfigEventProducer.onAdd] event
   * @param config the added config
   */
  fun added(config: Config)

  /**
   * Handle the [ConfigEventProducer.onUpdate] event
   * @param oldConfig the old config that is changed
   * @param newConfig the new config that provided the updates
   */
  fun updated(oldConfig: Config, newConfig: Config)

  /**
   * Handle the [ConfigEventProducer.onDelete] event
   * @param config the deleted config
   */
  fun deleted(config: Config)

  /**
   * Handle the [ConfigEventProducer.onReload] event
   * @param configType the config type for configs that are reloaded
   * @param reloadedConfigs the configs that are reloaded
   */
  fun reloaded(configType: ConfigType, reloadedConfigs: List<Config>)

}
