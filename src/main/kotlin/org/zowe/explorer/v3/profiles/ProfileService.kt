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
import com.google.gson.JsonParser
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService

/**
 * Service for building and manipulating profiles inside `zowe.config.json`.
 * Provides helpers that encapsulate the common navigation from the config root
 * down to a nested profile's `properties` object, so callers only specify
 * what to do with those properties
 */
@Service
class ProfileService {
  companion object {
    fun getService() = service<ProfileService>()
  }

  /**
   * Builds a new profile JSON object with the given [profileType] and optional [connectionProfile]
   * @param profileType the type of profile to build
   * @param connectionProfile the connection profile path to set, or null
   * @return the assembled profile JSON object
   */
  fun buildProfile(profileType: ProfileType, connectionProfile: String?): JsonObject {
    val properties = JsonObject()
    if (!connectionProfile.isNullOrEmpty()) {
      properties.addProperty("connectionProfile", connectionProfile)
    }

    val profile = JsonObject()
    profile.addProperty("type", profileType.typeAsString)
    profile.add("properties", properties)
    return profile
  }

  /**
   * Returns the `explorer_ij` profile, creating it if absent
   * @param profiles the top-level `profiles` object from the config
   * @return the `explorer_ij` profile JSON object
   */
  fun getOrCreateExplorerProfile(profiles: JsonObject): JsonObject {
    if (!profiles.has(ProfileType.EXPLORER_IJ.typeAsString)) {
      val newProfile = JsonObject()
      newProfile.addProperty("type", ProfileType.EXPLORER_IJ.typeAsString)
      profiles.add(ProfileType.EXPLORER_IJ.typeAsString, newProfile)
    }
    return profiles.getAsJsonObject(ProfileType.EXPLORER_IJ.typeAsString)
  }

  /**
   * Returns the nested `profiles` object inside the given [profile], creating it if absent
   * @param profile the parent profile JSON object
   * @return the nested `profiles` JSON object
   */
  fun getOrCreateNestedProfiles(profile: JsonObject): JsonObject {
    if (!profile.has("profiles")) {
      profile.add("profiles", JsonObject())
    }
    return profile.getAsJsonObject("profiles")
  }

  /**
   * Navigates to `profiles.explorer_ij.profiles.<profileName>.properties` and applies
   * the given [action] to the `properties` object, then writes the modified config back.
   * Encapsulates the full read → parse → navigate → modify → write cycle that all
   * profile entry handlers (add/edit/delete for masks, filters, job filters) share
   * @param configType the active config type
   * @param profileName the nested profile name to navigate to
   * @param projectBasePath the project base path for config file resolution
   * @param project the current project (used for write notifications), or null
   * @param action the modification to apply to the profile's `properties` object
   */
  fun editProfileProperties(
    configType: ConfigType,
    profileName: String,
    projectBasePath: String?,
    project: Project? = null,
    action: (properties: JsonObject) -> Unit
  ) {
    modifyConfig(configType, projectBasePath, project) { nestedProfiles ->
      val targetProfile = nestedProfiles.getAsJsonObject(profileName)
        ?: throw IllegalStateException("Profile '$profileName' not found")

      val properties = targetProfile.getAsJsonObject("properties")
        ?: JsonObject().also { targetProfile.add("properties", it) }

      action(properties)
    }
  }

  /**
   * Removes a nested profile from the `explorer_ij` section of the config
   * @param configType the active config type
   * @param profileName the nested profile name to remove
   * @param projectBasePath the project base path for config file resolution
   * @param project the current project (used for write notifications), or null
   */
  fun deleteProfile(
    configType: ConfigType,
    profileName: String,
    projectBasePath: String?,
    project: Project? = null
  ) {
    modifyConfig(configType, projectBasePath, project) { nestedProfiles ->
      nestedProfiles.remove(profileName)
    }
  }

  /**
   * Reads the config, navigates to `profiles.explorer_ij.profiles`, applies the given
   * [action] to the nested profiles object, then writes the config back.
   * All public mutation methods delegate to this to avoid duplicating the navigation logic
   */
  private fun modifyConfig(
    configType: ConfigType,
    projectBasePath: String?,
    project: Project?,
    action: (nestedProfiles: JsonObject) -> Unit
  ) {
    val configService = ZoweConfigService.getService()
    val content = configService.readConfigContent(configType, projectBasePath)
      ?: throw IllegalStateException("Config file does not exist")
    val gson = GsonBuilder().setPrettyPrinting().create()
    val root = JsonParser.parseString(content).asJsonObject

    val profiles = root.getAsJsonObject("profiles")
      ?: throw IllegalStateException("No 'profiles' section in config")

    val defaults = root.getAsJsonObject("defaults")
    val explorerIjPath = defaults?.get(ProfileType.EXPLORER_IJ.typeAsString)?.asString
    val explorerIj = if (explorerIjPath != null) {
      configService.navigateToProfile(profiles, explorerIjPath)
    } else {
      profiles.getAsJsonObject(ProfileType.EXPLORER_IJ.typeAsString)
    } ?: throw IllegalStateException("'${ProfileType.EXPLORER_IJ.typeAsString}' profile not found")

    val nestedProfiles = explorerIj.getAsJsonObject("profiles")
      ?: throw IllegalStateException("No nested profiles in '${ProfileType.EXPLORER_IJ.typeAsString}'")

    action(nestedProfiles)

    configService.writeConfigContent(configType, project, gson.toJson(root))
  }
}