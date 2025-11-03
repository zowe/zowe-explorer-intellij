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
 *   Dzianis Lisiankou
 */

package org.zowe.explorer.dataops.operations

import org.zowe.explorer.dataops.Operation
import org.zowe.kotlinsdk.InfoResponse

/**
 * Base class which represents an operation to be performed against system where z/OSMF instance is running
 * @param connectionConfig represents a connection config object
 */
class ZOSInfoOperation(var connectionConfig: Any) : Operation<InfoResponse> {
  override val resultClass = InfoResponse::class.java
}
