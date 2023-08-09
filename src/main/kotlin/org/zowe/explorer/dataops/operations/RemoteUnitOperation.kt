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

import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.utils.UNIT_CLASS

/** Interface to represent a remote operation, the result of that operation is Unit */
interface RemoteUnitOperation<Request> : RemoteQuery<ConnectionConfig, Request, Unit> {
  override val resultClass
    get() = UNIT_CLASS
}
