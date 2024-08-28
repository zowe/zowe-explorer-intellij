/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.tso.config.ui.table

import com.intellij.util.ui.ColumnInfo
import org.zowe.explorer.tso.config.ui.TSOSessionDialogState

/**
 * Class represents an account number column in TSO session table model.
 * It extends ColumnInfo abstract class and overloads getter and setter methods as values for this column
 */
class AccountNumberColumn: ColumnInfo<TSOSessionDialogState, String>("Account Number") {

  /**
   * Overloaded getter method of ColumnInfo abstract class
   */
  override fun valueOf(state: TSOSessionDialogState): String {
    return state.accountNumber
  }

  /**
   * Overloaded setter method of ColumnInfo abstract class
   */
  override fun setValue(state: TSOSessionDialogState, value: String) {
    state.accountNumber = value
  }

}
