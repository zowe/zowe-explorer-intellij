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

import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.impl.formJobFilterKey
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.profiles.ProfileService

/**
 * Handles removing a JES job filter entry from an existing `jes_ij` profile
 * inside the `explorer_ij` section of `zowe.config.json`
 */
class DeleteJobFilterHandler(
  private val projectBasePath: String?,
  private val project: Project? = null
) {

  /**
   * Removes a job filter entry from the given profile
   * @param configType the active config type
   * @param profileName the JES profile containing the filter
   * @param owner the owner value of the filter to remove
   * @param prefix the prefix value of the filter to remove
   * @param jobId the job ID value of the filter to remove
   */
  fun deleteEntry(
    configType: ConfigType,
    profileName: String,
    owner: String,
    prefix: String,
    jobId: String
  ) {
    ProfileService.getService().editProfileProperties(configType, profileName, projectBasePath, project) { properties ->
      val jobFilters = properties.getAsJsonObject("jobFilters")
        ?: throw IllegalStateException("No jobFilters in profile '$profileName'")
      jobFilters.remove(formJobFilterKey(owner, prefix, jobId))
    }
  }
}