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
import org.zowe.explorer.v3.impl.resolveUniqueName
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.ConnectionProfiles
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService

/**
 * Base dialog for editing an existing `explorer_ij` nested profile in the target
 * `zowe.config.json`. Allows renaming the profile and changing its `connectionProfile`
 * @param project the current project (used to resolve local config path)
 * @param configType the config type determining which zowe.config.json to write to
 * @param customTitle the dialog window title
 * @param profileName the current name of the profile being edited
 */
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
   * Updates an existing profile: renames it (when [newProfileName] differs from the
   * current name) and updates its `connectionProfile` property.
   *
   * The rename preserves the original ordering of the profiles and, if [newProfileName]
   * clashes with another existing profile, falls back to a unique name via [resolveUniqueName]
   *
   * @param oldProfileName the current name of the profile to update
   * @param newProfileName the new name for the profile
   * @param newConnectionProfile the new connection profile value
   * @param configType the config type determining which zowe.config.json to write to
   */
  fun updateProfile(
    oldProfileName: String,
    newProfileName: String,
    newConnectionProfile: String,
    configType: ConfigType
  ) {
    val gson = GsonBuilder().setPrettyPrinting().create()

    val root = zoweConfigService.getConfigAsJsonObject(configType, project.basePath)
      ?: throw IllegalStateException("Config file does not exist")
    val profiles = root.getAsJsonObject("profiles")
      ?: throw IllegalStateException("No 'profiles' section in config")
    val explorerProfilePath = zoweConfigService.getExplorerProfilePath(root)
    val explorerProfile = zoweConfigService.navigateToProfile(profiles, explorerProfilePath)
      ?: throw IllegalStateException("Profile with name '$oldProfileName' does not exist. Nothing to edit")
    val explorerNestedProfiles = explorerProfile.getAsJsonObject("profiles")
      ?: throw IllegalStateException("Profile with name '$oldProfileName' does not have nested profiles")
    val targetProfile = explorerNestedProfiles.getAsJsonObject(oldProfileName)
      ?: throw IllegalStateException("Profile '$oldProfileName' not found")
    val properties = targetProfile.getAsJsonObject("properties")
      ?: JsonObject().also { targetProfile.add("properties", it) }

    if (newConnectionProfile.isNotBlank()) {
      properties.addProperty("connectionProfile", newConnectionProfile)
    } else {
      properties.remove("connectionProfile")
    }

    val trimmedNewName = newProfileName.trim()
    if (trimmedNewName.isNotBlank() && trimmedNewName != oldProfileName) {
      val otherNames = explorerNestedProfiles.keySet().filter { it != oldProfileName }.toSet()
      val resolvedName = resolveUniqueName(trimmedNewName, otherNames)
      val reorderedProfiles = JsonObject()
      explorerNestedProfiles.entrySet().forEach { (key, value) ->
        reorderedProfiles.add(if (key == oldProfileName) resolvedName else key, value)
      }
      explorerProfile.add("profiles", reorderedProfiles)
    }

    zoweConfigService.writeConfigContent(configType, project, gson.toJson(root))
  }

  override fun doOKActionCallback() {
    updateProfile(profileName, state.profileName, state.connectionProfile, configType)
  }
}
