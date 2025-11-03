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

package org.zowe.explorer.v3

import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig

/** Class to track USS requests origins */
class UssRequester<ConnectionConfigType : HttpConnectionConfig>(
  override val connectionConfig: ConnectionConfigType
) : Requester<ConnectionConfigType>
