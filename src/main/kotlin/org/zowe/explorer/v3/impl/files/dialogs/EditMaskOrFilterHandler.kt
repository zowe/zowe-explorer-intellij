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

import com.google.gson.JsonObject
import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.profiles.ProfileService

/**
 * Handles editing a dataset mask or USS filter entry in an existing files profile
 * inside the `explorer_ij` section of `zowe.config.json`.
 * Supports changing both the value and the entry type (e.g., converting a DS mask to a USS filter)
 */
class EditMaskOrFilterHandler(
  private val projectBasePath: String?,
  private val project: Project? = null
) {

  /**
   * Edits the value and/or type of an existing dataset mask or USS filter entry.
   * If the type changes, the old entry is removed and a new one is created in the target collection
   * @param configType the active config type
   * @param profileName the files profile containing the entry
   * @param oldType the current type of the entry
   * @param oldValue the current value to find and replace
   * @param newType the new type (may differ from [oldType])
   * @param newValue the new value to set
   */
  fun editEntry(
    configType: ConfigType,
    profileName: String,
    oldType: EntryType,
    oldValue: String,
    newType: EntryType,
    newValue: String
  ) {
    if (oldType == newType && oldValue == newValue) return

    ProfileService.getService().editProfileProperties(configType, profileName, projectBasePath, project) { properties ->
      if (oldType == newType) {
        val (containerName, fieldName) = resolveContainerAndField(oldType)
        val container = properties.getAsJsonObject(containerName)
          ?: throw IllegalStateException("No $containerName in profile '$profileName'")
        updateEntryValue(container, fieldName, oldValue, newValue)
      } else {
        val (oldContainerName, oldFieldName) = resolveContainerAndField(oldType)
        val oldContainer = properties.getAsJsonObject(oldContainerName)
          ?: throw IllegalStateException("No $oldContainerName in profile '$profileName'")
        removeEntry(oldContainer, oldFieldName, oldValue)

        val (newContainerName, newFieldName) = resolveContainerAndField(newType)
        val newContainer = properties.getAsJsonObject(newContainerName)
          ?: JsonObject().also { properties.add(newContainerName, it) }
        val entry = JsonObject()
        entry.addProperty(newFieldName, newValue)
        newContainer.add(newValue, entry)
      }
    }
  }

  private fun resolveContainerAndField(
    entryType: EntryType
  ): Pair<String, String> {
    return when (entryType) {
      EntryType.DS_MASK -> "dsMasks" to "mask"
      EntryType.USS_FILTER -> "ussFilters" to "path"
    }
  }

  private fun updateEntryValue(container: JsonObject, fieldName: String, oldValue: String, newValue: String) {
    val matchingEntry = container.entrySet()
      .firstOrNull { it.value.isJsonObject && it.value.asJsonObject.get(fieldName)?.asString == oldValue }
      ?: throw IllegalStateException("Entry with value '$oldValue' not found")
    matchingEntry.value.asJsonObject.addProperty(fieldName, newValue)
  }

  private fun removeEntry(container: JsonObject, fieldName: String, value: String) {
    val key = container.entrySet()
      .firstOrNull { it.value.isJsonObject && it.value.asJsonObject.get(fieldName)?.asString == value }
      ?.key ?: throw IllegalStateException("Entry with value '$value' not found")
    container.remove(key)
  }
}