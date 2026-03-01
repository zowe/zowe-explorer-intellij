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
 * Holds the fields for the Zowe **Endevor®** profile section.
 *
 * @property shouldCreate when `true`, this profile is included in the generated config file.
 * @property host Endevor REST API hostname.
 * @property port Endevor REST API port.
 * @property user Endevor session username.
 * @property password Endevor session password (stored as a secure field).
 * @property protocol protocol used for the REST API connection (default `"https"`).
 * @property basePath base path for the REST API (default `"EndevorService/api/v2"`).
 * @property rejectUnauthorized whether to verify the server certificate against supplied CAs.
 * @property reportDir directory where Endevor reports are written (default `"."`).
 */
data class EndevorProfile(
  override var shouldCreate: Boolean = false,
  val host: ProfileField<String?> = ProfileField(null, "Host", "host", "The hostname of the endevor session"),
  val port: ProfileField<Int?> = ProfileField(null, "Port", "port", "The port number of the endevor session"),
  val user: ProfileField<String?> = ProfileField(null, "User", "user", "The username of the endevor session"),
  val password: ProfileField<CharArray?> = ProfileField(null, "Password", "password", "The password of the user"),
  val protocol: ProfileField<String?> = ProfileField("https", "Protocol", "protocol", "The protocol used for connecting to Endevor Rest API"),
  val basePath: ProfileField<String?> = ProfileField("EndevorService/api/v2", "Base path", "basePath", "The base path used for connecting to Endevor Rest API"),
  val rejectUnauthorized: ProfileField<Boolean?> = ProfileField(null, "Reject unauthorized", "rejectUnauthorized", "If set, the server certificate is verified against the list of supplied CAs"),
  val reportDir: ProfileField<String?> = ProfileField(".", "Report dir", "reportDir", "The default path where any reports will be written to, either absolute or relative to current directory")
) : ConfigProfile {
  override val profileName: String = "Endevor\u00ae profile"
  override val profileType: String = "endevor"

  override fun jsonFriendly(): Map<String, Any> = entry(
    profileType,
    mapOf(
      host.prop,
      port.prop,
      user.prop,
      protocol.prop,
      basePath.prop,
      rejectUnauthorized.prop,
      reportDir.prop
    ),
    listOf(password.nameInConfig)
  )
}