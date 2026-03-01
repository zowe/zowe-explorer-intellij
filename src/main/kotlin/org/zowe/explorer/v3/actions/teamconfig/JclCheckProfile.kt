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
 * Holds the fields for the Zowe **JCLCheck™** profile section.
 *
 * @property shouldCreate when `true`, this profile is included in the generated config file.
 * @property host hostname of the JCLCheck API service on the mainframe.
 * @property port port of the JCLCheck API service (default `12697`).
 * @property user username for JCLCheck API authentication.
 * @property password password for JCLCheck API authentication (stored as a secure field).
 * @property basePath base path for API Mediation Layer routing
 *   (default `"cajclcheck/api/v1"`); omit when not using AML.
 * @property rejectUnauthorized whether to reject self-signed TLS certificates (default `true`).
 * @property protocol connection protocol: `"http"` or `"https"` (default `"https"`).
 * @property jclcheckOptions runtime options passed verbatim to JCLCheck, equivalent to the
 *   `PARM=` or `OPTIONS DD` value on a batch JCLCheck run.
 */
data class JclCheckProfile(
  override var shouldCreate: Boolean = false,
  val host: ProfileField<String?> = ProfileField(null, "Host", "host", "Host name of the JCLCheck API service that is running on the mainframe system"),
  val port: ProfileField<Int?> = ProfileField(12697, "Port", "port", "Port for the JCLCheck API service that is running on the mainframe system"),
  val user: ProfileField<String?> = ProfileField(null, "User", "user", "User name for authenticating connections to the JCLCheck API service"),
  val password: ProfileField<CharArray?> = ProfileField(null, "Password", "password", "Password for authenticating connections to the JCLCheck API service"),
  val basePath: ProfileField<String?> = ProfileField("cajclcheck/api/v1", "Base path", "basePath", "The base path for your API mediation layer instance. Do not specify this option if you are not using an API mediation layer"),
  val rejectUnauthorized: ProfileField<Boolean?> = ProfileField(true, "Reject unauthorized", "rejectUnauthorized", "Reject self-signed certificates"),
  val protocol: ProfileField<String?> = ProfileField("https", "Protocol", "protocol", "Specifies protocol to use for JCLCheck connection (http or https)"),
  val jclcheckOptions: ProfileField<String?> = ProfileField(null, "JCLCheck options", "jclcheckOptions", "The desired set of JCLCheck runtime options, specified exactly as you would on the PARM= or OPTIONS DD on a batch run of JCLCheck")
) : ConfigProfile {
  override val profileName: String = "JCLCheck\u2122 profile"
  override val profileType: String = "jclcheck"

  override fun jsonFriendly(): Map<String, Any> = entry(
    profileType,
    mapOf(
      host.prop,
      port.prop,
      user.prop,
      basePath.prop,
      rejectUnauthorized.prop,
      protocol.prop,
      jclcheckOptions.prop
    ),
    listOf(password.nameInConfig)
  )
}