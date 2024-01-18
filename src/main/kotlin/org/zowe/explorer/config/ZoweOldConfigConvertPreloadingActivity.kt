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
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.utils.crudable.nextUniqueValue
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.pathString

/**
 * ZoweOldConfigConvertPreloadingActivity is needed to convert the old config version to the new one to be able to run IJMP and Zowe Explorer at the same time.
 * @author Uladzislau Kalesnikau
 * @version 0.2
 * @since 2021-06-21
 */
class ZoweOldConfigConvertPreloadingActivity : PreloadingActivity() {

  /**
   * Converts old connection IDs to new IDs and sets credentials.
   * Runs if old config file exists.
   * @param oldConfigFile old config file to get connection IDs.
   */
  private fun convertOldConnectionIds(oldConfigFile: File) {
    val crudable = ConfigService.instance.crudable
    val connections = OldConfigService.instance.state?.connections?.toMutableList() ?: emptyList()
    val filesWorkingSets = OldConfigService.instance.state?.filesWorkingSets?.toMutableList() ?: emptyList()
    val jesWorkingSets = OldConfigService.instance.state?.jesWorkingSets?.toMutableList() ?: emptyList()

    val oldDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(oldConfigFile)
    val oldConnectionIds = oldDocument.documentElement.getConnectionIds()
    val connectionsToConvert = connections.filter { oldConnectionIds.contains(it.uuid) }

    connectionsToConvert.forEach {
      val filesWsToConvert = filesWorkingSets.filter { fws -> fws.connectionConfigUuid == it.uuid }
      val jesWsToConvert = jesWorkingSets.filter { jws -> jws.connectionConfigUuid == it.uuid }
      val username = CredentialService.instance.getUsernameByKey(it.uuid) ?: ""
      val password = CredentialService.instance.getPasswordByKey(it.uuid) ?: ""
      crudable.deleteByUniqueKey(ConnectionConfig::class.java, it.uuid)
      it.uuid = crudable.nextUniqueValue<ConnectionConfig, String>()
      CredentialService.instance.setCredentials(it.uuid, username, password)
      crudable.add(it)
      filesWsToConvert.forEach { fws ->
        fws.connectionConfigUuid = it.uuid
        crudable.update(fws)
      }
      jesWsToConvert.forEach { jws ->
        jws.connectionConfigUuid = it.uuid
        crudable.update(jws)
      }
    }
  }

  /**
   * Converts the old version config to the new version config
   */
  private fun convertOldVersionConfig() {
    val newConfigStorageName = "org.zowe.explorer.config.OldConfigService"
    val oldConfigStorageName = "by.iba.connector.services.ConfigService"
    val oldConfigName = "iba_connector_config.xml"
    val newConfigName = "zowe_explorer_intellij_config.xml"
    val configPathDir = Paths.get(PathManager.getConfigPath(), PathManager.OPTIONS_DIRECTORY)
    val oldConfigPath = Paths.get(configPathDir.pathString, oldConfigName).pathString
    val newConfigPath = Paths.get(configPathDir.pathString, newConfigName).pathString
    val oldConfigFile = File(oldConfigPath)
    val newConfigFile = File(newConfigPath)
    if (oldConfigFile.exists()) {
      if (!newConfigFile.exists()) {
        val charset = Charsets.UTF_8
        runCatching {
          val newContent =
            oldConfigFile
              .readText(charset)
              .replace(oldConfigStorageName.toRegex(), newConfigStorageName)
          newConfigFile.createNewFile()
          Files.write(newConfigFile.toPath(), newContent.toByteArray(charset))
        }
      }
      service<IComponentStore>().reloadState(OldConfigServiceImpl::class.java)
      convertOldConnectionIds(oldConfigFile)
    }
  }

  /**
   * Replace config storage name with old one if new config file doesn't exist
   */
  private fun replaceConfigStorageName() {
    val oldConfigStorageName = "org.zowe.explorer.config.OldConfigService"
    val newConfigStorageName = "org.zowe.explorer.config.ConfigService"
    val oldConfigName = "zowe_explorer_intellij_config.xml"
    val newConfigName = "zowe_explorer_intellij_config_v2.xml"
    val configPathDir = Paths.get(PathManager.getConfigPath(), PathManager.OPTIONS_DIRECTORY)
    val oldConfigFile = File(Paths.get(configPathDir.pathString, oldConfigName).pathString)
    val newConfigFile = File(Paths.get(configPathDir.pathString, newConfigName).pathString)
    if (oldConfigFile.exists() && !newConfigFile.exists()) {
      val charset = Charsets.UTF_8
      runCatching {
        val newContent =
          oldConfigFile
            .readText(charset)
            .replace(newConfigStorageName.toRegex(), oldConfigStorageName)
        Files.write(oldConfigFile.toPath(), newContent.toByteArray(charset))
        service<IComponentStore>().reloadState(OldConfigServiceImpl::class.java)
      }
    }
  }

  override suspend fun execute() {
    convertOldVersionConfig()
    replaceConfigStorageName()
    super.execute()
  }

}
