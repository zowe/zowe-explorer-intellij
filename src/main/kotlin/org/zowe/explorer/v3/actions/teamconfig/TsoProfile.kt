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
 * Holds the fields for the Zowe **TSO/E** profile section.
 *
 * Used to configure TSO/E address space parameters when starting interactive TSO
 * sessions.
 *
 * @property shouldCreate when `true`, this profile is included in the generated config file.
 * @property account z/OS TSO/E accounting information.
 * @property characterSet character set for EBCDIC conversion (default `"697"`).
 * @property codePage code page for EBCDIC conversion (default `"1047"`).
 * @property columns number of columns on the virtual screen (default `80`).
 * @property logonProcedure logon procedure used to start the TSO/E address space (default `"IZUFPROC"`).
 * @property regionSize region size in kilobytes for the TSO/E address space (default `4096`).
 * @property rows number of rows on the virtual screen (default `24`).
 */
data class TsoProfile(
  var shouldCreate: Boolean = false,
  val account: ProfileField<String?> = ProfileField(null, "Your z/OS TSO/E accounting information"),
  val characterSet: ProfileField<String?> = ProfileField("697", "Character set for address space to convert messages and responses from UTF-8 to EBCDIC"),
  val codePage: ProfileField<String?> = ProfileField("1047", "Codepage value for TSO/E address space to convert messages and responses from UTF-8 to EBCDIC"),
  val columns: ProfileField<Int?> = ProfileField(80, "The number of columns on a screen"),
  val logonProcedure: ProfileField<String?> = ProfileField("IZUFPROC", "The logon procedure to use when creating TSO procedures on your behalf"),
  val regionSize: ProfileField<Int?> = ProfileField(4096, "Region size for the TSO/E address space"),
  val rows: ProfileField<Int?> = ProfileField(24, "The number of rows on a screen")
)