/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.actions.teamconfig

/**
 * Holds the fields for the Zowe **SYSVIEW®** profile section.
 *
 * @property shouldCreate when `true`, this profile is included in the generated config file.
 * @property host SYSVIEW REST API hostname.
 * @property port SYSVIEW REST API port.
 * @property user z/OS username for API authentication.
 * @property password z/OS password for API authentication (stored as a secure field).
 * @property rejectUnauthorized whether to verify the server certificate against supplied CAs.
 * @property ssid SSID of the SYSVIEW instance (default `"GSVX"`).
 * @property basePath base path for API Mediation Layer routing (default `"/api/v1"`);
 *   omit when not using AML.
 */
data class SysviewProfile(
  override var shouldCreate: Boolean = false,
  val host: ProfileField<String?> = ProfileField(null, "Host", "host", "The hostname of the SYSVIEW REST API"),
  val port: ProfileField<Int?> = ProfileField(null, "Port", "port", "The port number of the SYSVIEW REST API"),
  val user: ProfileField<String?> = ProfileField(null, "User", "user", "Your z/OS username used to authenticate to the SYSVIEW REST API"),
  val password: ProfileField<CharArray?> = ProfileField(null, "Password", "password", "Your z/OS password used to authenticate to the SYSVIEW REST API"),
  val rejectUnauthorized: ProfileField<Boolean?> = ProfileField(null, "Reject unauthorized", "rejectUnauthorized", "If set, the server certificate is verified against the list of supplied CAs"),
  val ssid: ProfileField<String?> = ProfileField("GSVX", "SSID", "ssid", "SSID of the SYSVIEW instance"),
  val basePath: ProfileField<String?> = ProfileField("/api/v1", "Base path", "basePath", "The base path for your API mediation layer instance. Do not specify this option if you are not using an API mediation layer")
) : ConfigProfile {
  override val profileName: String = "SYSVIEW\u00ae profile"
  override val profileType: String = "sysview"

  override fun jsonFriendly(): Map<String, Any> = entry(
    profileType,
    mapOf(
      host.prop,
      port.prop,
      user.prop,
      rejectUnauthorized.prop,
      ssid.prop,
      basePath.prop
    ),
    listOf(password.nameInConfig)
  )
}