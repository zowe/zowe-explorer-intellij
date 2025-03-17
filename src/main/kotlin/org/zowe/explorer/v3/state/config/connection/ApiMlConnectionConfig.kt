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
import org.zowe.kotlinsdk.annotations.ZVersion

/**
 * API ML connection config
 * @property gatewayPath the API ML gateway path
 */
class ApiMlConnectionConfig(
  uuid: String = EMPTY_ID,
  configType: ConfigType = ConfigType.API_ML_CONNECTION_CONFIG_V1,
  name: String = "",
  scheme: SupportedSchemes = SupportedSchemes.HTTPS,
  host: String = "",
  port: Int = 443,
  zVersion: ZVersion = ZVersion.ZOS_2_3,
  ussOwner: String = "",
  basePath: String = "/",
  rejectUnauthorized: Boolean = true,
  var gatewayPath: String = "/gateway/api/v1"
) : HttpConnectionConfig(
  uuid, configType, name, scheme, host, port, zVersion, ussOwner, basePath, rejectUnauthorized
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ApiMlConnectionConfig) return false
    if (!super.equals(other)) return false

    if (gatewayPath != other.gatewayPath) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + gatewayPath.hashCode()
    return result
  }
}
