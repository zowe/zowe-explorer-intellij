/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.teamconfig

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.messages.Topic
import org.zowe.kotlinsdk.providers.zowe.config.ZoweCredentialManager
import java.io.File
import java.util.EventListener

/**
 * Listener for Zowe config file content changes.
 * Fired when config content is written via [ZoweConfigService.writeConfigContent]
 */
fun interface ZoweConfigChangeListener : EventListener {
  fun onConfigChanged()
}

/**
 * Application-level service managing Zowe Team Config operations:
 * config type selection per project, config file resolution, read/write,
 * and profile extraction from `zowe.config.json`
 */
@Service
class ZoweConfigService {
  companion object {
    const val EXPLORER_IJ_PROFILE = "explorer_ij"
    const val FILES_IJ_TYPE = "files_ij"

    /** Fired after [writeConfigContent] modifies a Zowe config file */
    val CONFIG_CHANGED_TOPIC: Topic<ZoweConfigChangeListener> =
      Topic.create("ZoweConfigChanged", ZoweConfigChangeListener::class.java)

    fun getService() = service<ZoweConfigService>()
  }

  private val projectsToSelectedConfigType = mutableMapOf<Project, ConfigType>()

  /**
   * Returns the selected [ConfigType] for the given [project].
   * Defaults to [ConfigType.GLOBAL_TEAM] when project is null, [ConfigType.LOCAL_TEAM] otherwise
   */
  fun getSelectedConfigType(project: Project?): ConfigType {
    if (project == null) return ConfigType.GLOBAL_TEAM
    return projectsToSelectedConfigType.getOrDefault(project, ConfigType.LOCAL_TEAM)
  }

  /** Stores the selected [configType] for the given [project] and notifies listeners */
  fun setSelectedConfigType(project: Project, configType: ConfigType) {
    projectsToSelectedConfigType[project] = configType
    notifyConfigChanged()
  }

  /** Resolves the [File] for the given [configType], using [projectBasePath] for local configs */
  fun resolveConfigFile(configType: ConfigType, projectBasePath: String?): File {
    return if (!configType.isGlobal) {
      File(projectBasePath ?: ".", configType.fileName)
    } else {
      File(System.getProperty("user.home")).resolve(".zowe").resolve(configType.fileName)
    }
  }

  /** Returns a human-readable path description for the given [configType], e.g. `~/zowe.config.json` */
  fun configPathDescription(configType: ConfigType): String {
    val slash = if (SystemInfo.isWindows) "\\" else "/"
    return if (configType.isGlobal) {
      val globalBase = if (SystemInfo.isWindows) "C:\\Users\\<user>\\.zowe" else "/home/<user>/.zowe"
      "$globalBase$slash${configType.fileName}"
    } else {
      "~$slash${configType.fileName}"
    }
  }

  /**
   * Reads the content of the Zowe config file for the given [configType].
   * Prefers the in-memory Document when the file is open in an editor
   */
  fun readConfigContent(configType: ConfigType, projectBasePath: String?): String? {
    val file = resolveConfigFile(configType, projectBasePath)
    val vf = LocalFileSystem.getInstance().findFileByPath(file.absolutePath)
    if (vf != null) {
      val document: Document? = FileDocumentManager.getInstance().getDocument(vf)
      if (document != null) return document.text
    }
    return if (file.exists()) file.readText() else null
  }

  /**
   * Reads the names of `files_ij` profiles nested under the `explorer_ij` profile
   * in the Zowe config of the given [configType]
   */
  fun readFilesProfileNames(configType: ConfigType, projectBasePath: String?): List<String> {
    val content = readConfigContent(configType, projectBasePath) ?: return emptyList()
    val root = JsonParser.parseString(content).asJsonObject
    val profiles = root.getAsJsonObject("profiles") ?: return emptyList()
    val defaults = root.getAsJsonObject("defaults")
    val explorerIjPath = defaults?.get(EXPLORER_IJ_PROFILE)?.asString ?: EXPLORER_IJ_PROFILE

    val explorerIj = navigateToProfile(profiles, explorerIjPath) ?: return emptyList()
    val nestedProfiles = explorerIj.getAsJsonObject("profiles") ?: return emptyList()

    return nestedProfiles.entrySet()
      .filter { (_, value) -> value.isJsonObject && value.asJsonObject.get("type")?.asString == FILES_IJ_TYPE }
      .map { it.key }
  }

  /**
   * A profile entry from the Zowe config.
   * @property profilePath dot-separated path to the profile (e.g. "lpar1" or "lpar1.zosmf")
   * @property secureFields the list of secure field names from the `secure` array (empty if none)
   */
  data class ProfileEntry(
    val profilePath: String,
    val secureFields: List<String>
  )

  /**
   * Recursively reads all profiles from the Zowe config of the given [configType],
   * preserving config order. Each entry includes the profile's `secure` fields (empty list if absent)
   */
  fun readAllProfiles(configType: ConfigType, projectBasePath: String?): List<ProfileEntry> {
    val content = readConfigContent(configType, projectBasePath) ?: return emptyList()
    val root = JsonParser.parseString(content).asJsonObject
    val profiles = root.getAsJsonObject("profiles") ?: return emptyList()
    val result = mutableListOf<ProfileEntry>()
    collectAllProfiles(profiles, "", result)
    return result
  }

  private fun collectAllProfiles(profiles: JsonObject, prefix: String, result: MutableList<ProfileEntry>) {
    for ((name, value) in profiles.entrySet()) {
      if (!value.isJsonObject) continue
      val profile = value.asJsonObject
      val path = if (prefix.isEmpty()) name else "$prefix.$name"

      val secureArray = profile.getAsJsonArray("secure")
      val fields = if (secureArray != null && secureArray.size() > 0) {
        secureArray.map { it.asString }
      } else {
        emptyList()
      }
      result.add(ProfileEntry(path, fields))

      val nested = profile.getAsJsonObject("profiles")
      if (nested != null) {
        collectAllProfiles(nested, path, result)
      }
    }
  }

  /**
   * Adds the given [fields] to the `secure` array of the profile at [profilePath].
   * Merges with existing secure fields without duplicates.
   * Creates the `secure` array if it does not exist
   */
  fun addSecureFields(configType: ConfigType, projectBasePath: String?, project: Project?, profilePath: String, fields: List<String>) {
    val content = readConfigContent(configType, projectBasePath) ?: return
    val root = JsonParser.parseString(content).asJsonObject
    val profiles = root.getAsJsonObject("profiles") ?: return
    val profile = navigateToProfile(profiles, profilePath) ?: return

    val secureArray = profile.getAsJsonArray("secure") ?: JsonArray()
    val existing = secureArray.map { it.asString }.toSet()
    for (field in fields) {
      if (field !in existing) {
        secureArray.add(field)
      }
    }
    profile.add("secure", secureArray)

    val gson = GsonBuilder().setPrettyPrinting().create()
    writeConfigContent(configType, projectBasePath, project, gson.toJson(root))
  }

  /**
   * Removes the given [fields] from the `secure` array of the profile at [profilePath]
   * and deletes their values from the OS secure store
   */
  fun removeSecureFields(configType: ConfigType, projectBasePath: String?, project: Project?, profilePath: String, fields: List<String>) {
    val content = readConfigContent(configType, projectBasePath) ?: return
    val root = JsonParser.parseString(content).asJsonObject
    val profiles = root.getAsJsonObject("profiles") ?: return
    val profile = navigateToProfile(profiles, profilePath) ?: return

    val secureArray = profile.getAsJsonArray("secure") ?: return
    val toRemove = fields.toSet()
    val filtered = JsonArray()
    for (element in secureArray) {
      if (element.asString !in toRemove) {
        filtered.add(element)
      }
    }
    profile.add("secure", filtered)

    val gson = GsonBuilder().setPrettyPrinting().create()
    writeConfigContent(configType, projectBasePath, project, gson.toJson(root))

    val configFilePath = resolveConfigFile(configType, projectBasePath).absolutePath
    ZoweCredentialManager.removeSecureFields(configFilePath, profilePath, fields)
  }

  /**
   * Reads a single secure field value for the given [profilePath] and [fieldName]
   * from the OS secure store. Delegates to [ZoweCredentialManager.getSecureField]
   */
  fun readSecureField(configType: ConfigType, projectBasePath: String?, profilePath: String, fieldName: String): String? {
    val configFilePath = resolveConfigFile(configType, projectBasePath).absolutePath
    return ZoweCredentialManager.getSecureField(configFilePath, profilePath, fieldName)
  }

  /**
   * Saves a single secure field value for the given [profilePath] and [fieldName]
   * to the OS secure store. Delegates to [ZoweCredentialManager.setSecureField]
   */
  fun saveSecureField(configType: ConfigType, projectBasePath: String?, profilePath: String, fieldName: String, value: String) {
    val configFilePath = resolveConfigFile(configType, projectBasePath).absolutePath
    ZoweCredentialManager.setSecureField(configFilePath, profilePath, fieldName, value)
  }

  /**
   * Navigates a dot-separated [path] (e.g. "lpar1.zosmf") through nested `profiles` objects,
   * returning the target profile [JsonObject] or null if any segment is missing
   */
  fun navigateToProfile(profiles: JsonObject, path: String): JsonObject? {
    val segments = path.split(".")
    var current = profiles
    for ((idx, segment) in segments.withIndex()) {
      val profile = current.getAsJsonObject(segment) ?: return null
      if (idx == segments.lastIndex) return profile
      current = profile.getAsJsonObject("profiles") ?: return null
    }
    return null
  }

  /**
   * Writes [content] to the Zowe config file resolved by [configType] and [projectBasePath].
   * Uses IntelliJ Document API when the file is open in an editor to avoid cache conflicts,
   * otherwise writes directly to disk and refreshes VFS.
   * Fires [CONFIG_CHANGED_TOPIC] after a successful write
   */
  fun writeConfigContent(configType: ConfigType, projectBasePath: String?, project: Project?, content: String) {
    val file = resolveConfigFile(configType, projectBasePath)
    val vf = LocalFileSystem.getInstance().findFileByPath(file.absolutePath)
    if (vf != null) {
      val document = FileDocumentManager.getInstance().getDocument(vf)
      if (document != null) {
        WriteCommandAction.runWriteCommandAction(project) {
          document.setText(content)
        }
        notifyConfigChanged()
        return
      }
    }
    file.writeText(content)
    LocalFileSystem.getInstance().refreshAndFindFileByPath(file.absolutePath)
    notifyConfigChanged()
  }

  private fun notifyConfigChanged() {
    ApplicationManager.getApplication().messageBus
      .syncPublisher(CONFIG_CHANGED_TOPIC)
      .onConfigChanged()
  }
}
