/*
 * Copyright (c) 2020-2024 IBA Group.
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

package eu.ibagroup.formainframe.tso.config

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.tso.config.ui.TSOSessionDialogState
import eu.ibagroup.formainframe.utils.crudable.EntityWithUuid
import eu.ibagroup.formainframe.utils.crudable.annotations.Column
import eu.ibagroup.formainframe.utils.crudable.annotations.ForeignKey
import org.zowe.kotlinsdk.TsoCodePage
import java.util.Objects

/**
 * Class which represents configuration for TSO session.
 * Instances of this class are saved and can be reloaded after Intellij closed
 */
class TSOSessionConfig : EntityWithUuid {

  @Column
  var name: String = ""

  @Column
  @ForeignKey(ConnectionConfig::class)
  var connectionConfigUuid: String = ""

  @Column
  var logonProcedure: String = ""

  @Column
  var charset: String = ""

  @Column
  var codepage: TsoCodePage = TsoCodePage.IBM_1047

  @Column
  var rows: Int = 0

  @Column
  var columns: Int = 0

  @Column
  var accountNumber: String? = null

  @Column
  var userGroup: String? = null

  @Column
  var regionSize: Int? = null

  @Column
  var timeout: Long = 0L

  @Column
  var maxAttempts: Int = 0

  constructor()

  constructor(
    uuid: String,
    name: String,
    connectionConfigUuid: String,
    logonProcedure: String,
    charset: String,
    codepage: TsoCodePage,
    rows: Int,
    columns: Int,
    accountNumber: String?,
    userGroup: String?,
    regionSize: Int?,
    timeout: Long,
    maxAttempts: Int
  ): super(uuid) {
    this.name = name
    this.connectionConfigUuid = connectionConfigUuid
    this.logonProcedure = logonProcedure
    this.charset = charset
    this.codepage = codepage
    this.rows = rows
    this.columns = columns
    this.accountNumber = accountNumber
    this.userGroup = userGroup
    this.regionSize = regionSize
    this.timeout = timeout
    this.maxAttempts = maxAttempts
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass || !super.equals(other) || other !is TSOSessionConfig) return false

    return name == other.name &&
        connectionConfigUuid == other.connectionConfigUuid &&
        logonProcedure == other.logonProcedure &&
        charset == other.charset &&
        codepage == other.codepage &&
        rows == other.rows &&
        columns == other.columns &&
        accountNumber == other.accountNumber &&
        userGroup == other.userGroup &&
        regionSize == other.regionSize &&
        timeout == other.timeout &&
        maxAttempts == other.maxAttempts
  }

  override fun hashCode(): Int {
    return Objects.hash(
      name,
      connectionConfigUuid,
      logonProcedure,
      charset,
      codepage,
      rows,
      columns,
      accountNumber,
      userGroup,
      regionSize,
      timeout,
      maxAttempts
    )
  }

  override fun toString(): String {
    return "TSOSessionConfig(" +
        "name='$name', " +
        "connectionConfigUuid='$connectionConfigUuid', " +
        "logonProcedure='$logonProcedure', " +
        "charset='$charset', " +
        "codepage=$codepage, " +
        "rows=$rows, " +
        "columns=$columns, " +
        "accountNumber=$accountNumber, " +
        "userGroup=$userGroup, " +
        "regionSize=$regionSize, " +
        "timeout=$timeout, " +
        "maxAttempts=$maxAttempts" +
        ")"
  }

}

/**
 * Create the TSO session dialog state from the TSO session config
 */
fun TSOSessionConfig.toDialogState(): TSOSessionDialogState {
  return TSOSessionDialogState(
    uuid = this.uuid,
    name = this.name,
    connectionConfigUuid = this.connectionConfigUuid,
    logonProcedure = this.logonProcedure,
    charset = this.charset,
    codepage = this.codepage,
    rows = this.rows,
    columns = this.columns,
    accountNumber = this.accountNumber ?: "",
    userGroup = this.userGroup ?: "",
    regionSize = this.regionSize ?: 0,
    timeout = this.timeout,
    maxAttempts = this.maxAttempts,
  )
}
