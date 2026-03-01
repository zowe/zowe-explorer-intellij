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
 * Holds the fields for the Zowe **IBM MQ** profile section.
 *
 * @property shouldCreate when `true`, this profile is included in the generated config file.
 * @property host hostname used to access the IBM MQ REST API.
 * @property port port number used to access the IBM MQ REST API.
 * @property user MQ username (may be the same as the TSO login).
 * @property password MQ password (stored as a secure field; may be the same as the TSO password).
 * @property rejectUnauthorized whether to reject self-signed TLS certificates (default `false`).
 * @property protocol MQ REST API protocol: `"http"` or `"https"` (default `"https"`).
 */
data class MqProfile(
  var shouldCreate: Boolean = false,
  val host: ProfileField<String?> = ProfileField(null, "The host name used to access the IBM MQ REST API"),
  val port: ProfileField<Int?> = ProfileField(null, "The port number used to access the IBM MQ REST API"),
  val user: ProfileField<String?> = ProfileField(null, "The mainframe (MQ) user name, which can be the same as your TSO login"),
  val password: ProfileField<CharArray?> = ProfileField(null, "The mainframe (MQ) password, which can be the same as your TSO password"),
  val rejectUnauthorized: ProfileField<Boolean?> = ProfileField(false, "Reject self-signed certificates"),
  val protocol: ProfileField<String?> = ProfileField("https", "Specifies the MQ protocol (http or https)")
)