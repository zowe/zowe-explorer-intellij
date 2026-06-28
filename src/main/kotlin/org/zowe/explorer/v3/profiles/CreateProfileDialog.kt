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
import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.impl.resolveUniqueName
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.ConnectionProfiles
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService

// TODO: doc
abstract class CreateProfileDialog(
  private val project: Project,
  private val configType: ConfigType,
  customTitle: String,
  private val profileType: ProfileType
): ProfileDialog(project, configType, customTitle, "")  {
  private val zoweConfigService = ZoweConfigService()
  private val profileService = ProfileService()

  override fun initState(connectionProfiles: ConnectionProfiles) {
    if (
      connectionProfiles.defaultProfile != null
      && connectionProfiles.profiles.contains(connectionProfiles.defaultProfile)
    ) {
      state.connectionProfile = connectionProfiles.defaultProfile
    } else if (connectionProfiles.profiles.isNotEmpty()) {
      state.connectionProfile = connectionProfiles.profiles.first()
    }
  }

  private fun generateProfile() {
    val gson = GsonBuilder().setPrettyPrinting().create()

    val root = zoweConfigService.getConfigAsJsonObject(configType, project.basePath)
      ?: throw IllegalStateException("Config file does not exist")
    val profiles = root.getAsJsonObject("profiles")
      ?: throw IllegalStateException("No 'profiles' section in config")

    val explorerProfile = profileService.getOrCreateExplorerProfile(profiles)
    val explorerNestedProfiles = profileService.getOrCreateNestedProfiles(explorerProfile)

    val uniqueName = resolveUniqueName(state.profileName, explorerNestedProfiles.keySet())
    explorerNestedProfiles.add(
      uniqueName,
      ProfileService.getService()
        .buildProfile(profileType, state.connectionProfile)
    )

    val defaults = zoweConfigService.getOrCreateConfigDefaults(root)
    if (!defaults.has(ProfileType.EXPLORER_IJ.typeAsString)) {
      defaults.addProperty(ProfileType.EXPLORER_IJ.typeAsString, ProfileType.EXPLORER_IJ.typeAsString)
    }

    zoweConfigService.writeConfigContent(configType, project, gson.toJson(root))
  }

  override fun doOKActionCallback() {
    generateProfile()
  }
}
