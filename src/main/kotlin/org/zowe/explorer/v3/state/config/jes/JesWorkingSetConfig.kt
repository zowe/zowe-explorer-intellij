/*
 * Copyright (c) 2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package org.zowe.explorer.v3.state.config.jes

import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.WorkingSetConfig

/**
 * JES working set config class to describe JES working set config instances
 * @property jobFilters the related list of [JobFilterConfigItem]s
 */
class JesWorkingSetConfig(
  uuid: String = EMPTY_ID,
  configType: ConfigType = ConfigType.JES_WORKING_SET_CONFIG_V1,
  connectionConfigUuid: String = "",
  name: String = "",
  var jobFilters: MutableList<JobFilterConfigItem> = mutableListOf()
) : WorkingSetConfig(uuid, configType, connectionConfigUuid, name) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is JesWorkingSetConfig) return false
    if (!super.equals(other)) return false

    if (jobFilters != other.jobFilters) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + jobFilters.hashCode()
    return result
  }
}
