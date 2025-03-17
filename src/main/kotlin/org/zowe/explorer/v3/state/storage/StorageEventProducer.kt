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

package org.zowe.explorer.v3.state.storage

import org.zowe.explorer.v3.getListenerEndpoint
import org.zowe.explorer.v3.state.config.*
import org.zowe.explorer.v3.state.settings.*

/**
 * Config storage event producer.
 * Produces events for the [ConfigEventListener] and [OtherSettingsEventListener]
 */
@OptIn(StableStorage::class)
class StorageEventProducer : ConfigEventProducer, OtherSettingsEventProducer {
  /** @see [ConfigEventProducer.onRegister] */
  override fun onRegister(configType: ConfigType) {
    getListenerEndpoint(StorageService.STORAGE_CONFIGS_TOPIC).registered(configType)
  }

  /** @see [ConfigEventProducer.onAdd] */
  override fun onAdd(config: Config) {
    getListenerEndpoint(StorageService.STORAGE_CONFIGS_TOPIC).added(config)
  }

  /** @see [ConfigEventProducer.onUpdate] */
  override fun onUpdate(oldConfig: Config, newConfig: Config) {
    getListenerEndpoint(StorageService.STORAGE_CONFIGS_TOPIC).updated(oldConfig, newConfig)
  }

  /** @see [ConfigEventProducer.onDelete] */
  override fun onDelete(config: Config) {
    getListenerEndpoint(StorageService.STORAGE_CONFIGS_TOPIC).deleted(config)
  }

  /** @see [ConfigEventProducer.onReload] */
  override fun onReload(configType: ConfigType, reloadedConfigs: List<Config>) {
    getListenerEndpoint(StorageService.STORAGE_CONFIGS_TOPIC).reloaded(configType, reloadedConfigs)
  }

  /** @see [OtherSettingsEventProducer.onOtherSettingsReload] */
  override fun onOtherSettingsReload(newSettings: OtherSettingsHolder) {
    getListenerEndpoint(StorageService.OTHER_SETTINGS_TOPIC).otherSettingsReloaded(newSettings)
  }

  /** @see [OtherSettingsEventProducer.onOtherSettingsChange] */
  override fun onOtherSettingsChange(newSettings: OtherSettingsHolder) {
    getListenerEndpoint(StorageService.OTHER_SETTINGS_TOPIC).otherSettingsChanged(newSettings)
  }
}
