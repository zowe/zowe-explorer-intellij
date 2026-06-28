/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.profiles

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.ConnectionProfiles
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService

// TODO: doc
abstract class EditProfileDialog(
  private val project: Project,
  private val configType: ConfigType,
  customTitle: String,
  private val profileName: String
): ProfileDialog(project, configType, customTitle, profileName) {
  private val zoweConfigService = ZoweConfigService.getService()

  override fun initState(connectionProfiles: ConnectionProfiles) {
    val currentConnectionProfile = ZoweConfigService.getService()
      .readConnectionProfile(configType, project.basePath, profileName)
    state.connectionProfile = when {
      currentConnectionProfile != null && connectionProfiles.profiles.contains(currentConnectionProfile) ->
        currentConnectionProfile
      connectionProfiles.defaultProfile != null && connectionProfiles.profiles.contains(connectionProfiles.defaultProfile) ->
        connectionProfiles.defaultProfile
      connectionProfiles.profiles.isNotEmpty() ->
        connectionProfiles.profiles.first()
      else -> ""
    }
  }

  /**
   * Updates the `connectionProfile` property of an existing profile
   *
   * @param profileName the name of the profile to update
   * @param newConnectionProfile the new connection profile value
   * @param configType the config type determining which zowe.config.json to write to
   */
  fun updateProfile(profileName: String, newConnectionProfile: String, configType: ConfigType) {
    val gson = GsonBuilder().setPrettyPrinting().create()

    val root = zoweConfigService.getConfigAsJsonObject(configType, project.basePath)
      ?: throw IllegalStateException("Config file does not exist")
    val explorerProfile = zoweConfigService.getExplorerProfile(configType, project.basePath)
      ?: throw IllegalStateException("Profile with name '$profileName' does not exist. Nothing to edit")
    val explorerNestedProfiles = explorerProfile.getAsJsonObject("profiles")
      ?: throw IllegalStateException("Profile with name '$profileName' does not have nested profiles")
    val targetProfile = explorerNestedProfiles.getAsJsonObject(profileName)
      ?: throw IllegalStateException("Profile '$profileName' not found")
    val properties = targetProfile.getAsJsonObject("properties")
      ?: JsonObject().also { targetProfile.add("properties", it) }

    if (newConnectionProfile.isNotBlank()) {
      properties.addProperty("connectionProfile", newConnectionProfile)
    } else {
      properties.remove("connectionProfile")
    }

    zoweConfigService.writeConfigContent(configType, project, gson.toJson(root))
  }

  override fun doOKActionCallback() {
    updateProfile(profileName, state.connectionProfile, configType)
  }
}
