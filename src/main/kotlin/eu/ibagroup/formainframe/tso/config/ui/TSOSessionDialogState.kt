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

package eu.ibagroup.formainframe.tso.config.ui

import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.tso.config.TSOSessionConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.nextUniqueValue
import org.zowe.kotlinsdk.TsoCodePage

/**
 * Data class which represents state fot TSO session dialog
 */
class TSOSessionDialogState(
  var uuid: String = "",
  var name: String = "",
  var connectionConfigUuid: String = "",
  var logonProcedure: String = "DBSPROCC",
  var charset: String = "697",
  var codepage: TsoCodePage = TsoCodePage.IBM_1047,
  var rows: Int = 24,
  var columns: Int = 80,
  var accountNumber: String = "ACCT#",
  var userGroup: String = "GROUP1",
  var regionSize: Int = 64000,
  var timeout: Long = 10,
  var maxAttempts: Int = 3,
  override var mode: DialogMode = DialogMode.CREATE
): DialogState {

  val tsoSessionConfig: TSOSessionConfig
    get() = TSOSessionConfig(
      uuid,
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

/**
 * Init a new uuid obtained from crudable for the TSO session dialog state
 */
fun TSOSessionDialogState.initEmptyUuids(crudable: Crudable): TSOSessionDialogState {
  return this.apply {
    uuid = crudable.nextUniqueValue<TSOSessionConfig, String>()
  }
}
