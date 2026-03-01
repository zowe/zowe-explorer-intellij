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
 * Holds the fields for the Zowe **Endevor Bridge for Git (EBG)** profile section.
 *
 * @property shouldCreate when `true`, this profile is included in the generated config file.
 * @property protocol SCM protocol used for the EBG connection (default `"https"`).
 * @property host Endevor Bridge for Git hostname.
 * @property port Endevor Bridge for Git port.
 * @property user Git username used for EBG authentication.
 * @property token Git personal access token obtained from the Git Enterprise Server
 *   (stored as a secure field).
 * @property rejectUnauthorized whether to reject self-signed TLS certificates.
 */
data class EbgProfile(
  override var shouldCreate: Boolean = false,
  val protocol: ProfileField<String?> = ProfileField("https", "Protocol", "protocol", "The Endevor Bridge for Git SCM protocol"),
  val host: ProfileField<String?> = ProfileField(null, "Host", "host", "The Endevor Bridge for Git hostname"),
  val port: ProfileField<Int?> = ProfileField(null, "Port", "port", "The Endevor Bridge for Git port"),
  val user: ProfileField<String?> = ProfileField(null, "User", "user", "Endevor Bridge for Git username (your git username)"),
  val token: ProfileField<CharArray?> = ProfileField(null, "Token", "token", "Git personal access token (it can be obtained from your Git Enterprise Server)"),
  val rejectUnauthorized: ProfileField<Boolean?> = ProfileField(null, "Reject unauthorized", "rejectUnauthorized", "Reject self-signed certificates")
) : ConfigProfile {
  override val profileName: String = "Endevor Bridge for Git profile"
  override val profileType: String = "ebg"

  override fun jsonFriendly(): Map<String, Any> = entry(
    profileType,
    mapOf(
      protocol.prop,
      host.prop,
      port.prop,
      user.prop,
      rejectUnauthorized.prop
    ),
    listOf(token.nameInConfig)
  )
}