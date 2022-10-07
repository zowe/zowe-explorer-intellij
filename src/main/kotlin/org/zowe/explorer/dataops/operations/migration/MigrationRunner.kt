/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.operations.migration

import org.zowe.explorer.dataops.Query
import org.zowe.explorer.dataops.operations.OperationRunner
import org.zowe.explorer.utils.UNIT_CLASS

/**
 * Interface which represents migration runner class
 */
@Suppress("UNCHECKED_CAST")
interface MigrationRunner<Q : Query<*, Unit>> : OperationRunner<Q, Unit> {

  /**
   * Method determines if migration operation can run or not
   * @param operation - a typed operation instance
   */
  override fun canRun(operation: Q): Boolean {
    return true
  }

  override val resultClass: Class<out Unit>
    get() = UNIT_CLASS

}
