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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.components.files

import com.intellij.icons.AllIcons
import org.zowe.explorer.v3.components.files.operations.LoadUssNodesOperation
import org.zowe.explorer.v3.components.files.operations.LoadUssNodesOperationData
import org.zowe.explorer.v3.components.files.operations.RefreshUssNodesOperation
import org.zowe.explorer.v3.components.files.operations.RefreshUssNodesOperationData
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import org.zowe.explorer.v3.tree.nodes.Renameable
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.FileFetcherNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.LazyExpandable
import org.zowe.explorer.v3.tree.nodes.NoItemsFoundNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.Refreshable

// TODO: doc
class UssFilterNodeDescriptor(
  displayName: String,
  override var connectionConfigUuid: String,
) : FileFetcherNodeDescriptor(
  displayName,
  "USS filter",
  AllIcons.Nodes.Module,
  connectionConfigUuid=connectionConfigUuid
), Renameable, LazyExpandable, Refreshable {
  override val path: List<String>
    get() {
      val connectionConfig = ConfigCacheService.getService()
        .getConfigFromCache(ConfigType.HTTP_CONNECTION_CONFIG_V1, connectionConfigUuid)
        ?: throw Exception("Connection config is not found for node $this")
      val host = (connectionConfig as HttpConnectionConfig).host
      val filterPath =
        if (displayName == "/") listOf(displayName)
        else displayName.split("/").map { "$it/"}
      return listOf(host, "files", "uss") + filterPath
    }

  override var wasExpanded = false

  override fun expandNode(node: ExplorerTreeNode) {
    wasExpanded = true
    val explorerComponent = ExplorerTreeComponentService.getService()
      .getFilesExplorerComponent(node.project)
    explorerComponent.invalidateNode(node)
  }

  override fun getNodeChildren(node: ExplorerTreeNode): List<ExplorerTreeNode> {
    return if (connectionConfigUuid.isEmpty()) {
      listOf(
        ExplorerTreeNode(
          NoItemsFoundNodeDescriptor("connection is not set"),
          node.project,
          node
        )
      )
    } else {
      if (wasExpanded) {
        val operation = LoadUssNodesOperation(
          LoadUssNodesOperationData(node, path, displayName)
        )
        operation.run().loadedNodes
      } else {
        listOf(ExplorerTreeNode(NoItemsFoundNodeDescriptor(), node.project, node))
      }
    }
  }

  override fun isNodeReadyForRefresh(node: ExplorerTreeNode): Boolean {
    return connectionConfigUuid.isNotEmpty() && wasExpanded
  }

  override fun refreshNode(node: ExplorerTreeNode) {
    val operation = RefreshUssNodesOperation(
      RefreshUssNodesOperationData(node, path, displayName)
    )
    operation.run()
  }

  override fun renameNode() {
    TODO("Not yet implemented")
//    if (nodeState == State.BUSY) {
//      NotificationsService.getService()
//        .notifyWarning(
//          project,
//          "Node is busy",
//          "Rename is not possible while the node has an active job in progress",
//          ""
//        )
//    } else {
//      val dialog = RenameUssEntityDialog(project, this, nodeDescriptor.displayName)
//      if (dialog.showAndGet()) {
//        NodeSyncService.getService()
//          .unregisterNode(this)
//        nodeDescriptor.displayName = dialog.state
//        // TODO: update config, update the node by the config after?
//        if (nodeState == State.LOADED) {
//          refreshNode()
//        } else {
//          ExplorerTreeComponentService.getService()
//            .getFilesExplorerComponent(project)
//            .invalidateNode(this, false)
//        }
//      }
//    }
  }
}