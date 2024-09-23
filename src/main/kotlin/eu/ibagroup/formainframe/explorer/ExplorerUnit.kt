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

package eu.ibagroup.formainframe.explorer

import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase

/** Interface to represent base unit in explorer */
interface ExplorerUnit<Connection: ConnectionConfigBase> {

  val name: String

  val uuid: String

  val connectionConfig: Connection?

  val explorer: Explorer<Connection, *>

}
