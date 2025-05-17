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

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.messages.Topic
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.utils.*
import org.zowe.explorer.v3.state.config.*
import org.zowe.explorer.v3.state.storage.StableStorage
import org.zowe.explorer.v3.state.storage.StorageService
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * THIS IS THE PREFERRED WAY TO WORK WITH CONFIG INSTANCES
 * Config cache service to work with intermediate state of configs.
 * When changes are made and are ready to be saved, will synchronize the state to the config storage
 */
@Service
@OptIn(StableStorage::class)
class ConfigCacheService {
  companion object {
    val TOPIC = Topic.create("ConfigEventListener", ConfigEventListener::class.java)

    fun getService(): ConfigCacheService = service()
  }

  private val eventProducer = ConfigEventProducerImpl()
  private val stateLock = Any()

  /** State that could be reverted in the middle of changes without the changes being applied to the config storage */
  private var state = ConfigCacheState()
    get() = synchronized(stateLock) { field }
    private set(value) = synchronized(stateLock) { field = value }

  /** Config state manager to manipulate the [state] */
  private val configStateManager = ConfigStateManager(state)

  init {
    /**
     * The subscription model expects that modifiable state already has the consistent changes against the stable
     * storage. So whenever the basic event, related to the [StorageService], happens, it tries to update
     * the modifiable state without throwing the error notification if the changes are already there.
     * Also, some of the handlers are needed to reflect the initial load of the config state from storage.
     * To spread the updates across components, the respective event is produced with the updates to apply
     */
    subscribe(
      StorageService.STORAGE_CONFIGS_TOPIC,
      object : ConfigEventListener {
        /**
         * Register config types in the config state
         * @param configType the config type to register
         */
        override fun registered(configType: ConfigType) {
          synchronized(stateLock) {
            if (!state.configs.containsKey(configType)) {
              state.configs[configType] = mutableListOf()
              // TODO: log info that in config cache state a new config type is registered
              eventProducer.onRegister(configType)
            }
          }
        }

        /**
         * Reload configs from the storage.
         * Will produce the [eventProducer.onReload] event after the changes are applied to the cache
         * @param configType the config type to reload configs for
         * @param reloadedConfigs the configs from the storage to apply
         */
        override fun reloaded(configType: ConfigType, reloadedConfigs: List<Config>) {
          synchronized(stateLock) {
            configStateManager.replaceConfigsByType(configType, reloadedConfigs)
              .onSuccess {
                // TODO: log info that config cache state is reloaded
                eventProducer.onReload(configType, reloadedConfigs)
              }
              .onFailure {
                NotificationsService.errorNotification(it)
              }
          }
        }

        /**
         * Add a config from the storage if it is not added yet.
         * Will produce the [eventProducer.onAdd] event after the try of the changes apply to the cache
         * @param config the config to add to the cache if it is not there yet
         */
        override fun added(config: Config) {
          synchronized(stateLock) {
            configStateManager.addConfig(config)
              .onSuccess {
                // TODO: log warning that the config should already be added in cache
                eventProducer.onAdd(config)
              }
          }
        }

        /**
         * Update a config from the storage changes if it is not updated yet.
         * Will produce the [eventProducer.onUpdate] event after the try of the changes apply to the cache
         * @param oldConfig the old config to update in the cache if it is not updated yet
         * @param oldConfig the config changes to apply in the cache if they are not applied yet
         */
        override fun updated(oldConfig: Config, newConfig: Config) {
          synchronized(stateLock) {
            configStateManager.updateConfig(newConfig)
              .onSuccess {
                // TODO: log warning that the config should already be updated in cache
                eventProducer.onUpdate(oldConfig, newConfig)
              }
          }
        }

        /**
         * Delete a config from the cache if it is not deleted yet.
         * Will produce the [eventProducer.onDelete] event after the try of the changes apply to the cache
         * @param config the config to delete from the cache if it is not deleted yet
         */
        override fun deleted(config: Config) {
          synchronized(stateLock) {
            configStateManager.deleteConfig(config)
              .onSuccess {
                // TODO: log warning that the config should already be deleted from cache
                eventProducer.onDelete(config)
              }
          }
        }
      }
    )
  }

  /** Get all config types available and registered in [StorageService] */
  fun getRegisteredConfigTypes(): List<ConfigType> {
    return StorageService.getService().getRegisteredConfigTypes()
  }

  /**
   * Check whether the config cache state differs from the storage state
   * @param configType the config type to check the changes by
   * @return true if the cache state differs from the storage state
   */
  fun isCacheModified(configType: ConfigType): Boolean {
    return synchronized(stateLock) {
      val storageConfigs = StorageService.getService().getConfigsFromStorage(configType).toList()
      val cacheConfigs = getConfigsFromCache(configType).toList()
      val isModified = !(storageConfigs isTheSameAs cacheConfigs)
      isModified
    }
  }

  /**
   * Write the config cache state changes to the storage state if they differ
   * @param configType the config type to save the state for
   */
  fun saveCacheToStorage(configType: ConfigType) {
    synchronized(stateLock) {
      if (isCacheModified(configType)) {
        val storageConfigs = StorageService.getService()
          .getConfigsFromStorage(configType)
          .filterNotNull()
          .collect(Collectors.toList())
        val cacheConfigs = getConfigsFromCache(configType)
          .filterNotNull()
          .collect(Collectors.toList())

        val configsToDelete = storageConfigs
          .filter { oldConfig ->
            cacheConfigs.none { newConfig ->
              oldConfig.uuid == newConfig.uuid
            }
          }
        configsToDelete.forEach { config ->
          StorageService.getService()
            .deleteConfigFromStorage(config)
        }

        val configsToAdd = cacheConfigs
          .filter { newConfig ->
            storageConfigs.none { oldConfig ->
              oldConfig.uuid == newConfig.uuid
            }
          }
        configsToAdd.forEach { config ->
          StorageService.getService()
            .addConfigToStorage(config)
        }

        val configsToUpdate = cacheConfigs
          .filter { newConfig ->
            storageConfigs.any { oldConfig ->
              oldConfig.uuid == newConfig.uuid && oldConfig != newConfig
            }
          }
        configsToUpdate.forEach { config ->
          StorageService.getService()
            .updateConfigInStorage(config)
        }
      }
    }
  }

  /**
   * Get a config from cache by the [configType] and it's [uuid]
   * @param configType the config type to get config by
   * @param uuid the UUID to get config by
   */
  fun getConfigFromCache(configType: ConfigType, uuid: String): Config? {
    return getConfigsFromCache(configType).toList()
      .find { it.uuid == uuid }
  }

  /** @see [ConfigStateManager.getConfigs] */
  fun getConfigsFromCache(configType: ConfigType): Stream<Config> {
    return configStateManager.getConfigs(configType)
  }

  /** @see [ConfigStateManager.addConfig] */
  fun addConfigToCache(newConfig: Config) {
    configStateManager.addConfig(newConfig)
      .onSuccess { addedConfig ->
        eventProducer.onAdd(addedConfig)
      }
      .onFailure {
        NotificationsService.errorNotification(it)
      }
  }

  /** @see [ConfigStateManager.updateConfig] */
  fun updateConfigInCache(newConfig: Config) {
    configStateManager.updateConfig(newConfig)
      .onSuccess { (oldConfig, newConfigAdded) ->
        eventProducer.onUpdate(oldConfig, newConfigAdded)
      }
      .onFailure {
        NotificationsService.errorNotification(it)
      }
  }

  /** @see [ConfigStateManager.deleteConfig] */
  fun deleteConfigFromCache(config: Config) {
    configStateManager.deleteConfig(config)
      .onSuccess { deletedConfig ->
        eventProducer.onDelete(deletedConfig)
      }
      .onFailure {
        NotificationsService.errorNotification(it)
      }
  }

  /** Reload configs in cache from storage state  */
  fun reloadConfigsFromStorage() {
    StorageService.getService().reloadConfigsFromStorage()
  }

}
