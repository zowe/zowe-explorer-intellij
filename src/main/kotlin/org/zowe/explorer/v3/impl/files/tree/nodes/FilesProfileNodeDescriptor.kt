/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.tree.nodes

import org.zowe.explorer.v3.impl.formDsBasePathFromHost
import org.zowe.explorer.v3.impl.formUssBasePathFromHost
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig
import org.zowe.explorer.v3.state.config.files.FilesWorkingSetConfig
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.NoItemsFoundNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.NodeSyncService
import org.zowe.explorer.v3.tree.nodes.ProfileNodeDescriptor
import org.zowe.explorer.v3.impl.files.ds.tree.nodes.DatasetMaskNodeDescriptor
import org.zowe.explorer.v3.impl.files.uss.tree.nodes.UssFilterNodeDescriptor

/**
 * A files profile node descriptor.
 * Is designed to carry info about data set masks and USS filters, as well as functionality to manipulate them
 */
class FilesProfileNodeDescriptor(
  displayName: String,
  config: FilesWorkingSetConfig?
) : ProfileNodeDescriptor(displayName, "Files Profile", config), FilesExplorerRelated {
  /**
   * Get files profile node children elements
   * @param node the original node, associated with the descriptor
   * @return a list of [org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode]'s, that are basically USS filters and data set masks
   */
  override fun getNodeChildren(node: ExplorerTreeNode): List<ExplorerTreeNode> {
    val filesProfileConfig = config as? FilesWorkingSetConfig
      ?: throw Exception("Files profile config must not be null")
    val connectionConfig = ConfigCacheService.getService()
      .getConfigFromCache(ConfigType.HTTP_CONNECTION_CONFIG_V1, filesProfileConfig.connectionConfigUuid)
      ?: throw Exception("Connection config is not found for node descriptor $this")
    val host = (connectionConfig as HttpConnectionConfig).host

    val dsMaskNodeDescriptors = filesProfileConfig
      .dsMasks
      .map { dsMask ->
        val dsMaskNodeDescriptor = NodeSyncService.getService()
          .getOrPutFilterNodeDescriptor(
            formDsBasePathFromHost(host),
            dsMask.mask
          ) {
            DatasetMaskNodeDescriptor(dsMask.mask, filesProfileConfig.connectionConfigUuid)
          }
        ExplorerTreeNode(dsMaskNodeDescriptor, node.project, node)
      }

    val ussFilterNodeDescriptors = filesProfileConfig
      .ussPaths
      .map { ussPath ->
        val ussFilterNodeDescriptor = NodeSyncService.getService()
          .getOrPutFilterNodeDescriptor(
            formUssBasePathFromHost(host),
            ussPath.path
          ) {
            UssFilterNodeDescriptor(ussPath.path, filesProfileConfig.connectionConfigUuid)
          }
        ExplorerTreeNode(ussFilterNodeDescriptor, node.project, node)
      }
    return (dsMaskNodeDescriptors + ussFilterNodeDescriptors)
      .ifEmpty { listOf(ExplorerTreeNode(NoItemsFoundNodeDescriptor(), node.project, node)) }
  }
}