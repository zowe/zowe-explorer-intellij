/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.connection

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.kotlinsdk.providers.zowe.config.ZoweCredentialManager

/**
 * A single node in the profile merge chain, holding the dot-separated
 * [path] (e.g. "lpar1.zosmf") and the corresponding JSON profile object
 */
data class ProfileNode(val path: String, val profile: JsonObject)

/**
 * Builds and walks the Zowe profile hierarchy to resolve properties.
 *
 * For a connection profile path like `lpar1.zosmf` the merge chain is:
 * 1. `profiles.lpar1.profiles.zosmf` — the target profile
 * 2. `profiles.lpar1` — its parent
 * 3. The default `base` profile from the `defaults` section
 *
 * Each property is resolved independently: the first profile in the chain
 * that provides the value wins. Properties listed in a profile's `secure`
 * array are read from the OS secure store via [ZoweCredentialManager]
 */
object ProfileChainResolver {
  private const val BASE_PROFILE_TYPE = "base"

  /**
   * Parses the config and builds the profile chain for the given [connectionProfilePath]
   * @return the chain and the resolved config file path, or `null` if the config cannot be read
   */
  fun resolve(
    configType: ConfigType,
    projectBasePath: String?,
    connectionProfilePath: String
  ): ChainContext? {
    val configService = ZoweConfigService.getService()
    val content = configService.readConfigContent(configType, projectBasePath) ?: return null
    val root = JsonParser.parseString(content).asJsonObject
    val profiles = root.getAsJsonObject("profiles") ?: return null
    val configFilePath = configService.resolveConfigFile(configType, projectBasePath).absolutePath
    val chain = buildProfileChain(profiles, root, connectionProfilePath)
    return ChainContext(chain, configFilePath)
  }

  /**
   * Resolves a string property by walking the [chain]
   */
  fun resolveString(chain: List<ProfileNode>, configFilePath: String, name: String): String? {
    for (node in chain) {
      val value = readStringFromNode(node, configFilePath, name)
      if (value != null) return value
    }
    return null
  }

  /**
   * Resolves an integer property by walking the [chain]
   */
  fun resolveInt(chain: List<ProfileNode>, configFilePath: String, name: String): Int? {
    for (node in chain) {
      if (isSecureField(node.profile, name)) {
        val secureValue = ZoweCredentialManager.getSecureField(configFilePath, node.path, name)
        if (secureValue != null) return secureValue.toIntOrNull()
      }
      val element = node.profile.getAsJsonObject("properties")?.get(name)
      if (element != null && !element.isJsonNull) {
        return try { element.asInt } catch (_: Exception) { null }
      }
    }
    return null
  }

  /**
   * Resolves a boolean property by walking the [chain]
   */
  fun resolveBoolean(chain: List<ProfileNode>, configFilePath: String, name: String): Boolean? {
    for (node in chain) {
      if (isSecureField(node.profile, name)) {
        val secureValue = ZoweCredentialManager.getSecureField(configFilePath, node.path, name)
        if (secureValue != null) return secureValue.toBooleanStrictOrNull()
      }
      val element = node.profile.getAsJsonObject("properties")?.get(name)
      if (element != null && !element.isJsonNull) {
        return try { element.asBoolean } catch (_: Exception) { null }
      }
    }
    return null
  }

  private fun buildProfileChain(
    profiles: JsonObject,
    root: JsonObject,
    connectionProfilePath: String
  ): List<ProfileNode> {
    val configService = ZoweConfigService.getService()
    val chain = mutableListOf<ProfileNode>()

    val ancestorPaths = buildAncestorPaths(connectionProfilePath)
    for (path in ancestorPaths) {
      val profile = configService.navigateToProfile(profiles, path)
      if (profile != null) {
        chain.add(ProfileNode(path, profile))
      }
    }

    val defaults = root.getAsJsonObject("defaults")
    val baseProfilePath = defaults?.get(BASE_PROFILE_TYPE)?.asString
    if (baseProfilePath != null && baseProfilePath !in ancestorPaths) {
      val baseProfile = configService.navigateToProfile(profiles, baseProfilePath)
      if (baseProfile != null) {
        chain.add(ProfileNode(baseProfilePath, baseProfile))
      }
    }

    return chain
  }

  private fun buildAncestorPaths(profilePath: String): List<String> {
    val segments = profilePath.split(".")
    return segments.indices.reversed().map { i ->
      segments.subList(0, i + 1).joinToString(".")
    }
  }

  private fun readStringFromNode(node: ProfileNode, configFilePath: String, name: String): String? {
    if (isSecureField(node.profile, name)) {
      val secureValue = ZoweCredentialManager.getSecureField(configFilePath, node.path, name)
      if (secureValue != null) return secureValue
    }
    val element = node.profile.getAsJsonObject("properties")?.get(name)
    if (element != null && !element.isJsonNull) {
      return element.asString
    }
    return null
  }

  private fun isSecureField(profile: JsonObject, name: String): Boolean {
    val secureArray = profile.getAsJsonArray("secure") ?: return false
    return secureArray.any { it.asString == name }
  }

  data class ChainContext(val chain: List<ProfileNode>, val configFilePath: String)
}
