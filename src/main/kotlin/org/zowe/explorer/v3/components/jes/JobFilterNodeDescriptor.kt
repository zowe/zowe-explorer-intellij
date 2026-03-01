/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.components.jes

import org.zowe.explorer.v3.components.jes.operations.LoadJobFilterNodesOperation
import org.zowe.explorer.v3.components.formJesBasePathFromHost
import org.zowe.explorer.v3.components.formJobFilterName
import org.zowe.explorer.v3.components.jes.operations.LoadJobFilterNodesOperationData
import org.zowe.explorer.v3.components.jes.operations.RefreshJobFilterNodesOperation
import org.zowe.explorer.v3.components.jes.operations.RefreshJobFilterNodesOperationData
import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.PlainFilterNodeDescriptor

// TODO: doc
class JobFilterNodeDescriptor(
  prefix: String,
  owner: String,
  jobId: String,
  connectionConfigUuid: String
) : PlainFilterNodeDescriptor(
  formJobFilterName(prefix, owner, jobId),
  basePath = formJesBasePathFromConnectionConfig(connectionConfigUuid),
  "Data set mask",
  ZoweExplorerIcons.datasetMask,
  connectionConfigUuid
) {
  companion object {
    fun formJesBasePathFromConnectionConfig(connectionConfigUuid: String): List<String> {
      val connectionConfig = ConfigCacheService.getService()
        .getConfigFromCache(ConfigType.HTTP_CONNECTION_CONFIG_V1, connectionConfigUuid)
        ?: throw Exception("Connection config is not found for node $this")
      val host = (connectionConfig as HttpConnectionConfig).host
      return formJesBasePathFromHost(host)
    }
  }

  override fun generateLoadNodesOperation(node: ExplorerTreeNode): LoadJobFilterNodesOperation {
    return LoadJobFilterNodesOperation(
      LoadJobFilterNodesOperationData(node, basePath, fetchFilter)
    )
  }

  override fun generateRefreshNodesOperation(node: ExplorerTreeNode): RefreshJobFilterNodesOperation {
    return RefreshJobFilterNodesOperation(
      RefreshJobFilterNodesOperationData(node, basePath, fetchFilter)
    )
  }

  // TODO: doc
  override fun checkMatchesFilter(elemName: String): Boolean {
    // TODO: complete
    return true
  }
}