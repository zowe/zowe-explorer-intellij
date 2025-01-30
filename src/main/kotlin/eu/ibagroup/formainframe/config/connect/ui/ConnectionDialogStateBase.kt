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
package eu.ibagroup.formainframe.config.connect.ui

import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.config.connect.Credentials

/**
 * Bases dialog state for each connection (z/OSMF, CICS and etc.).
 * @author Valiantsin Krus
 */
abstract class ConnectionDialogStateBase<ConnectionConfig: ConnectionConfigBase>(): DialogState, Cloneable {
  abstract var connectionUuid: String
  abstract var connectionName: String
  abstract var username: String
  abstract var owner: String
  abstract var password: CharArray
  abstract var connectionUrl: String
  abstract var credentials: Credentials
  abstract var connectionConfig: ConnectionConfig
}
