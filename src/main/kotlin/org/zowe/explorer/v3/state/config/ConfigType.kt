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

import org.zowe.explorer.v3.state.config.connection.ApiMlConnectionConfig
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig
import org.zowe.explorer.v3.state.config.files.FilesWorkingSetConfig
import org.zowe.explorer.v3.state.config.jes.JesWorkingSetConfig
import org.zowe.explorer.v3.state.config.tso.TsoSessionConfig

/** Supported config types. By this enum, the configs in XML format are recognized and loaded in the plug-in's memory */
enum class ConfigType {
  FILES_WORKING_SET_CONFIG_V1,
  JES_WORKING_SET_CONFIG_V1,
  HTTP_CONNECTION_CONFIG_V1,
  API_ML_CONNECTION_CONFIG_V1,
  TSO_SESSION_CONFIG_V1,
  UNKNOWN_CONFIG;

  companion object {
    operator fun invoke(configType: String): ConfigType {
      return entries.find { "$it" == configType } ?: UNKNOWN_CONFIG
    }
  }

  /** Get a config class by the config type */
  fun toConfigClass(): Class<out Config> {
    return when (this) {
      FILES_WORKING_SET_CONFIG_V1 -> FilesWorkingSetConfig::class.java
      HTTP_CONNECTION_CONFIG_V1 -> HttpConnectionConfig::class.java
      API_ML_CONNECTION_CONFIG_V1 -> ApiMlConnectionConfig::class.java
      JES_WORKING_SET_CONFIG_V1 -> JesWorkingSetConfig::class.java
      TSO_SESSION_CONFIG_V1 -> TsoSessionConfig::class.java
      else -> TODO()
    }
  }
}
