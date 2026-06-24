/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.uss.tree.nodes

import org.zowe.explorer.v3.impl.files.tree.nodes.FilesExplorerRelated
import org.zowe.explorer.v3.impl.files.uss.operations.LoadUssNodesOperation
import org.zowe.explorer.v3.impl.files.uss.operations.RefreshUssNodesOperation
import org.zowe.explorer.v3.impl.formUssBasePathFromHost
import org.zowe.explorer.v3.impl.files.uss.operations.LoadUssNodesOperationData
import org.zowe.explorer.v3.impl.files.uss.operations.RefreshUssNodesOperationData
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.FetcherNodeDescriptor
import javax.swing.Icon

// TODO: doc
abstract class UssFetcherNodeDescriptor(
  displayName: String,
  protected val filterPath: List<String>,
  tooltip: String,
  icon: Icon,
  connectionConfigUuid: String
) : FetcherNodeDescriptor(
  displayName,
  basePath = formUssBasePathFromConnectionConfig(connectionConfigUuid),
  tooltip,
  icon,
  connectionConfigUuid = connectionConfigUuid
), FilesExplorerRelated {
  companion object {
    fun formUssBasePathFromConnectionConfig(connectionConfigUuid: String): List<String> {
      val connectionConfig = ConfigCacheService.getService()
        .getConfigFromCache(ConfigType.HTTP_CONNECTION_CONFIG_V1, connectionConfigUuid)
        ?: throw Exception("Connection config is not found for node $this")
      val host = (connectionConfig as HttpConnectionConfig).host
      return formUssBasePathFromHost(host)
    }

    fun formUssFilterPath(displayName: String): List<String> {
      return if (displayName == "/") listOf(displayName)
        else displayName.split("/").map { "$it/"}
    }
  }

  protected val fetchPath = basePath + filterPath

  override fun generateLoadNodesOperation(node: ExplorerTreeNode): LoadUssNodesOperation {
    return LoadUssNodesOperation(
      LoadUssNodesOperationData(node, fetchPath, fetchFilter)
    )
  }

  override fun generateRefreshNodesOperation(node: ExplorerTreeNode): RefreshUssNodesOperation {
    return RefreshUssNodesOperation(
      RefreshUssNodesOperationData(node, fetchPath, fetchFilter)
    )
  }
}
