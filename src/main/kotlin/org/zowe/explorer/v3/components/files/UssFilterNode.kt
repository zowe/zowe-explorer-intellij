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

import com.intellij.openapi.project.Project
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.v3.components.ExplorerTreeComponentService
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.RefreshableNode
import org.zowe.explorer.v3.tree.nodes.Renameable
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.SymlinkMode

// TODO: doc
// TODO: finalize
class UssFilterNode(
  project: Project,
  nodeData: UssFilterNodeData,
  parent: ExplorerTreeNode
) : RefreshableNode(project, nodeData, parent), Renameable {
  override fun fetchChildren(): List<ExplorerTreeNode> {
    nodeData as UssFilterNodeData
    val connectionConfigNew = ConfigCacheService.getService()
      .getConfigFromCache(ConfigType.HTTP_CONNECTION_CONFIG_V1, nodeData.connectionConfigUuid)
      ?: throw Exception("Connection config is not found for node $this")
    connectionConfigNew as HttpConnectionConfig
    val connectionConfig = ConnectionConfig(
      connectionConfigNew.uuid,
      connectionConfigNew.name,
      "${connectionConfigNew.scheme}://${connectionConfigNew.host}:${connectionConfigNew.port}",
      !connectionConfigNew.rejectUnauthorized,
      connectionConfigNew.zVersion,
      connectionConfigNew.ussOwner
    )
    val response = api<DataAPI>(connectionConfig)
      .listUssPath(
        authorizationToken = "...",
        path = nodeData.displayName,
        depth = 0,
        followSymlinks = SymlinkMode.REPORT
      )
      .execute()

    return if (response.isSuccessful) {
      response.body()
        ?.items
        ?.filter { it.name != "." && it.name != ".." }
        ?.map {
          val ussPath = "${nodeData.displayName}/${it.name}"
          if (it.isDirectory) {
            val childNodeData = UssFolderNodeData(it.name, ussPath, connectionConfigUuid=nodeData.connectionConfigUuid)
            UssFolderNode(project, childNodeData, this)
          } else {
            val childNodeData = UssFileNodeData(it.name, ussPath, connectionConfigUuid=nodeData.connectionConfigUuid)
            UssFileNode(project,childNodeData, this)
          }
        }
        ?.toList()
        ?: produceNoItemsFoundChildren()
    } else {
      produceErrorChildren(Exception("Response was not successful"))
    }
  }

  override fun renameNode() {
    // TODO: prohibit operation when the node cannot be changed at the moment
    val dialog = RenameUssEntityDialog(project, this, nodeData.displayName)
    if (dialog.showAndGet()) {
      nodeData.displayName = dialog.state
      // TODO: update config
      if (nodeState == State.LOADED) {
        refreshNode()
      } else {
        ExplorerTreeComponentService.getService()
          .getFilesExplorerComponent(project)
          .invalidateNode(this, false)
      }
    }
  }
}
