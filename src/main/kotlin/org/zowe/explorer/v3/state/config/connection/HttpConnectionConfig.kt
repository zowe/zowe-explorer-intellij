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

package org.zowe.explorer.v3.state.config.connection

import org.zowe.explorer.v3.SupportedSchemes
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.ConnectionConfig
import org.zowe.kotlinsdk.annotations.ZVersion

/**
 * HTTP connection config
 * @property basePath the base path to access service by
 * @property rejectUnauthorized to mark if the connection cannot accept self-signed certificates
 */
open class HttpConnectionConfig(
  uuid: String = EMPTY_ID,
  configType: ConfigType = ConfigType.HTTP_CONNECTION_CONFIG_V1,
  name: String = "",
  scheme: SupportedSchemes = SupportedSchemes.HTTPS,
  host: String = "",
  port: Int = 443,
  zVersion: ZVersion = ZVersion.ZOS_2_3,
  ussOwner: String = "",
  var basePath: String = "/",
  var rejectUnauthorized: Boolean = true,
  var isHostnameVerified: Boolean = false
) : ConnectionConfig(uuid, configType, name, scheme, host, port, zVersion, ussOwner) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is HttpConnectionConfig) return false
    if (!super.equals(other)) return false

    if (rejectUnauthorized != other.rejectUnauthorized) return false
    if (isHostnameVerified != other.isHostnameVerified) return false
    if (basePath != other.basePath) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + rejectUnauthorized.hashCode()
    result = 31 * result + isHostnameVerified.hashCode()
    result = 31 * result + basePath.hashCode()
    return result
  }
}
