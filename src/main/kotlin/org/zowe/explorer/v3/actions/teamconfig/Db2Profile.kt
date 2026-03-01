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
 * Holds the fields for the Zowe **IBM Db2** profile section.
 *
 * @property shouldCreate when `true`, this profile is included in the generated config file.
 * @property host Db2 server hostname.
 * @property port Db2 server port.
 * @property user Db2 username (may be the same as the TSO login).
 * @property password Db2 password (stored as a secure field; may be the same as the TSO password).
 * @property database name of the target Db2 database.
 * @property sslFile path to the root CA certificate file for SSL connections.
 */
data class Db2Profile(
  override var shouldCreate: Boolean = false,
  val host: ProfileField<String?> = ProfileField(null, "Host", "host", "The Db2 server host name"),
  val port: ProfileField<Int?> = ProfileField(null, "Port", "port", "The Db2 server port number"),
  val user: ProfileField<String?> = ProfileField(null, "User", "user", "The Db2 user ID (may be the same as the TSO login)"),
  val password: ProfileField<CharArray?> = ProfileField(null, "Password", "password", "The Db2 password (may be the same as the TSO password)"),
  val database: ProfileField<String?> = ProfileField(null, "Database", "database", "The name of the database"),
  val sslFile: ProfileField<String?> = ProfileField(null, "SSL file", "sslFile", "Path to the root CA Certificate file")
) : ConfigProfile {
  override val profileName: String = "IBM Db2 profile"
  override val profileType: String = "db2"

  override fun jsonFriendly(): Map<String, Any> = entry(
    profileType,
    mapOf(
      host.prop,
      port.prop,
      user.prop,
      database.prop,
      sslFile.prop
    ),
    listOf(password.nameInConfig)
  )
}