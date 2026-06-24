/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.jes.tree.nodes

import org.zowe.explorer.v3.impl.formJesBasePathFromHost
import org.zowe.explorer.v3.impl.formJobFilterName
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig
import org.zowe.explorer.v3.state.config.jes.JesWorkingSetConfig
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.NoItemsFoundNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.NodeSyncService
import org.zowe.explorer.v3.tree.nodes.ProfileNodeDescriptor

// TODO: doc
class JesProfileNodeDescriptor(
  displayName: String,
  config: JesWorkingSetConfig?
) : ProfileNodeDescriptor(displayName, "JES Profile", config), JesExplorerRelated {
  override fun getNodeChildren(node: ExplorerTreeNode): List<ExplorerTreeNode> {
    val jesProfileConfig = config as? JesWorkingSetConfig
      ?: throw Exception("JES profile config must not be null")
    val connectionConfig = ConfigCacheService.getService()
      .getConfigFromCache(ConfigType.HTTP_CONNECTION_CONFIG_V1, jesProfileConfig.connectionConfigUuid)
      ?: throw Exception("Connection config is not found for node descriptor $this")
    val host = (connectionConfig as HttpConnectionConfig).host

    return jesProfileConfig
      .jobFilters
      .map { jobFilter ->
        val jobFilterNodeDescriptor = NodeSyncService.getService()
          .getOrPutFilterNodeDescriptor(
            formJesBasePathFromHost(host),
            formJobFilterName(jobFilter.prefix, jobFilter.owner, jobFilter.jobId)
          ) {
            JobFilterNodeDescriptor(
              jobFilter.prefix,
              jobFilter.owner,
              jobFilter.jobId,
              jesProfileConfig.connectionConfigUuid
            )
          }
        ExplorerTreeNode(jobFilterNodeDescriptor, node.project, node)
      }
      .ifEmpty { listOf(ExplorerTreeNode(NoItemsFoundNodeDescriptor(), node.project, node)) }
  }
}