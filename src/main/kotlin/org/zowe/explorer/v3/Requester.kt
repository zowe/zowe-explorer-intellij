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

package org.zowe.explorer.v3

/**
 * Interface to track requests origins. Represents the elements that requested a related data
 * @property connectionConfig the related connection config
 */
interface Requester<ConnectionConfigType : ConnectionConfig> {
  val connectionConfig: ConnectionConfigType
}
