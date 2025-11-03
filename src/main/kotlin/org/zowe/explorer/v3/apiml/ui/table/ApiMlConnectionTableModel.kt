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

package org.zowe.explorer.v3.apiml.ui.table

import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.credentials.cache.CredentialsCacheService
import org.zowe.explorer.v3.state.config.connection.ApiMlConnectionConfig
import org.zowe.explorer.v3.apiml.ui.ApiMlConnectionDialogState
import org.zowe.explorer.v3.apiml.ui.table.column.ConnectionNameColumn
import org.zowe.explorer.v3.apiml.ui.table.column.ConnectionUrlColumn
import org.zowe.explorer.v3.apiml.ui.table.column.ConnectionUsernameColumn
import org.zowe.explorer.v3.apiml.ui.table.column.ConnectionUssOwnerColumn
import org.zowe.explorer.v3.apiml.ui.toDialogState
import org.zowe.explorer.v3.ui.table.TableModel

class ApiMlConnectionTableModel: TableModel<ApiMlConnectionDialogState>(
  ConnectionNameColumn(),
  ConnectionUrlColumn(),
  ConnectionUsernameColumn(),
  ConnectionUssOwnerColumn()
) {

  override val clazz = ApiMlConnectionDialogState::class.java

  override fun fetch(): MutableList<ApiMlConnectionDialogState> {
    return ConfigCacheService.getService()
      .getConfigsFromCache(ConfigType.API_ML_CONNECTION_CONFIG_V1)
      .toList()
      .filterIsInstance<ApiMlConnectionConfig>()
      .map { it.toDialogState() }
      .toMutableList()
  }

//  override fun onApplyingMergedCollection(merged: MergedCollections<ApiMlConnectionDialogState>) {
//    TODO("Not yet implemented")
//  }

  override fun onDelete(value: ApiMlConnectionDialogState) {
    ConfigCacheService.getService()
      .deleteConfigFromCache(value.apiMlConnectionConfig)
    CredentialsCacheService.getService()
      .deleteCredentialsFromCache(value.credentials)
  }

  override fun onUpdate(value: ApiMlConnectionDialogState): Boolean {
    ConfigCacheService.getService()
      .updateConfigInCache(value.apiMlConnectionConfig)
    CredentialsCacheService.getService()
      .updateCredentialsInCache(value.credentials)
    return true
  }

  override fun onAdd(value: ApiMlConnectionDialogState): Boolean {
    ConfigCacheService.getService()
      .addConfigToCache(value.apiMlConnectionConfig)
    CredentialsCacheService.getService()
      .addCredentialsToCache(value.credentials)
    return true
  }

  override fun set(row: Int, item: ApiMlConnectionDialogState) {
    // TODO: check current impl
    super.set(row, item)
  }

  init {
    initialize()
  }

}