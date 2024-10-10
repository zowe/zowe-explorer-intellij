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

package org.zowe.explorer.v3.state.config.migration

import com.intellij.util.xmlb.XmlSerializationException
import com.intellij.util.xmlb.XmlSerializer
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.telemetry.NotificationCompatibleException
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.tso.config.TSOSessionConfig
import org.zowe.explorer.utils.crudable.EntityWithUuid
import org.zowe.explorer.v3.SupportedSchemes
import org.zowe.explorer.v3.state.config.*
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig
import org.zowe.explorer.v3.state.config.files.DatasetMaskConfigItem
import org.zowe.explorer.v3.state.config.files.UssPathConfigItem
import org.zowe.explorer.v3.state.config.jes.JobFilterConfigItem
import org.zowe.explorer.v3.state.config.tso.TsoSessionConfig
import org.zowe.explorer.v3.state.settings.OtherSettingsService
import org.zowe.explorer.v3.state.storage.StorageState
import org.jdom.Element
import java.net.URL

/**
 * Get configs list from a defined XML structure
 * @param xmlStructure the structure to get configs from
 * @param configTypeStr the config type to get the configs from the list by
 * @return list of XML elements that are possibly convertible to the provided config type
 */
private fun getConfigsListFromXml(xmlStructure: Element?, configTypeStr: String): List<Element> {
  val xmlListTag = xmlStructure?.children
    ?.find { configElem ->
      configElem.attributes.find { attr -> attr.name == "type" && attr.value == configTypeStr } != null
    }
    ?.children
    ?: listOf()
  return if (xmlListTag.isEmpty()) listOf() else xmlListTag[0].children
}

/**
 * Check if XML configuration already has some configs to parse.
 * Is needed for further check if the configs are parsed correctly
 * @param savedConfigs the XML root elements to check if the configs are there
 * @param configTypeStr the config type string to check if the configs of this type are there
 */
fun doesXmlHasConfigs(savedConfigs: Element?, configTypeStr: String): Boolean {
  return getConfigsListFromXml(savedConfigs, configTypeStr).isNotEmpty()
}

/**
 * Transform saved configs in XML format to our config type
 * @param savedConfigs the saved configs in XML format to transform
 * @param configTypeStr the config type string to find configs to transform
 * @param deserializer the function that converts an XML element to our config type
 * @return a list of configs of our type
 */
fun xmlToConfigs(
  savedConfigs: Element?,
  configTypeStr: String,
  deserializer: (Element) -> Config
): MutableList<Config> {
  return getConfigsListFromXml(savedConfigs, configTypeStr)
    .map(deserializer)
    .toMutableList()
}

/**
 * Merge recognized and converted configs. Will modify unique values in migrated configs to be able to store them in
 * the right format in the config storage state
 * @param loadedConfigsByTypes the map of loaded configs by type
 * @param convertedConfigs the converted configs list to merge into the map by the corresponding type
 */
private fun mergeAsUnique(
  loadedConfigsByTypes: MutableMap<ConfigType, MutableList<Config>>,
  convertedConfigs: List<Config>
): MutableMap<ConfigType, MutableList<Config>> {
  return loadedConfigsByTypes.mapValues { (configType, loadedConfigs) ->
    val convertedConfigsCorrectType = convertedConfigs.filter { convertedConfig ->
      convertedConfig.configType == configType
    }
    // TODO: implement merging configs changing unique fields when necessary
    (loadedConfigs + convertedConfigsCorrectType) as MutableList
  } as MutableMap
}

/**
 * Perform cascade migration of the configs that are not in the enum of stable supported configs.
 * The migration will perform the actions based on the unrecognized config type. It will happen in a cascade way,
 * meaning that if we have v1 of some config, it will be converted to the v2 of the corresponding config, then v2 will
 * be converted to the v3, and so on. After that, all the changed configs will be merged in the stable configs lists
 * changing the unique values of the converted entities so that the values won't be duplicated in the list
 * @param loadedConfigsByTypes the already loaded configs to conform the converted entities with
 * @param savedConfigs the saved configs as the [Element] instance
 * @return the final map with conformed configs lists
 */
fun performCascadeMigration(
  loadedConfigsByTypes: MutableMap<ConfigType, MutableList<Config>>,
  savedConfigs: Element?
): MutableMap<ConfigType, MutableList<Config>> {
  // WORKING_SET_CONFIG_V1 -> FILES_WORKING_SET_CONFIG_V1
  val convertedConfigs = xmlToConfigs(savedConfigs, "WORKING_SET_CONFIG_V1") { savedConfig ->
    XmlSerializer.deserialize(savedConfig, WorkingSetConfigV1::class.java)
      .convertToNextVersion()
  }

  // Check that all the configs are converted to the correct config type
  convertedConfigs.forEach { convertedConfig ->
    if (convertedConfig.configType == ConfigType.UNKNOWN_CONFIG) {
      NotificationsService.errorNotification(
        NotificationCompatibleException(
          "Config ${convertedConfig.uuid} is not fully converted to the acceptable config type"
        )
      )
    }
  }

  // Conform configs
  return mergeAsUnique(loadedConfigsByTypes, convertedConfigs)
}

/**
 * Perform migration of the configs of the old type to the configs of the new type.
 * Is intended to reflect to the changes of configs of the old type, adding or updating the respective
 * items in the new config lists
 * @param configTypeToTransform the next config type to perform migration for
 */
private fun performOldConfigsTypeMigrationToNewConfigsType(configTypeToTransform: ConfigType) {
  fun connectionConfigToHttpConnectionConfig(oldEntityWithUuid: EntityWithUuid): HttpConnectionConfig {
    /**
     * Split URL string to three values: scheme, host and port
     * @param urlString the URL string to split
     */
    fun splitUrl(urlString: String): Triple<SupportedSchemes, String, Int> {
      val url = URL(urlString)
      val scheme = if (url.protocol == "https") SupportedSchemes.HTTPS else SupportedSchemes.HTTP
      val host = url.host
      val port = url.port
      return Triple(scheme, host, port)
    }

    val oldConfig = oldEntityWithUuid as ConnectionConfig
    val (scheme, host, port) = splitUrl(oldConfig.url)
    return HttpConnectionConfig(
      uuid = oldConfig.uuid,
      name = oldConfig.name,
      scheme = scheme,
      host = host,
      port = port,
      zVersion = oldConfig.zVersion,
      ussOwner = oldConfig.owner,
      rejectUnauthorized = !oldConfig.isAllowSelfSigned
    )
  }

  fun oldFilesWsConfigToNewFilesWsConfig(
    oldEntityWithUuid: EntityWithUuid
  ): org.zowe.explorer.v3.state.config.files.FilesWorkingSetConfig {
    val oldConfig = oldEntityWithUuid as FilesWorkingSetConfig
    return org.zowe.explorer.v3.state.config.files.FilesWorkingSetConfig(
      uuid = oldConfig.uuid,
      name = oldConfig.name,
      connectionConfigUuid = oldConfig.connectionConfigUuid,
      dsMasks = oldConfig.dsMasks
        .map { oldDsMask -> DatasetMaskConfigItem(oldDsMask.mask, oldDsMask.volser) }
        .toMutableList(),
      ussPaths = oldConfig.ussPaths
        .map { oldUssPath -> UssPathConfigItem(oldUssPath.path) }
        .toMutableList()
    )
  }

  fun oldJesWsConfigToNewJesWsConfig(
    oldEntityWithUuid: EntityWithUuid
  ): org.zowe.explorer.v3.state.config.jes.JesWorkingSetConfig {
    val oldConfig = oldEntityWithUuid as JesWorkingSetConfig
    return org.zowe.explorer.v3.state.config.jes.JesWorkingSetConfig(
      uuid = oldConfig.uuid,
      name = oldConfig.name,
      connectionConfigUuid = oldConfig.connectionConfigUuid,
      jobFilters = oldConfig.jobsFilters
        .map { oldJobFilter ->
          JobFilterConfigItem(oldJobFilter.owner, oldJobFilter.prefix, oldJobFilter.jobId)
        }
        .toMutableList()
    )
  }

  fun oldTsoSessionConfigToNewTsoSessionConfig(oldEntityWithUuid: EntityWithUuid): TsoSessionConfig {
    val oldConfig = oldEntityWithUuid as TSOSessionConfig
    return TsoSessionConfig(
      uuid = oldConfig.uuid,
      name = oldConfig.name,
      connectionConfigUuid = oldConfig.connectionConfigUuid,
      logonProcedure = oldConfig.logonProcedure,
      charset = oldConfig.charset,
      codepage = oldConfig.codepage,
      rows = oldConfig.rows,
      columns = oldConfig.columns,
      accountNumber = oldConfig.accountNumber,
      userGroup = oldConfig.userGroup,
      regionSize = oldConfig.regionSize,
      timeout = oldConfig.timeout,
      maxAttempts = oldConfig.maxAttempts
    )
  }

  val (oldConfigClass, oldConfigToNewConfigFunc) = when (configTypeToTransform) {
    ConfigType.HTTP_CONNECTION_CONFIG_V1 -> ConnectionConfig::class.java to ::connectionConfigToHttpConnectionConfig
    ConfigType.FILES_WORKING_SET_CONFIG_V1 -> FilesWorkingSetConfig::class.java to ::oldFilesWsConfigToNewFilesWsConfig
    ConfigType.JES_WORKING_SET_CONFIG_V1 -> JesWorkingSetConfig::class.java to ::oldJesWsConfigToNewJesWsConfig
    ConfigType.TSO_SESSION_CONFIG_V1 -> TSOSessionConfig::class.java to ::oldTsoSessionConfigToNewTsoSessionConfig
    else -> throw Exception("Unsupported config type to transform")
  }
  val oldConfigs = ConfigService.getService()
    .crudable
    .getAll(oldConfigClass)
    .toList()

  val newConfigs = ConfigCacheService.getService()
    .getConfigsFromCache(configTypeToTransform)
    .toList()

  val newConfigsToAdd = oldConfigs.filter { oldConfig ->
    newConfigs.none { newConfig ->
      newConfig.uuid == oldConfig.uuid
    }
  }
    .map(oldConfigToNewConfigFunc)
  newConfigsToAdd.forEach { configToAdd ->
    ConfigCacheService.getService().addConfigToCache(configToAdd)
  }

  val newConfigsToUpdate = oldConfigs.filter { oldConfig ->
    newConfigs.any { newConfig ->
      newConfig.uuid == oldConfig.uuid
    }
  }
    .map(oldConfigToNewConfigFunc)
    .filter { newConfigFromOldConfig ->
      newConfigs.any { newConfig ->
        newConfig.uuid == newConfigFromOldConfig.uuid && newConfig != newConfigFromOldConfig
      }
    }
  newConfigsToUpdate.forEach { configToUpdate ->
    ConfigCacheService.getService().updateConfigInCache(configToUpdate)
  }

  if (newConfigsToAdd.isNotEmpty() || newConfigsToUpdate.isNotEmpty()) {
    ConfigCacheService.getService().saveCacheToStorage(configTypeToTransform)
  }
}

/** Perform migration from [ConfigService] to a [StorageState] */
fun performOldConfigStorageMigration() {
  val otherSettingsState = OtherSettingsService.getService().getOtherSettings()
  val batchSizeToSave = ConfigService.getService().batchSize
  val isAutoSyncEnabledToSave = ConfigService.getService().isAutoSyncEnabled
  if (
    otherSettingsState.batchSize != batchSizeToSave
    || otherSettingsState.isAutoSyncEnabled != isAutoSyncEnabledToSave
  ) {
    otherSettingsState.batchSize = batchSizeToSave
    otherSettingsState.isAutoSyncEnabled = isAutoSyncEnabledToSave

    OtherSettingsService.getService().updateOtherSettings(otherSettingsState)
    OtherSettingsService.getService().saveOtherSettings()
  }

  performOldConfigsTypeMigrationToNewConfigsType(ConfigType.HTTP_CONNECTION_CONFIG_V1)
  performOldConfigsTypeMigrationToNewConfigsType(ConfigType.FILES_WORKING_SET_CONFIG_V1)
  performOldConfigsTypeMigrationToNewConfigsType(ConfigType.JES_WORKING_SET_CONFIG_V1)
  performOldConfigsTypeMigrationToNewConfigsType(ConfigType.TSO_SESSION_CONFIG_V1)
}
