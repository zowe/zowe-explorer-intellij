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

import org.zowe.explorer.telemetry.NotificationCompatibleException
import org.zowe.explorer.utils.clone
import org.zowe.explorer.utils.filterNotNull
import org.zowe.explorer.utils.optional
import org.zowe.explorer.utils.streamOrEmpty
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.stream.Stream

/**
 * Config state manager to provide a control mechanism to the state handling
 * @property state the config state with [ConfigsHolder.configs] property
 */
class ConfigStateManager(private val state: ConfigsHolder) {

  private val lock = ReentrantReadWriteLock()

  /**
   * Return configs for the specified [configType]
   * @param configType the configs type to get the list of configs by
   * @return the list of configs in a [Stream] format
   */
  fun getConfigs(configType: ConfigType): Stream<Config> {
    lock.readLock().lock()
    return try {
      state.configs[configType]
        .streamOrEmpty()
        .filterNotNull()
        .map { it.clone(configType.toConfigClass()) }
    } finally {
      lock.readLock().unlock()
    }
  }

  /**
   * Add a new config as the [newConfig] instance. To add the config, it will search if there is already the same entry
   * present in the configs list. If it has one, the failure result will be returned. In the succeeding case,
   * the success result will be returned
   * @param newConfig the new config instance to add
   * @return [Result] instance with succeeded or failed result inside
   */
  fun addConfig(newConfig: Config): Result<Config> {
    lock.writeLock().lock()
    return try {
      val configsByType = state.configs[newConfig.configType]
      if (configsByType != null) {
        val isAlreadyExists = configsByType.find { it.uuid == newConfig.uuid } != null

        if (!isAlreadyExists) {
          configsByType.add(newConfig)
          Result.success(newConfig)
        } else {
          Result.failure(
            NotificationCompatibleException(
              "Error during a config add",
              "Config for type ${newConfig.configType} with UUID ${newConfig.uuid} is not added as it is already exist"
            )
          )
        }
      } else {
        Result.failure(
          NotificationCompatibleException(
            "Error during a config add",
            "Configs type ${newConfig.configType} is not registered"
          )
        )
      }
    } finally {
      lock.writeLock().unlock()
    }
  }

  /**
   * Replace all the configs in the state by the specified type
   * @param configType the config type to replace configs for
   * @param newConfigs the new configs list to replace the original list by
   * @return [Result] instance with succeeded or failed result inside
   */
  fun replaceConfigsByType(configType: ConfigType, newConfigs: List<Config>): Result<List<Config>> {
    lock.writeLock().lock()
    return try {
      val configsByType = state.configs[configType]
      if (configsByType != null) {
        configsByType.clear()
        configsByType.addAll(
          newConfigs
            .map { it.clone(configType.toConfigClass()) }
            .toMutableList()
        )
        Result.success(newConfigs)
      } else {
        Result.failure(
          NotificationCompatibleException(
            "Error during a configs replace",
            "Configs type $configType is not registered"
          )
        )
      }
    } finally {
      lock.writeLock().unlock()
    }
  }

  /**
   * Update a config with the [newConfig] instance. To update the config, it will search for the old entry
   * in the state's configs map. Also, it will check if the saved instance and the updating one are not the same.
   * In any of the failing cases, the failure result will be returned.
   * In the succeeding case, the success result will be returned with the pair of old and new config instances
   * @param newConfig the new config instance to update with
   * @return [Result] instance with succeeded or failed result inside
   */
  fun updateConfig(newConfig: Config): Result<Pair<Config, Config>> {
    lock.writeLock().lock()
    return try {
      val configsByType = state.configs[newConfig.configType]
      if (configsByType != null) {
        val oldConfigWithIndex = configsByType.withIndex().find { it.value.uuid == newConfig.uuid }.optional

        if (oldConfigWithIndex.isPresent) {
          val (index, oldConfig) = oldConfigWithIndex.get()
          if (oldConfig != newConfig) {
            configsByType[index] = newConfig
            Result.success(oldConfig to newConfig)
          } else {
            Result.failure(
              NotificationCompatibleException(
                "Error during a config update",
                "Config for type ${newConfig.configType} with UUID ${newConfig.uuid} is not updated " +
                  "as it is the same as the provided instance"
              )
            )
          }
        } else {
          Result.failure(
            NotificationCompatibleException(
              "Error during a config update",
              "Config for type ${newConfig.configType} with UUID ${newConfig.uuid} is not found for update"
            )
          )
        }
      } else {
        Result.failure(
          NotificationCompatibleException(
            "Error during a config update",
            "Configs type ${newConfig.configType} is not registered"
          )
        )
      }
    } finally {
      lock.writeLock().unlock()
    }
  }

  /**
   * Delete the [config] from the state's configs map. If not removed, the failure result is returned. If the operation
   * is succeeded, the original instance to delete is returned
   * @param config the config instance to delete
   * @return [Result] instance with succeeded or failed result inside
   */
  fun deleteConfig(config: Config): Result<Config> {
    lock.writeLock().lock()
    return try {
      val configsByType = state.configs[config.configType]
      if (configsByType != null) {
        if (configsByType.remove(config)) {
          Result.success(config)
        } else {
          Result.failure(
            NotificationCompatibleException(
              "Error during a config delete",
              "Config for type ${config.configType} with UUID ${config.uuid} is not found and thus not deleted"
            )
          )
        }
      } else {
        Result.failure(
          NotificationCompatibleException(
            "Error during a config delete",
            "Configs type ${config.configType} is not registered"
          )
        )
      }
    } finally {
      lock.writeLock().unlock()
    }
  }

}
