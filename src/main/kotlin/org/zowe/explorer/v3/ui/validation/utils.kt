/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Dzianis Lisiankou
 */

package org.zowe.explorer.v3.ui.validation

import com.intellij.openapi.ui.ValidationInfo
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.ConnectionConfig
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import javax.swing.JTextField

fun validateConnectionName(component: JTextField, ignoreValue: String? = null, configType: ConfigType): ValidationInfo? {
  val configsWithTheSameName = ConfigCacheService.getService().getConfigsFromCache(configType)
    .toList()
    .filterIsInstance<ConnectionConfig>()
    .count { ignoreValue != it.name && it.name == component.text.trim() }
  val configAlreadyExists = configsWithTheSameName > 0
  return if (configAlreadyExists) {
    ValidationInfo("You must provide unique connection name. Connection ${component.text} already exists.", component)
  } else {
    null
  }
}
