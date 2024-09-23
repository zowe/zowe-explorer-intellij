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

package eu.ibagroup.formainframe.dataops.operations

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.Operation
import org.zowe.kotlinsdk.SystemsResponse

/**
 * Base class which represents an operation to be performed against systems info defined to z/OSMF particular instance
 * @param connectionConfig represents a connection config object
 */
class InfoOperation(var connectionConfig: ConnectionConfig) : Operation<SystemsResponse> {
  override val resultClass = SystemsResponse::class.java
}
