/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.teamconfig.profiles

/**
 * Holds the fields for the Zowe **zFTP** profile section.
 *
 * @property shouldCreate when `true`, this profile is included in the generated config file.
 * @property host hostname or IP address of the z/OS FTP server.
 * @property port FTP server port (default `21`).
 * @property user username for FTP authentication.
 * @property password password for FTP authentication (stored as a secure field).
 * @property secureFtp if `true`, both the control and data connections are encrypted
 *   (default `true`).
 * @property rejectUnauthorized whether to reject self-signed certificates; only relevant
 *   when [secureFtp] is `true`.
 * @property servername server name for the SNI TLS extension; only relevant for
 *   secure connections.
 * @property connectionTimeout maximum time in milliseconds to wait for the control
 *   connection to be established (default `10000`).
 * @property encoding transfer encoding for z/OS datasets and USS files.
 */
data class ZftpProfile(
  override var shouldCreate: Boolean = false,
  val host: ProfileField<String?> = ProfileField(null, "Host", "host", "The hostname or IP address of the z/OS server to connect to"),
  val port: ProfileField<Int?> = ProfileField(21, "Port", "port", "The port of the z/OS FTP server"),
  val user: ProfileField<String?> = ProfileField(null, "User", "user", "Username for authentication on z/OS"),
  val password: ProfileField<CharArray?> = ProfileField(null, "Password", "password", "Password to authenticate to FTP"),
  val secureFtp: ProfileField<Boolean?> = ProfileField(true, "Secure FTP", "secureFtp", "Set to true for both control and data connection encryption"),
  val rejectUnauthorized: ProfileField<Boolean?> = ProfileField(null, "Reject unauthorized", "rejectUnauthorized", "Reject self-signed certificates. Only specify this if you are connecting to a secure FTP instance"),
  val servername: ProfileField<String?> = ProfileField(null, "Server name", "servername", "Server name for the SNI (Server Name Indication) TLS extension. Only specify if you are connecting securely"),
  val connectionTimeout: ProfileField<Int?> = ProfileField(10000, "Connection timeout", "connectionTimeout", "How long (in milliseconds) to wait for the control connection to be established"),
  val encoding: ProfileField<String?> = ProfileField(null, "Encoding", "encoding", "The encoding for download and upload of z/OS data set")
) : ConfigProfile {
  override val profileName: String = "zFTP profile"
  override val profileType: String = "zftp"

  override fun jsonFriendly(): Map<String, Any> = entry(
    profileType,
    mapOf(
      host.prop,
      port.prop,
      user.prop,
      secureFtp.prop,
      rejectUnauthorized.prop,
      servername.prop,
      connectionTimeout.prop,
      encoding.prop
    ),
    listOf(password.nameInConfig)
  )
}