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

package org.zowe.explorer.v3.state.config

/**
 * Working set config class to describe working set config instances.
 * Is a base class for other specific working set entities
 * @property name the name of the working set
 */
abstract class WorkingSetConfig(
  uuid: String = EMPTY_ID,
  configType: ConfigType = ConfigType.FILES_WORKING_SET_CONFIG_V1,
  override var connectionConfigUuid: String = "",
  var name: String = ""
) : ConnectionConfigRelated, Config(uuid, configType) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is WorkingSetConfig) return false
    if (!super.equals(other)) return false

    if (connectionConfigUuid != other.connectionConfigUuid) return false
    if (name != other.name) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + connectionConfigUuid.hashCode()
    result = 31 * result + name.hashCode()
    return result
  }
}
