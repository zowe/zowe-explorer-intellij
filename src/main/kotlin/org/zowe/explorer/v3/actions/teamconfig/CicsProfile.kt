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
 * Holds the fields for the Zowe **IBM CICS** profile section.
 *
 * @property shouldCreate when `true`, this profile is included in the generated config file.
 * @property host CMCI server hostname.
 * @property port CMCI server port (default `1490`).
 * @property user CICS username.
 * @property password CICS password (stored as a secure field).
 * @property regionName name of the target CICS region.
 * @property cicsPlex name of the target CICSPlex.
 * @property rejectUnauthorized whether to reject self-signed TLS certificates (default `true`).
 * @property protocol CMCI protocol: `"http"` or `"https"` (default `"https"`).
 */
data class CicsProfile(
  var shouldCreate: Boolean = false,
  val host: ProfileField<String?> = ProfileField(null, "The CMCI server host name"),
  val port: ProfileField<Int?> = ProfileField(1490, "The CMCI server port"),
  val user: ProfileField<String?> = ProfileField(null, "Your username to connect to CICS"),
  val password: ProfileField<CharArray?> = ProfileField(null, "Your password to connect to CICS"),
  val regionName: ProfileField<String?> = ProfileField(null, "The name of the CICS region name to interact with"),
  val cicsPlex: ProfileField<String?> = ProfileField(null, "The name of the CICSPlex to interact with"),
  val rejectUnauthorized: ProfileField<Boolean?> = ProfileField(true, "Reject self-signed certificates"),
  val protocol: ProfileField<String?> = ProfileField("https", "Specifies CMCI protocol (http or https)")
)