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
 * Handles adding a JES job filter entry to an existing `jes_ij` profile
 * inside the `explorer_ij` section of `zowe.config.json`.
 *
 * A job filter entry is written as:
 * ```
 * profiles.explorer_ij.profiles.<profileName>.properties.jobFilters.<owner.prefix(id)> = {
 *   "owner": "<owner>", "prefix": "<prefix>", "id": "<id>"
 * }
 * ```
 *
 * The entry key is [formJobFilterKey], so filters are unique per profile: re-adding a
 * filter with the same owner, prefix and id overwrites the existing entry rather than
 * duplicating it
 */
class AddJobFilterHandler(
  private val projectBasePath: String?,
  private val project: Project? = null
) {

  /**
   * Adds a job filter entry to the given [profileName]
   * @param configType the active config type
   * @param profileName the JES profile to add the entry to
   * @param owner the job owner to search jobs by
   * @param prefix the job name prefix to search jobs by
   * @param id the job ID to search a job by
   */
  fun addEntry(
    configType: ConfigType,
    profileName: String,
    owner: String,
    prefix: String,
    id: String
  ) {
    ProfileService.getService().editProfileProperties(configType, profileName, projectBasePath, project) { properties ->
      val jobFilters = properties.getAsJsonObject("jobFilters")
        ?: JsonObject().also { properties.add("jobFilters", it) }
      val entry = JsonObject()
      entry.addProperty("owner", owner)
      entry.addProperty("prefix", prefix)
      entry.addProperty("id", id)
      jobFilters.add(formJobFilterKey(owner, prefix, id), entry)
    }
  }
}