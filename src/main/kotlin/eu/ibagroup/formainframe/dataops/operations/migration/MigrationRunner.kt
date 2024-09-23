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

package eu.ibagroup.formainframe.dataops.operations.migration

import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.utils.UNIT_CLASS

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
