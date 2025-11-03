/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Dzianis Lisiankou
 */

package org.zowe.explorer.v3.operations.apiml

import org.zowe.explorer.v3.Requester
import org.zowe.explorer.v3.operations.UnitOperationData
import org.zowe.explorer.v3.state.config.connection.ApiMlConnectionConfig

data class ApiMlLoginOperationData<ConnectionConfigType : ApiMlConnectionConfig>(
  val username: String,
  val password: CharArray,
  override val origin: Requester<ConnectionConfigType>
): UnitOperationData<ConnectionConfigType>