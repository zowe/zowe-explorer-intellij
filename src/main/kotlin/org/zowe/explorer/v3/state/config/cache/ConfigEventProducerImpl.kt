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

package org.zowe.explorer.v3.state.config.cache

import org.zowe.explorer.v3.getListenerEndpoint
import org.zowe.explorer.v3.state.config.Config
import org.zowe.explorer.v3.state.config.ConfigEventProducer
import org.zowe.explorer.v3.state.config.ConfigEventListener
import org.zowe.explorer.v3.state.config.ConfigType

/** Config cache event producer. Produces events for the [ConfigEventListener] */
class ConfigEventProducerImpl : ConfigEventProducer {
  /** @see [ConfigEventProducer.onRegister] */
  override fun onRegister(configType: ConfigType) {
    getListenerEndpoint(ConfigCacheService.TOPIC).registered(configType)
  }

  /** @see [ConfigEventProducer.onAdd] */
  override fun onAdd(config: Config) {
    getListenerEndpoint(ConfigCacheService.TOPIC).added(config)
  }

  /** @see [ConfigEventProducer.onUpdate] */
  override fun onUpdate(oldConfig: Config, newConfig: Config) {
    getListenerEndpoint(ConfigCacheService.TOPIC).updated(oldConfig, newConfig)
  }

  /** @see [ConfigEventProducer.onDelete] */
  override fun onDelete(config: Config) {
    getListenerEndpoint(ConfigCacheService.TOPIC).deleted(config)
  }

  /** @see [ConfigEventProducer.onReload] */
  override fun onReload(configType: ConfigType, reloadedConfigs: List<Config>) {
    getListenerEndpoint(ConfigCacheService.TOPIC).reloaded(configType, reloadedConfigs)
  }
}
