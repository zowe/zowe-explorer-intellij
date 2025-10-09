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

package org.zowe.explorer.v3.components.files.operations

import io.ktor.http.isSuccess
import org.zowe.explorer.v3.api.ZosmfApiService
import org.zowe.explorer.v3.components.files.UssFileNodeDescriptor
import org.zowe.explorer.v3.newoperations.LoadNodesOperation
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.ConnectionConfigRelated
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig
import org.zowe.explorer.v3.tree.nodes.ErrorNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.NoItemsFoundNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.NodeSyncService
import org.zowe.kotlinsdk.providers.zowe.UserPassHttpConnection
import org.zowe.kotlinsdk.providers.zowe.zosmf.files.definitions.ZosmfSymlinkMode
import org.zowe.kotlinsdk.providers.zowe.zosmf.files.messaging.ZosmfListFilesRequest
import org.zowe.kotlinsdk.providers.zowe.zosmf.files.messaging.ZosmfListFilesResponse

// TODO: doc
class LoadUssNodesOperation(
  override val operationData: LoadUssNodesOperationData
) : LoadNodesOperation {
  override suspend fun fetchChildren(): List<ExplorerTreeNode> {
    val parentNode = operationData.node
    val parentNodeData = parentNode.nodeDescriptor as ConnectionConfigRelated
    val connectionConfig = ConfigCacheService.getService()
      .getConfigFromCache(ConfigType.HTTP_CONNECTION_CONFIG_V1, parentNodeData.connectionConfigUuid)
      ?: throw Exception("Connection config is not found for node $this")
    connectionConfig as HttpConnectionConfig
    val listFilesRequest = ZosmfListFilesRequest(
      UserPassHttpConnection(
        connectionConfig.host,
        connectionConfig.port,
        user = "...",
        password = "..."
      ),
      filter = operationData.filter,
      depth = 0,
      followSymlinks = ZosmfSymlinkMode.REPORT
    )
    val listFilesResponse = ZosmfApiService.getService()
      .files
      .listFiles(listFilesRequest) as ZosmfListFilesResponse
    return if (!listFilesResponse.status.isSuccess()) {
      val errorCode = listFilesResponse.status.value
      val errorDescription = listFilesResponse.status.description
      listOf(
        ExplorerTreeNode(
          ErrorNodeDescriptor("Host returned $errorCode: $errorDescription"),
          operationData.node.project,
          operationData.node
        )
      )
    } else {
      listFilesResponse
        .items
        .filter { it.name != "." && it.name != ".." }
        .map {
          val ussPath = operationData.path + it.name
          val childeNode = ExplorerTreeNode(
            UssFileNodeDescriptor(it.name, ussPath, parentNodeData.connectionConfigUuid),
            parentNode.project,
            parentNode
          )
//        val childNode = if (it.fileType == FileItem.FileType.DIRECTORY) {
//          val childNodeData = UssFolderNodeData(it.name, ussPath, connectionConfigUuid = nodeDescriptor.connectionConfigUuid)
//          UssFolderNode(project, childNodeData, this)
//        } else {
//          val childNodeData = UssFileNodeData(it.name, ussPath, connectionConfigUuid=nodeDescriptor.connectionConfigUuid)
//          UssFileNode(project,childNodeData, this)
//        }
          childeNode
        }
        .ifEmpty { listOf(ExplorerTreeNode(NoItemsFoundNodeDescriptor(), parentNode.project, parentNode)) }
    }
  }

  override fun run(): LoadUssNodesOperationResult {
    val loadedNodes = NodeSyncService.getService().loadNodes(this)
    return LoadUssNodesOperationResult(loadedNodes)
  }
}
