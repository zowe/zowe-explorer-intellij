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

package org.zowe.explorer.v3.operations

import org.zowe.explorer.v3.ConnectionConfig

/**
 * Interface to create operations without expecting any exact result
 */
interface UnitOperationData<ConnectionConfigType : ConnectionConfig> : OperationData<Unit, ConnectionConfigType> {
  override val resultClass: Class<out Unit>
    get() = Unit::class.java
}
