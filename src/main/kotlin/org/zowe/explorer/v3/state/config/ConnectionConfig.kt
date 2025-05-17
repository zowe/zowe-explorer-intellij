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

import org.zowe.explorer.v3.SupportedSchemes
import org.zowe.kotlinsdk.annotations.ZVersion

/**
 * Connection config base class to store specific connection configurations
 * @property name the name of the connection
 * @property scheme the used scheme of the connection
 * @property host the host IP address of the connection
 * @property port the port of the connection
 * @property zVersion the version of the z/OS
 * @property ussOwner the owner
 */
abstract class ConnectionConfig(
  uuid: String = EMPTY_ID,
  configType: ConfigType,
  var name: String = "",
  var scheme: SupportedSchemes = SupportedSchemes.UNSUPPORTED,
  var host: String = "",
  var port: Int = 0,
  var zVersion: ZVersion = ZVersion.ZOS_2_3,
  var ussOwner: String = ""
) : Config(uuid, configType) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ConnectionConfig) return false
    if (!super.equals(other)) return false

    if (port != other.port) return false
    if (name != other.name) return false
    if (scheme != other.scheme) return false
    if (host != other.host) return false
    if (zVersion != other.zVersion) return false
    if (ussOwner != other.ussOwner) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + port
    result = 31 * result + name.hashCode()
    result = 31 * result + scheme.hashCode()
    result = 31 * result + host.hashCode()
    result = 31 * result + zVersion.hashCode()
    result = 31 * result + ussOwner.hashCode()
    return result
  }
}
