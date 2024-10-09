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

package org.zowe.explorer.apiml.config.ui.table

import org.zowe.explorer.apiml.config.ApiMLConnectionConfig
import org.zowe.explorer.apiml.config.ui.ApiMLConnectionDialogState
import org.zowe.explorer.config.connect.ui.ConnectionsTableModelBase
import org.zowe.explorer.utils.crudable.Crudable

class ApiMLConnectionTableModel(
  crudable: Crudable
) : ConnectionsTableModelBase<ApiMLConnectionConfig, ApiMLConnectionDialogState>(crudable) {

  override val clazz = ApiMLConnectionDialogState::class.java

  override val connectionConfigClass = ApiMLConnectionConfig::class.java

  init {
    initialize()
  }

  override fun connectionToDialogState(
    connectionConfig: ApiMLConnectionConfig,
    crudable: Crudable
  ): ApiMLConnectionDialogState {
    return connectionConfig.toDialogState()
  }

}