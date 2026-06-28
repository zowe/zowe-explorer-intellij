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

import com.google.gson.JsonObject
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

// TODO: doc
@Service
class ProfileService {
  companion object {
    fun getService() = service<ProfileService>()
  }

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

  fun getOrCreateExplorerProfile(profiles: JsonObject): JsonObject {
    if (!profiles.has(ProfileType.EXPLORER_IJ.typeAsString)) {
      val newProfile = JsonObject()
      newProfile.addProperty("type", ProfileType.EXPLORER_IJ.typeAsString)
      profiles.add(ProfileType.EXPLORER_IJ.typeAsString, newProfile)
    }
    return profiles.getAsJsonObject(ProfileType.EXPLORER_IJ.typeAsString)
  }

  fun getOrCreateNestedProfiles(profile: JsonObject): JsonObject {
    if (!profile.has("profiles")) {
      profile.add("profiles", JsonObject())
    }
    return profile.getAsJsonObject("profiles")
  }
}
