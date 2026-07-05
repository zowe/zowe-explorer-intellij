/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.jes.dialogs

import com.google.gson.JsonObject
import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.impl.formJobFilterKey
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.profiles.ProfileService

/**
 * Handles editing an existing JES job filter entry in a `jes_ij` profile
 * inside the `explorer_ij` section of `zowe.config.json`.
 *
 * The old filter entry is removed by its key ([formJobFilterKey]) and a new
 * entry is written with the updated values. If neither the key nor the values
 * changed, the handler returns without writing
 */
class EditJobFilterHandler(
  private val projectBasePath: String?,
  private val project: Project? = null
) {

  /**
   * Replaces an existing job filter entry with the updated values
   * @param configType the active config type
   * @param profileName the JES profile containing the filter
   * @param oldOwner the current owner value of the filter being edited
   * @param oldPrefix the current prefix value of the filter being edited
   * @param oldId the current job ID value of the filter being edited
   * @param newOwner the new owner value
   * @param newPrefix the new prefix value
   * @param newId the new job ID value
   */
  fun editEntry(
    configType: ConfigType,
    profileName: String,
    oldOwner: String,
    oldPrefix: String,
    oldId: String,
    newOwner: String,
    newPrefix: String,
    newId: String
  ) {
    val oldKey = formJobFilterKey(oldOwner, oldPrefix, oldId)
    val newKey = formJobFilterKey(newOwner, newPrefix, newId)
    if (oldKey == newKey && oldOwner == newOwner && oldPrefix == newPrefix && oldId == newId) return

    ProfileService.getService().editProfileProperties(configType, profileName, projectBasePath, project) { properties ->
      val jobFilters = properties.getAsJsonObject("jobFilters")
        ?: throw IllegalStateException("No jobFilters in profile '$profileName'")

      jobFilters.remove(oldKey)

      val entry = JsonObject()
      entry.addProperty("owner", newOwner)
      entry.addProperty("prefix", newPrefix)
      entry.addProperty("id", newId)
      jobFilters.add(newKey, entry)
    }
  }
}