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
  override var shouldCreate: Boolean = false,
  val host: ProfileField<String?> = ProfileField(null, "Host", "host", "The z/OS SSH server host name"),
  val port: ProfileField<Int?> = ProfileField(22, "Port", "port", "The z/OS SSH server port"),
  val user: ProfileField<String?> = ProfileField(null, "User", "user", "Mainframe user name, which can be the same as your TSO login"),
  val password: ProfileField<CharArray?> = ProfileField(null, "Password", "password", "Mainframe password, which can be the same as your TSO password"),
  val privateKey: ProfileField<String?> = ProfileField(null, "Private key", "privateKey", "Path to a file containing your private key, that must match a public key stored in the server for authentication"),
  val keyPassphrase: ProfileField<CharArray?> = ProfileField(null, "Key passphrase", "keyPassphrase", "Private key passphrase, which unlocks the private key"),
  val handshakeTimeout: ProfileField<Int?> = ProfileField(null, "Handshake timeout", "handshakeTimeout", "How long in milliseconds to wait for the SSH handshake to complete")
) : ConfigProfile {
  override val profileName: String = "SSH profile"
  override val profileType: String = "ssh"

  override fun jsonFriendly(): Map<String, Any> = entry(
    profileType,
    mapOf(
      host.prop,
      port.prop,
      user.prop,
      privateKey.prop,
      handshakeTimeout.prop
    ),
    listOf(password.nameInConfig, keyPassphrase.nameInConfig)
  )
}