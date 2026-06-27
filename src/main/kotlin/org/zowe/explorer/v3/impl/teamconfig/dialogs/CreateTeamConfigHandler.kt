/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.teamconfig.dialogs

import com.google.gson.GsonBuilder
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import java.io.File

private data class ProfileSpec(
  val name: String,
  val type: String,
  val properties: Map<String, Any> = emptyMap(),
  val secure: List<String> = emptyList(),
  val children: List<ProfileSpec> = emptyList()
)

private val DEFAULT_PROFILES = listOf(
  ProfileSpec("base", "base", mapOf("host" to "example.com", "rejectUnauthorized" to true), listOf("password")),
  ProfileSpec("zosmf", "zosmf", mapOf("port" to 443, "protocol" to "https"), listOf("password")),
  ProfileSpec("tso", "tso", mapOf("characterSet" to "697", "codePage" to "1047", "columns" to 80, "logonProcedure" to "IZUFPROC", "regionSize" to 4096, "rows" to 24)),
  ProfileSpec("ssh", "ssh", mapOf("port" to 22), listOf("password", "keyPassphrase")),
  ProfileSpec("explorer_ij", "explorer_ij")
)

class CreateTeamConfigHandler(
  private val configService: ZoweConfigService,
  private val projectBasePath: String?
) {

  fun generate(state: CreateTeamConfigDialogState) {
    val file = resolveConfigFile(state)
    file.parentFile?.mkdirs()
    val gson = GsonBuilder().setPrettyPrinting().create()
    file.writeText(gson.toJson(buildConfigMap()))
    copySchema(file)
  }

  data class ProfileEntry(
    val name: String,
    val type: String,
    val properties: Map<String, Any>,
    val children: List<ProfileEntry>
  )

  fun profileEntries(): List<ProfileEntry> =
    DEFAULT_PROFILES.map { it.toEntry() }

  private fun ProfileSpec.toEntry(): ProfileEntry =
    ProfileEntry(name, type, properties, children.map { it.toEntry() })

  fun resolveConfigFile(state: CreateTeamConfigDialogState): File {
    return configService.resolveConfigFile(state.configType, projectBasePath)
  }

  private fun buildProfileEntry(spec: ProfileSpec): Map<String, Any> {
    val entry = linkedMapOf<String, Any>("type" to spec.type)
    if (spec.properties.isNotEmpty()) {
      entry["properties"] = spec.properties
    }
    if (spec.secure.isNotEmpty()) {
      entry["secure"] = spec.secure
    }
    if (spec.children.isNotEmpty()) {
      val childProfiles = linkedMapOf<String, Any>()
      for (child in spec.children) {
        childProfiles[child.name] = buildProfileEntry(child)
      }
      entry["profiles"] = childProfiles
    }
    return entry
  }

  private fun buildConfigMap(): Map<String, Any> {
    val profiles = linkedMapOf<String, Any>()
    val defaults = linkedMapOf<String, String>()
    for (spec in DEFAULT_PROFILES) {
      profiles[spec.name] = buildProfileEntry(spec)
      defaults[spec.type] = spec.name
    }
    return linkedMapOf(
      "\$schema" to "./zowe.schema.json",
      "profiles" to profiles,
      "defaults" to defaults,
      "autoStore" to true
    )
  }

  private fun copySchema(configFile: File) {
    val schemaSource = javaClass.getResourceAsStream("/files/zowe.schema.json")
    schemaSource?.use { input ->
      configFile.resolveSibling("zowe.schema.json").outputStream().use { output ->
        input.copyTo(output)
      }
    }
  }
}