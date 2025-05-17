/*
 * Copyright (c) 2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package org.zowe.explorer.v3.state.config.tso

import org.zowe.explorer.v3.state.config.Config
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.ConnectionConfigRelated
import org.zowe.kotlinsdk.TsoCodePage

/**
 * TSO session config class to describe a TSO session information, specified by a user
 * @property name the name of the TSO session
 * @property logonProcedure the LOGON procedure to use in the session
 * @property charset the charset to use in the session
 * @property codepage the codepage to use in the session
 * @property rows the amount of the rows expected in the TSO session
 * @property columns the amount of the columns expected in the TSO session
 * @property accountNumber the account number to use in the session
 * @property userGroup the user group to use in the session
 * @property regionSize the region size for the session
 * @property timeout the timeout to consider the TSO session connection is not succeeded after
 * @property maxAttempts the max attempts to try to connection to the TSO
 */
class TsoSessionConfig(
  uuid: String = EMPTY_ID,
  configType: ConfigType = ConfigType.TSO_SESSION_CONFIG_V1,
  var name: String = "",
  override var connectionConfigUuid: String = "",
  var logonProcedure: String = "",
  var charset: String = "",
  var codepage: TsoCodePage = TsoCodePage.IBM_1047,
  var rows: Int = 0,
  var columns: Int = 0,
  var accountNumber: String? = null,
  var userGroup: String? = null,
  var regionSize: Int? = null,
  var timeout: Long = 0L,
  var maxAttempts: Int = 0
) : ConnectionConfigRelated, Config(uuid, configType) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TsoSessionConfig) return false
    if (!super.equals(other)) return false

    if (rows != other.rows) return false
    if (columns != other.columns) return false
    if (regionSize != other.regionSize) return false
    if (timeout != other.timeout) return false
    if (maxAttempts != other.maxAttempts) return false
    if (name != other.name) return false
    if (connectionConfigUuid != other.connectionConfigUuid) return false
    if (logonProcedure != other.logonProcedure) return false
    if (charset != other.charset) return false
    if (codepage != other.codepage) return false
    if (accountNumber != other.accountNumber) return false
    if (userGroup != other.userGroup) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + rows
    result = 31 * result + columns
    result = 31 * result + (regionSize ?: 0)
    result = 31 * result + timeout.hashCode()
    result = 31 * result + maxAttempts
    result = 31 * result + name.hashCode()
    result = 31 * result + connectionConfigUuid.hashCode()
    result = 31 * result + logonProcedure.hashCode()
    result = 31 * result + charset.hashCode()
    result = 31 * result + codepage.hashCode()
    result = 31 * result + (accountNumber?.hashCode() ?: 0)
    result = 31 * result + (userGroup?.hashCode() ?: 0)
    return result
  }
}
