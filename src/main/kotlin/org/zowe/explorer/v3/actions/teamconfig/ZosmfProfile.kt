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
 * Holds the fields for the Zowe **z/OSMF** profile section.
 *
 * Enables connectivity to the z/OS Management Facility REST API, which is the primary
 * back-end for dataset and USS operations in Zowe Explorer.
 *
 * @property shouldCreate when `true`, this profile is included in the generated config file.
 * @property host z/OSMF server hostname.
 * @property port z/OSMF server port (default `443`).
 * @property user mainframe username.
 * @property password mainframe password (stored as a secure field).
 * @property rejectUnauthorized whether to reject self-signed TLS certificates (default `true`).
 * @property certFile path to a client certificate file.
 * @property certKeyFile path to the private key matching [certFile].
 * @property basePath base path for API Mediation Layer routing; omit when not using AML.
 * @property protocol HTTP protocol to use: `"http"` or `"https"` (default `"https"`).
 * @property encoding transfer encoding for z/OS datasets and USS files (default `IBM-1047`).
 * @property responseTimeout maximum seconds the z/OSMF Files TSO servlet may run (5–600).
 */
data class ZosmfProfile(
  var shouldCreate: Boolean = false,
  val host: ProfileField<String?> = ProfileField(null, "The z/OSMF server host name"),
  val port: ProfileField<Int?> = ProfileField(443, "The z/OSMF server port"),
  val user: ProfileField<String?> = ProfileField(null, "Mainframe (z/OSMF) user name, which can be the same as your TSO login"),
  val password: ProfileField<CharArray?> = ProfileField(null, "Mainframe (z/OSMF) password, which can be the same as your TSO password"),
  val rejectUnauthorized: ProfileField<Boolean?> = ProfileField(true, "Reject self-signed certificates"),
  val certFile: ProfileField<String?> = ProfileField(null, "The file path to a certificate file to use for authentication"),
  val certKeyFile: ProfileField<String?> = ProfileField(null, "The file path to a certificate key file to use for authentication"),
  val basePath: ProfileField<String?> = ProfileField(null, "The base path for your API mediation layer instance. Do not specify this option if you are not using an API mediation layer"),
  val protocol: ProfileField<String?> = ProfileField("https", "The protocol used (HTTP or HTTPS)"),
  val encoding: ProfileField<String?> = ProfileField(null, "The encoding for download and upload of z/OS data set and USS files. The default encoding if not specified is IBM-1047"),
  val responseTimeout: ProfileField<Int?> = ProfileField(null, "The maximum amount of time in seconds the z/OSMF Files TSO servlet should run before returning a response. Allowed values: 5 - 600")
)