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

import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.profiles.ProfileService

/**
 * Handles removing a dataset mask or USS filter entry from an existing files profile
 * inside the `explorer_ij` section of `zowe.config.json`
 */
class DeleteMaskOrFilterHandler(
  private val projectBasePath: String?,
  private val project: Project? = null
) {

  /**
   * Removes a dataset mask or USS filter entry from the given profile
   * @param configType the active config type
   * @param profileName the files profile containing the entry
   * @param entryType the type of the entry to remove
   * @param value the value of the mask or filter to remove
   */
  fun deleteEntry(
    configType: ConfigType,
    profileName: String,
    entryType: EntryType,
    value: String
  ) {
    ProfileService.getService().editProfileProperties(configType, profileName, projectBasePath, project) { properties ->
      val (containerName, fieldName) = when (entryType) {
        EntryType.DS_MASK -> "dsMasks" to "mask"
        EntryType.USS_FILTER -> "ussFilters" to "path"
      }
      val container = properties.getAsJsonObject(containerName)
        ?: throw IllegalStateException("No $containerName in profile '$profileName'")

      val key = container.entrySet()
        .firstOrNull { it.value.isJsonObject && it.value.asJsonObject.get(fieldName)?.asString == value }
        ?.key ?: throw IllegalStateException("Entry with value '$value' not found")
      container.remove(key)
    }
  }
}