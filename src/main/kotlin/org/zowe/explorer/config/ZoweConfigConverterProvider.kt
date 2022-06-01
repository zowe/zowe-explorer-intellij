/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config

import com.intellij.configurationStore.getPersistentStateComponentStorageLocation
import com.intellij.conversion.*
import com.intellij.openapi.components.impl.stores.IComponentStore
import com.intellij.openapi.components.service
import org.jetbrains.annotations.ApiStatus
import java.io.File
import java.nio.file.Files
import kotlin.io.path.pathString

/**
 * Implementation of Zowe Explorer config converter. Converts old format of configuration files (iba_mainframe_connector.xml)
 * to new format (zowe_explorer_intellij_config.xml). No additional changes in here, only copy from one file to the new one
 * @author Uladzislau Kalesnikau
 */
@Suppress("MissingDeprecatedAnnotationOnScheduledForRemovalApi")
@ApiStatus.ScheduledForRemoval(inVersion = "0.3")
class ZoweConfigConverterProvider : ConverterProvider() {

  @Suppress("UnstableApiUsage")
  class ConverterImpl : ProjectConverter() {

    private val newConfigStorageName = "org.zowe.explorer.config.ConfigService"
    private val oldConfigStorageName = "by.iba.connector.services.ConfigService"
    private val oldConfigName = "iba_connector_config.xml"
    private lateinit var newConfigFile: File
    private lateinit var oldConfigFile: File

    override fun createRunConfigurationsConverter(): ConversionProcessor<RunManagerSettings> {

      return object : ConversionProcessor<RunManagerSettings>() {

        /**
         * Check if the new config file is not exist yet and the old config file is already exist
         */
        override fun isConversionNeeded(settings: RunManagerSettings?): Boolean {
          val newConfigStorage = getPersistentStateComponentStorageLocation(service<ConfigService>().javaClass)
          val newConfigPath = newConfigStorage?.pathString
          val newConfigPathSplitted = newConfigPath?.split("/")
          val oldConfigPath =
            newConfigPathSplitted
              ?.dropLast(1)
              ?.plus(oldConfigName)
              ?.joinToString("/")
          oldConfigFile = File(oldConfigPath)
          newConfigFile = File(newConfigPath)

          return !newConfigFile.exists() && oldConfigFile.exists()
        }

        /**
         * Process configuration copy from the old config file to the new config file
         */
        override fun process(settings: RunManagerSettings?) {
          runCatching {
            val charset = Charsets.UTF_8
            val newContent =
              oldConfigFile
                .readText(charset)
                .replace(oldConfigStorageName.toRegex(), newConfigStorageName)
            newConfigFile.createNewFile()
            Files.write(newConfigFile.toPath(), newContent.toByteArray(charset))
            service<IComponentStore>().reloadState(ConfigServiceImpl::class.java)
          }
        }

      }
    }

  }

  override fun getConversionDescription(): String = "Zowe Explorer: old version config -> new version config"

  override fun createConverter(context: ConversionContext) = ConverterImpl()

}
