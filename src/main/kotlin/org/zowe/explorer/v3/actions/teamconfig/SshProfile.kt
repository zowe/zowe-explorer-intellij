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
 * Holds the fields for the Zowe **SSH** profile section.
 * 
 * @property shouldCreate when `true`, this profile is included in the generated config file.
 * @property host z/OS SSH server hostname.
 * @property port z/OS SSH server port (default `22`).
 * @property user mainframe username.
 * @property password mainframe password (stored as a secure field); alternative to key-based auth.
 * @property privateKey path to the private key file for key-based authentication.
 * @property keyPassphrase passphrase that unlocks [privateKey] (stored as a secure field).
 * @property handshakeTimeout maximum time in milliseconds to wait for the SSH handshake.
 */
data class SshProfile(
  var shouldCreate: Boolean = false,
  val host: ProfileField<String?> = ProfileField(null, "The z/OS SSH server host name"),
  val port: ProfileField<Int?> = ProfileField(22, "The z/OS SSH server port"),
  val user: ProfileField<String?> = ProfileField(null, "Mainframe user name, which can be the same as your TSO login"),
  val password: ProfileField<CharArray?> = ProfileField(null, "Mainframe password, which can be the same as your TSO password"),
  val privateKey: ProfileField<String?> = ProfileField(null, "Path to a file containing your private key, that must match a public key stored in the server for authentication"),
  val keyPassphrase: ProfileField<CharArray?> = ProfileField(null, "Private key passphrase, which unlocks the private key"),
  val handshakeTimeout: ProfileField<Int?> = ProfileField(null, "How long in milliseconds to wait for the SSH handshake to complete")
)