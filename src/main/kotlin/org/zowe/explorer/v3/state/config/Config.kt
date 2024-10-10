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

import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import org.zowe.explorer.v3.EntityWithUuid

/**
 * Config base class to provide the common class of all the plug-in's configs.
 * WARNING: make sure you override [equals] and [hashCode] functions in child classes
 * so the comparison works correctly during the configs update
 * @property configType the config type for configs recognition service
 */
@Tag
abstract class Config(
  uuid: String = EMPTY_ID,
  @get:Transient
  var configType: ConfigType
) : EntityWithUuid(uuid) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Config) return false

    if (configType != other.configType) return false

    return true
  }

  override fun hashCode(): Int {
    return configType.hashCode()
  }
}
