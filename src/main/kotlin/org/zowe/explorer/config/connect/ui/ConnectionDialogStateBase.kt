/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package org.zowe.explorer.config.connect.ui

import org.zowe.explorer.common.ui.DialogState
import org.zowe.explorer.config.connect.ConnectionConfigBase
import org.zowe.explorer.config.connect.Credentials

/**
 * Bases dialog state for each connection (z/OSMF, CICS and etc.).
 * @author Valiantsin Krus
 */
abstract class ConnectionDialogStateBase<ConnectionConfig: ConnectionConfigBase>(): DialogState, Cloneable {
  abstract var connectionUuid: String
  abstract var connectionName: String
  abstract var username: String
  abstract var password: String
  abstract var connectionUrl: String
  abstract var credentials: Credentials
  abstract var connectionConfig: ConnectionConfig
}
