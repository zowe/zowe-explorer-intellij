/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.components.files.operations

import io.ktor.http.isSuccess
import org.zowe.explorer.v3.api.ZosmfApiService
import org.zowe.explorer.v3.components.files.PartitionedDatasetNodeDescriptor
import org.zowe.explorer.v3.components.files.SequentialDatasetNodeDescriptor
import org.zowe.explorer.v3.connection.ZoweConnectionService
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
import org.zowe.kotlinsdk.core.datasets.data.DatasetItem
import org.zowe.kotlinsdk.providers.zowe.zosmf.datasets.messaging.ZosmfListDatasetsRequest
import org.zowe.kotlinsdk.providers.zowe.zosmf.datasets.messaging.ZosmfListDatasetsResponse

// TODO: doc
// TODO: not yet implemented, as Zowe Client Kotlin SDK is missing the respective functionality
class LoadJobFilterNodesOperation(
  override val operationData: LoadDatasetMaskNodesOperationData
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
    val listDataSetsRequest = ZosmfListDatasetsRequest(
      httpConnection,
      mask = operationData.filter
    )
    TODO("Not yet implemented")
    val listDataSetsResponse = ZosmfApiService.getService()
      .datasets
      .listDatasets(listDataSetsRequest) as ZosmfListDatasetsResponse
    return if (!listDataSetsResponse.status.isSuccess()) {
      val errorCode = listDataSetsResponse.status.value
      val errorDescription = listDataSetsResponse.status.description
      listOf(
        ExplorerTreeNode(
          ErrorNodeDescriptor("Host returned $errorCode: $errorDescription"),
          operationData.node.project,
          operationData.node
        )
      )
    } else {
      listDataSetsResponse
        .dsItems
        .map { dsEntity ->
          val dsNodeDescriptor = if (
            dsEntity.datasetOrganization == DatasetItem.DatasetOrganization.PO
            || dsEntity.datasetOrganization == DatasetItem.DatasetOrganization.POE
          ) {
            NodeSyncService.getService()
              .getOrPutRealNodeDescriptor(operationData.path, dsEntity.datasetName) {
                PartitionedDatasetNodeDescriptor(
                  dsEntity.datasetName,
                  connectionConfigUuid = parentNodeData.connectionConfigUuid
                )
              }
          } else {
            NodeSyncService.getService()
              .getOrPutRealNodeDescriptor(operationData.path, dsEntity.datasetName) {
                SequentialDatasetNodeDescriptor(
                  dsEntity.datasetName,
                  operationData.path,
                  connectionConfigUuid = parentNodeData.connectionConfigUuid
                )
              }
          }
          ExplorerTreeNode(dsNodeDescriptor, parentNode.project, parentNode)
        }
        .ifEmpty { listOf(ExplorerTreeNode(NoItemsFoundNodeDescriptor(), parentNode.project, parentNode)) }
    }
  }

  override fun setNodesRefreshInfo(fetcherNodeDescriptor: FetcherNodeDescriptor) {
    fetcherNodeDescriptor.setUpdateInfo()
  }

  override fun run(): LoadDatasetMaskNodesOperationResult {
    val loadedNodes = NodeSyncService.getService().loadNodesForPlainFilter(this)
    return LoadDatasetMaskNodesOperationResult(loadedNodes)
  }
}
