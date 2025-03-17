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

package org.zowe.explorer.v3.state.config.files

import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.WorkingSetConfig

/**
 * Files working set config class to describe files working set config instances
 * @property dsMasks the related list of [DatasetMaskConfigItem]s
 * @property ussPaths the related list of [UssPathConfigItem]s
 */
class FilesWorkingSetConfig(
  uuid: String = EMPTY_ID,
  configType: ConfigType = ConfigType.FILES_WORKING_SET_CONFIG_V1,
  connectionConfigUuid: String = "",
  name: String = "",
  var dsMasks: MutableList<DatasetMaskConfigItem> = mutableListOf(),
  var ussPaths: MutableList<UssPathConfigItem> = mutableListOf()
) : WorkingSetConfig(uuid, configType, connectionConfigUuid, name) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is FilesWorkingSetConfig) return false
    if (!super.equals(other)) return false

    if (dsMasks != other.dsMasks) return false
    if (ussPaths != other.ussPaths) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + dsMasks.hashCode()
    result = 31 * result + ussPaths.hashCode()
    return result
  }
}
