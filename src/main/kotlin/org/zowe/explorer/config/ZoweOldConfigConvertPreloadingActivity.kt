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

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.components.impl.stores.IComponentStore
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.annotations.ApiStatus
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.pathString

/**
 * ZoweOldConfigConvertPreloadingActivity is needed to convert the old config version to the new one to be able to run IJMP and Zowe Explorer at the same time.
 * @author Uladzislau Kalesnikau
 * @version 0.2
 * @since 2021-06-21
 */
@ApiStatus.ScheduledForRemoval(inVersion = "1.0.0")
class ZoweOldConfigConvertPreloadingActivity : PreloadingActivity() {

  /**
   * Converts the old version config to the new version config
   */
  private fun convertOldVersionConfig() {
    val newConfigStorageName = "org.zowe.explorer.config.ConfigService"
    val oldConfigStorageName = "by.iba.connector.services.ConfigService"
    val oldConfigName = "iba_connector_config.xml"
    val newConfigName = "zowe_explorer_intellij_config.xml"
    val configPathDir = Paths.get(PathManager.getConfigPath(), PathManager.OPTIONS_DIRECTORY)
    val oldConfigPath = Paths.get(configPathDir.pathString, oldConfigName).pathString
    val newConfigPath = Paths.get(configPathDir.pathString, newConfigName).pathString
    val oldConfigFile = File(oldConfigPath)
    val newConfigFile = File(newConfigPath)
    if (!newConfigFile.exists() && oldConfigFile.exists()) {
      val charset = Charsets.UTF_8
      runCatching {
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

  override fun preload(indicator: ProgressIndicator) {
    convertOldVersionConfig()
  }

}
