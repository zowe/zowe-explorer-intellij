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

import com.intellij.openapi.components.*
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.XmlSerializerUtil
import org.zowe.explorer.telemetry.NotificationCompatibleException
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.v3.state.config.*
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.config.ConfigEventListener
import org.zowe.explorer.v3.state.config.migration.doesXmlHasConfigs
import org.zowe.explorer.v3.state.config.migration.performCascadeMigration
import org.zowe.explorer.v3.state.config.migration.xmlToConfigs
import org.zowe.explorer.v3.state.settings.OtherSettingsEventListener
import org.zowe.explorer.v3.state.settings.OtherSettingsHolder
import org.zowe.explorer.v3.state.settings.OtherSettingsService
import org.jdom.Element
import java.util.stream.Stream

/**
 * AVOID WORKING WITH CONFIGS AND OTHER SETTINGS THROUGH THIS CLASS
 * @see [ConfigCacheService]
 * @see [OtherSettingsService]
 * Storage service to both read and write configs and other settings in XML format.
 * Stores stable configs and other settings states to later synchronize them with
 * XML config files and the config cache service
 */
@StableStorage
@Service
@State(
  name = "ForMainframeStorageService",
  storages = [Storage(value = "for-mainframe-config.xml", exportable = true)],
)
class StorageService : PersistentStateComponentWithModificationTracker<Element> {

  companion object {
    private val CONFIG_DECLARATOR_EP_NAME by lazy {
      ExtensionPointName.create<ConfigDeclarator>("org.zowe.explorer.configDeclarator")
    }
    val STORAGE_CONFIGS_TOPIC =
      Topic.create("StorageConfigEventListener", ConfigEventListener::class.java)
    val OTHER_SETTINGS_TOPIC =
      Topic.create("StorageOtherSettingsEventListener", OtherSettingsEventListener::class.java)

    @Volatile
    private var isServiceStable = true

    fun getService(): StorageService {
      return if (isServiceStable) service()
      else throw Exception("ConfigStorageService is not available due to issues appeared earlier")
    }

    /** Resets the service to the stable state so it could be used again */
    fun resetService() {
      isServiceStable = true
    }
  }

  @Volatile
  private var storageState: StorageState = StorageState()

  private val configDeclarators: List<ConfigDeclarator> by lazy { CONFIG_DECLARATOR_EP_NAME.extensionList }
  private val eventProducer = StorageEventProducer()
  private val configStateManager = ConfigStateManager(storageState)

  override fun getState(): Element {
    return XmlSerializer.serialize(storageState)
  }

  override fun getStateModificationCount() = storageState.modificationCount

  /**
   * Load storage state from XML configurations.
   * Performed at the start of the plug-in run and during plug-in's work when there are changes
   * in the XML files that come outside
   * @param state the XML state as an [Element] instance
   */
  override fun loadState(state: Element) {
    val savedConfigs: Element? = state.getChild("configs")

    // Simple settings fields
    val simpleState = state.apply { removeChild("configs") }
    val loadedState = XmlSerializer.deserialize(simpleState, StorageState::class.java)

    // Configs processing
    registerConfigTypes()

    // Parse configs
    val loadedConfigsByTypes = storageState.configs
      .mapValues { (configType, _) ->
        val xmlHasConfigs = doesXmlHasConfigs(savedConfigs, "$configType")
        val parsedConfigs = xmlToConfigs(savedConfigs, "$configType") { savedConfig ->
          XmlSerializer.deserialize(savedConfig, configType.toConfigClass())
        }
        if (xmlHasConfigs && parsedConfigs.size == 0) {
          isServiceStable = false
        }
        parsedConfigs
      }
      .toMutableMap()

    if (!isServiceStable) {
      NotificationsService
        .errorNotification(
          NotificationCompatibleException(
            "Error during parsing saved configs",
            "Check the correctness of the declared services and/or the .xml file being parsed"
          )
        )
      return
    }

    // Converting and merging recognized and unrecognized configs
    val savedConfigTypes = savedConfigs?.children
      ?.mapNotNull { configElem ->
        configElem.attributes
          .find { attr -> attr.name == "type" }
          ?.value
      }
      ?.distinct()
      ?: listOf()
    val registeredConfigTypes = getRegisteredConfigTypes()
    val unrecognizedConfigTypes = savedConfigTypes.filter { configTypeStr ->
      registeredConfigTypes.find { regConfigType -> configTypeStr == "$regConfigType" } == null
    }
    val conformedConfigsByTypes = if (unrecognizedConfigTypes.isNotEmpty()) {
      performCascadeMigration(loadedConfigsByTypes, savedConfigs)
    } else loadedConfigsByTypes

    // Final save
    XmlSerializerUtil.copyBean(loadedState, storageState)
    storageState.configs.clear()
    storageState.configs = conformedConfigsByTypes

    // Refresh all listeners with the storage values
    reloadOtherSettingsFromStorage()
    reloadConfigsFromStorage()
  }

  /**
   * Register a [configType] in the storage to be able to work with the configs of the related type
   * @param configType the config type to register
   */
  private fun registerConfigType(configType: ConfigType) {
    if (!storageState.configs.containsKey(configType)) {
      storageState.configs[configType] = mutableListOf()
    }
    eventProducer.onRegister(configType)
  }

  /** Register all config types available and registered in [configDeclarators] extension point */
  fun registerConfigTypes() {
    if (storageState.configs.isEmpty()) {
      configDeclarators.forEach { registerConfigType(it.configType) }
    }
  }

  /** Get all config types available and registered in [configDeclarators] extension point */
  fun getRegisteredConfigTypes(): List<ConfigType> {
    return configDeclarators.map { it.configType }
  }

  /** @see [ConfigStateManager.getConfigs] */
  fun getConfigsFromStorage(configType: ConfigType): Stream<Config> {
    return configStateManager.getConfigs(configType)
  }

  /** @see [ConfigStateManager.addConfig] */
  fun addConfigToStorage(newConfig: Config) {
    configStateManager.addConfig(newConfig)
      .onSuccess {
        storageState.modificationPerformed()
        eventProducer.onAdd(it)
      }
      .onFailure {
        NotificationsService.errorNotification(it)
      }
  }

  /** @see [ConfigStateManager.updateConfig] */
  fun updateConfigInStorage(newConfig: Config) {
    configStateManager.updateConfig(newConfig)
      .onSuccess { (oldConfig, newConfig) ->
        storageState.modificationPerformed()
        eventProducer.onUpdate(oldConfig, newConfig)
      }
      .onFailure {
        NotificationsService.errorNotification(it)
      }
  }

  /** @see [ConfigStateManager.deleteConfig] */
  fun deleteConfigFromStorage(config: Config) {
    configStateManager.deleteConfig(config)
      .onSuccess {
        storageState.modificationPerformed()
        eventProducer.onDelete(it)
      }
      .onFailure {
        NotificationsService.errorNotification(it)
      }
  }

  /**
   * Update the other settings element in the storage
   * @param newSettings the new other settings state to apply to the storage
   */
  fun updateSettingsInStorage(newSettings: OtherSettingsHolder) {
    val newSettingsAsXmlElement = XmlSerializer.serialize(newSettings)
    val newSettingsAsStorageState = XmlSerializer.deserialize(newSettingsAsXmlElement, StorageState::class.java)
    newSettingsAsStorageState.configs = storageState.configs
    XmlSerializerUtil.copyBean(newSettingsAsStorageState, storageState)
    eventProducer.onOtherSettingsChange(newSettings)
  }

  /** Produce an event to reload the other settings elements in the other settings state */
  fun reloadOtherSettingsFromStorage() {
    eventProducer.onOtherSettingsReload(storageState)
  }

  /** Produce events to reload each config type in the config cache state */
  fun reloadConfigsFromStorage() {
    storageState.configs.entries
      .forEach { (configType, loadedConfigs) ->
        eventProducer.onReload(configType, loadedConfigs)
      }
  }

}
