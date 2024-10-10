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

package org.zowe.explorer.v3.state.config

/**
 * Interface to specify that instances of the inherited classes are a [ConnectionConfig]-related classes.
 * The relation is set up by the [connectionConfigUuid], that is unique for each [ConnectionConfig]
 */
interface ConnectionConfigRelated {
  val connectionConfigUuid: String
}
