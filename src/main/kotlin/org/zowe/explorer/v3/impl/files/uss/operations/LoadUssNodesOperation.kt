/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.uss.operations

import io.ktor.http.isSuccess
import org.zowe.explorer.v3.impl.api.ZosmfApiService
import org.zowe.explorer.v3.impl.files.uss.tree.nodes.UssFileNodeDescriptor
import org.zowe.explorer.v3.impl.files.uss.tree.nodes.UssFolderNodeDescriptor
import org.zowe.explorer.v3.impl.connection.ZoweConnectionService
import org.zowe.explorer.v3.newoperations.LoadNodesOperation
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.ConnectionConfigRelated
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig
import org.zowe.explorer.v3.tree.nodes.ErrorNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.FetcherNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.NoItemsFoundNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.NodeSyncService
import org.zowe.kotlinsdk.core.StatusType
import org.zowe.kotlinsdk.core.files.data.FileItem
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
    val zoweConnectionManager = ZoweConnectionService.getService()
      .getZoweConnectionManager(parentNode.project)
    val httpConnection = zoweConnectionManager.produceHttpConnection("zosmf", shouldOverrideWithEnv = true)
    connectionConfig as HttpConnectionConfig
    val listFilesRequest = ZosmfListFilesRequest(
      httpConnection,
      filter = operationData.filter,
      depth = 0,
      followSymlinks = ZosmfSymlinkMode.REPORT
    )
    val listFilesResponse = ZosmfApiService.getService()
      .files
      .listFiles(listFilesRequest) as ZosmfListFilesResponse
    return if (listFilesResponse.status.type != StatusType.SUCCESS) {
      listOf(
        ExplorerTreeNode(
          ErrorNodeDescriptor("Host returned ${listFilesResponse.status.text}"),
          operationData.node.project,
          operationData.node
        )
      )
    } else {
      listFilesResponse
        .items
        .filter { it.name != "." && it.name != ".." }
        .map { ussEntity ->
          val ussNodeDescriptor = if (ussEntity.fileType == FileItem.FileType.DIRECTORY) {
            NodeSyncService.getService()
              .getOrPutRealNodeDescriptor(operationData.path, ussEntity.name) {
                UssFolderNodeDescriptor(
                  ussEntity.name,
                  operationData.filter,
                  connectionConfigUuid = parentNodeData.connectionConfigUuid
                )
              }
          } else {
            NodeSyncService.getService()
              .getOrPutRealNodeDescriptor(operationData.path, ussEntity.name) {
                UssFileNodeDescriptor(
                  ussEntity.name,
                  operationData.path,
                  connectionConfigUuid = parentNodeData.connectionConfigUuid
                )
              }
          }
          ExplorerTreeNode(ussNodeDescriptor, parentNode.project, parentNode)
        }
        .ifEmpty { listOf(ExplorerTreeNode(NoItemsFoundNodeDescriptor(), parentNode.project, parentNode)) }
    }
  }

  override fun setNodesRefreshInfo(fetcherNodeDescriptor: FetcherNodeDescriptor) {
    fetcherNodeDescriptor.setUpdateInfo()
  }

  override fun run(): LoadUssNodesOperationResult {
    val loadedNodes = NodeSyncService.getService().loadNodesForFetcherFilter(this)
    return LoadUssNodesOperationResult(loadedNodes)
  }
}
