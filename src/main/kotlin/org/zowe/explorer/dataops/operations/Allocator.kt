/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.operations

import org.zowe.explorer.dataops.Query
import org.zowe.explorer.utils.UNIT_CLASS

@Suppress("UNCHECKED_CAST")
interface Allocator<Q: Query<*, Unit>> : OperationRunner<Q, Unit> {

  override fun canRun(operation: Q): Boolean {
    return true
  }

  override val resultClass: Class<out Unit>
    get() = UNIT_CLASS

}
