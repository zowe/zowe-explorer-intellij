/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.dialogs

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.kotlinsdk.providers.zowe.config.ZoweConfigFile
import java.util.UUID

/**
 * Handles writing a files profile into `zowe.config.json`.
 *
 * The profile is added as a nested profile inside the root `explorer_ij` profile:
 * ```
 * profiles.explorer_ij.profiles.<profileName> = {
 *   "type": "files_ij",
 *   "properties": {
 *     "connectionProfile": "lpar1.zosmf",
 *     "dsMasks": { "<mask>": { "<mask>": "..." } },
 *     "ussPaths": { "<path>": { "<path>": "..." } }
 *   }
 * }
 * ```
 */
class CreateFilesProfileHandler(
  private val configService: ZoweConfigService,
  private val projectBasePath: String?,
  private val project: com.intellij.openapi.project.Project? = null
) {

  companion object {
    const val EXPLORER_IJ_PROFILE = "explorer_ij"
    const val FILES_IJ_TYPE = "files_ij"
    private const val ZOSMF_TYPE = "zosmf"
  }

  data class ConnectionProfiles(
    val profiles: List<String>,
    val defaultProfile: String?
  )

  fun findConnectionProfiles(configType: ConfigType): ConnectionProfiles {
    val configFile = configService.resolveConfigFile(configType, projectBasePath)
    if (!configFile.exists()) return ConnectionProfiles(emptyList(), null)

    val sdkConfigType = if (configType.fileName.contains("user"))
      org.zowe.kotlinsdk.providers.zowe.config.ConfigType.USER_CONFIG
    else
      org.zowe.kotlinsdk.providers.zowe.config.ConfigType.TEAM_CONFIG

    val zoweConfigFile = ZoweConfigFile(type = sdkConfigType, name = configType.fileName.removeSuffix(".json").removeSuffix(".config").removeSuffix(".config.user"))
    zoweConfigFile.location = configFile.parentFile?.absolutePath
    zoweConfigFile.initFromFile(shouldValidateSchema = false)

    val allProfiles = zoweConfigFile.getProfilesNameAndType(shouldValidateSchema = false)
    val zosmfProfiles = allProfiles.filter { it.second == ZOSMF_TYPE }.map { it.first }
    val defaultProfile = zoweConfigFile.defaults?.get(ZOSMF_TYPE)

    return ConnectionProfiles(zosmfProfiles, defaultProfile)
  }

  /**
   * Returns the name that will actually be used for the profile.
   * If [name] collides with an existing profile, a `_N` suffix is appended
   */
  fun resolveProfileName(name: String, configType: ConfigType): String {
    return resolveUniqueName(name, existingProfileNames(configType))
  }

  fun existingProfileNames(configType: ConfigType): Set<String> {
    val content = configService.readConfigContent(configType, projectBasePath) ?: return emptySet()
    val root = JsonParser.parseString(content).asJsonObject
    val profiles = root.getAsJsonObject("profiles") ?: return emptySet()
    val defaults = root.getAsJsonObject("defaults")
    val explorerIjPath = defaults?.get(EXPLORER_IJ_PROFILE)?.asString

    val explorerIj = if (explorerIjPath != null) {
      configService.navigateToProfile(profiles, explorerIjPath)
    } else {
      profiles.getAsJsonObject(EXPLORER_IJ_PROFILE)
    } ?: return emptySet()

    return explorerIj.getAsJsonObject("profiles")?.keySet() ?: emptySet()
  }

  fun generate(state: CreateFilesProfileDialogState, configType: ConfigType) {
    val content = configService.readConfigContent(configType, projectBasePath)
      ?: throw IllegalStateException("Config file does not exist")
    val gson = GsonBuilder().setPrettyPrinting().create()
    val root = JsonParser.parseString(content).asJsonObject

    val profiles = root.getAsJsonObject("profiles")
      ?: throw IllegalStateException("No 'profiles' section in config")

    val explorerIjProfile = getOrCreateExplorerIjProfile(profiles)
    val explorerIjProfiles = getOrCreateNestedProfiles(explorerIjProfile)

    val uniqueName = resolveUniqueName(state.profileName, explorerIjProfiles.keySet())
    explorerIjProfiles.add(uniqueName, buildFilesProfile(state))

    val defaults = root.getAsJsonObject("defaults") ?: JsonObject().also { root.add("defaults", it) }
    if (!defaults.has(EXPLORER_IJ_PROFILE)) {
      defaults.addProperty(EXPLORER_IJ_PROFILE, EXPLORER_IJ_PROFILE)
    }

    configService.writeConfigContent(configType, projectBasePath, project, gson.toJson(root))
  }

  private fun getOrCreateExplorerIjProfile(profiles: JsonObject): JsonObject {
    if (!profiles.has(EXPLORER_IJ_PROFILE)) {
      val newProfile = JsonObject()
      newProfile.addProperty("type", "explorer_ij")
      profiles.add(EXPLORER_IJ_PROFILE, newProfile)
    }
    return profiles.getAsJsonObject(EXPLORER_IJ_PROFILE)
  }

  private fun getOrCreateNestedProfiles(profile: JsonObject): JsonObject {
    if (!profile.has("profiles")) {
      profile.add("profiles", JsonObject())
    }
    return profile.getAsJsonObject("profiles")
  }

  /**
   * If [name] already exists in [existingNames], appends an incrementing suffix
   * (e.g. `profile_1`, `profile_2`) until a unique key is found
   */
  private fun resolveUniqueName(name: String, existingNames: Set<String>): String {
    if (name !in existingNames) return name
    var counter = 1
    while ("${name}_$counter" in existingNames) counter++
    return "${name}_$counter"
  }

  private fun buildFilesProfile(state: CreateFilesProfileDialogState): JsonObject {
    val properties = JsonObject()
    if (state.connectionProfile.isNotBlank()) {
      properties.addProperty("connectionProfile", state.connectionProfile)
    }

    val dsMasksObj = JsonObject()
    for (entry in state.dsMasks) {
      val maskObj = JsonObject()
      maskObj.addProperty("mask", entry.mask)
      dsMasksObj.add(UUID.randomUUID().toString(), maskObj)
    }
    if (dsMasksObj.size() > 0) {
      properties.add("dsMasks", dsMasksObj)
    }

    val ussPathsObj = JsonObject()
    for (entry in state.ussPaths) {
      val pathObj = JsonObject()
      pathObj.addProperty("path", entry.path)
      ussPathsObj.add(UUID.randomUUID().toString(), pathObj)
    }
    if (ussPathsObj.size() > 0) {
      properties.add("ussPaths", ussPathsObj)
    }

    val profile = JsonObject()
    profile.addProperty("type", FILES_IJ_TYPE)
    profile.add("properties", properties)
    return profile
  }
}
