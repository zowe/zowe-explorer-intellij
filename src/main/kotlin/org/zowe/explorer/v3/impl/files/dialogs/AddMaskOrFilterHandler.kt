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
import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import java.util.UUID

/**
 * Handles adding a dataset mask or USS filter entry to an existing files profile
 * inside the `explorer_ij` section of `zowe.config.json`.
 *
 * A dataset mask entry is written as:
 * ```
 * profiles.explorer_ij.profiles.<profileName>.properties.dsMasks.<maskValue> = { "mask": "<value>" }
 * ```
 *
 * A USS filter entry is written as:
 * ```
 * profiles.explorer_ij.profiles.<profileName>.properties.ussFilters.<filterValue> = { "path": "<value>" }
 * ```
 */
class AddMaskOrFilterHandler(
  private val configService: ZoweConfigService,
  private val projectBasePath: String?,
  private val project: Project? = null
) {

  companion object {
    private const val EXPLORER_IJ_PROFILE = "explorer_ij"
  }

  /**
   * Adds a dataset mask or USS filter entry to the given [profileName]
   * @param configType the active config type
   * @param profileName the files profile to add the entry to
   * @param entryType whether to add a dataset mask or USS filter
   * @param value the mask or filter value
   */
  fun addEntry(
    configType: ConfigType,
    profileName: String,
    entryType: AddMaskOrFilterDialog.EntryType,
    value: String
  ) {
    val content = configService.readConfigContent(configType, projectBasePath)
      ?: throw IllegalStateException("Config file does not exist")
    val gson = GsonBuilder().setPrettyPrinting().create()
    val root = JsonParser.parseString(content).asJsonObject

    val profiles = root.getAsJsonObject("profiles")
      ?: throw IllegalStateException("No 'profiles' section in config")

    val defaults = root.getAsJsonObject("defaults")
    val explorerIjPath = defaults?.get(EXPLORER_IJ_PROFILE)?.asString
    val explorerIj = if (explorerIjPath != null) {
      configService.navigateToProfile(profiles, explorerIjPath)
    } else {
      profiles.getAsJsonObject(EXPLORER_IJ_PROFILE)
    } ?: throw IllegalStateException("'$EXPLORER_IJ_PROFILE' profile not found")

    val nestedProfiles = explorerIj.getAsJsonObject("profiles")
      ?: throw IllegalStateException("No nested profiles in '$EXPLORER_IJ_PROFILE'")
    val targetProfile = nestedProfiles.getAsJsonObject(profileName)
      ?: throw IllegalStateException("Profile '$profileName' not found")

    val properties = targetProfile.getAsJsonObject("properties")
      ?: JsonObject().also { targetProfile.add("properties", it) }

    when (entryType) {
      AddMaskOrFilterDialog.EntryType.DS_MASK -> {
        val dsMasks = properties.getAsJsonObject("dsMasks")
          ?: JsonObject().also { properties.add("dsMasks", it) }
        val entry = JsonObject()
        entry.addProperty("mask", value)
        dsMasks.add(UUID.randomUUID().toString(), entry)
      }
      AddMaskOrFilterDialog.EntryType.USS_FILTER -> {
        val ussFilters = properties.getAsJsonObject("ussFilters")
          ?: JsonObject().also { properties.add("ussFilters", it) }
        val entry = JsonObject()
        entry.addProperty("path", value)
        ussFilters.add(UUID.randomUUID().toString(), entry)
      }
    }

    configService.writeConfigContent(configType, project, gson.toJson(root))
  }
}
