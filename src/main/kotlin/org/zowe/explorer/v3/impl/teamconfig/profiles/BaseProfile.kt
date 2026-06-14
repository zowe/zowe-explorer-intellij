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
 * Holds the fields for the Zowe **base** profile section.
 *
 * The base profile provides shared connection defaults (host, credentials, TLS settings)
 * that other profile types can inherit through the Zowe Team Config layering mechanism.
 * It is always included in the generated config file; individual fields are optional
 * and written only when their value is non-null.
 *
 * @property host hostname of the mainframe service.
 * @property port port number of the mainframe service.
 * @property user username for authentication.
 * @property password password for authentication (stored as a secure field in the config).
 * @property rejectUnauthorized whether to reject self-signed TLS certificates (default `true`).
 * @property tokenType type of the API token used for token-based authentication.
 * @property tokenValue value of the API token.
 * @property certFile path to a client certificate file used for authentication.
 * @property certKeyFile path to the private key file that corresponds to [certFile].
 */
data class BaseProfile(
  val host: ProfileField<String?> = ProfileField(
    null,
    "Host",
    "host",
    "Host name of service on the mainframe"
  ),
  val port: ProfileField<Int?> = ProfileField(
    null,
    "Port",
    "port",
    "Port number of service on the mainframe"
  ),
  val user: ProfileField<String?> = ProfileField(
    null,
    "User",
    "user",
    "User name to authenticate to service on the mainframe"
  ),
  val password: ProfileField<CharArray?> = ProfileField(
    null,
    "Password",
    "password",
    "Password to authenticate to service on the mainframe"
  ),
  val rejectUnauthorized: ProfileField<Boolean?> = ProfileField(
    true,
    "Reject unauthorized",
    "rejectUnauthorized",
    "Reject self-signed certificates"
  ),
  val tokenType: ProfileField<String?> = ProfileField(
    null,
    "Token type",
    "tokenType",
    "The type of token to get and use for the API. Omit this option to use the default token type, which is provided by 'zowe auth login'"
  ),
  val tokenValue: ProfileField<String?> = ProfileField(
    null,
    "Token value",
    "tokenValue",
    "The value of the token to pass to the API"
  ),
  val certFile: ProfileField<String?> = ProfileField(
    null,
    "Cert file",
    "certFile",
    "The file path to a certificate file to use for authentication"
  ),
  val certKeyFile: ProfileField<String?> = ProfileField(
    null,
    "Cert key file",
    "certKeyFile",
    "The file path to a certificate key file to use for authentication"
  )
) : ConfigProfile {
  override val profileName: String = "Base profile"
  override val profileType: String = "base"
  override var shouldCreate: Boolean = true

  override fun jsonFriendly(): Map<String, Any> = entry(
    profileType,
    mapOf(
      host.prop,
      port.prop,
      user.prop,
      rejectUnauthorized.prop,
      tokenType.prop,
      tokenValue.prop,
      certFile.prop,
      certKeyFile.prop
    ),
    listOf(password.nameInConfig)
  )
}
