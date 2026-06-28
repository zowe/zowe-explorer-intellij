/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.jes.operations

import org.zowe.explorer.v3.impl.api.ZosmfApiService
import org.zowe.explorer.v3.impl.connection.ConnectionProfileRelated
import org.zowe.explorer.v3.impl.connection.ZoweConnectionService
import org.zowe.explorer.v3.impl.jes.tree.nodes.JobNodeDescriptor
import org.zowe.explorer.v3.newoperations.LoadNodesOperation
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig
import org.zowe.explorer.v3.tree.nodes.ErrorNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.FetcherNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.NoItemsFoundNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.NodeSyncService
import org.zowe.kotlinsdk.core.StatusType
import org.zowe.kotlinsdk.providers.zowe.zosmf.jes.messaging.ZosmfListJobsRequest

// TODO: doc
class LoadJobFilterNodesOperation(
  override val operationData: LoadJobFilterNodesOperationData
) : LoadNodesOperation {
  override suspend fun fetchChildren(): List<ExplorerTreeNode> {
    val parentNode = operationData.node
    val parentNodeData = parentNode.nodeDescriptor as ConnectionProfileRelated
    val connectionConfig = ConfigCacheService.getService()
      .getConfigFromCache(ConfigType.HTTP_CONNECTION_CONFIG_V1, parentNodeData.connectionProfile)
      ?: throw Exception("Connection config is not found for node $this")
    val zoweConnectionManager = ZoweConnectionService.getService()
      .getZoweConnectionManager(parentNode.project)
    val httpConnection = zoweConnectionManager.produceHttpConnection("zosmf", shouldOverrideWithEnv = true)
    connectionConfig as HttpConnectionConfig
    val listJobsRequest = ZosmfListJobsRequest(
      httpConnection,
      jobPrefix = operationData.filter,
      jobOwner = operationData.owner,
      jobId = operationData.jobId
    )
    val listJobsResponse = ZosmfApiService.getService()
      .jes
      .listJobs(listJobsRequest)
    return if (listJobsResponse.status.type != StatusType.SUCCESS) {
      listOf(
        ExplorerTreeNode(
          ErrorNodeDescriptor("Host returned ${listJobsResponse.status.text}"),
          operationData.node.project,
          operationData.node
        )
      )
    } else {
      listJobsResponse
        .jobs
        .map { jobEntity ->
          val jobNodeDescriptor = NodeSyncService.getService()
            .getOrPutRealNodeDescriptor(operationData.path, "${jobEntity.name}(${jobEntity.id})") {
              JobNodeDescriptor(
                "${jobEntity.name}(${jobEntity.id})",
                operationData.path,
                connectionProfile = parentNodeData.connectionProfile
              )
            }
          ExplorerTreeNode(jobNodeDescriptor, parentNode.project, parentNode)
        }
        .ifEmpty { listOf(ExplorerTreeNode(NoItemsFoundNodeDescriptor(), parentNode.project, parentNode)) }
    }
  }

  override fun setNodesRefreshInfo(fetcherNodeDescriptor: FetcherNodeDescriptor) {
    fetcherNodeDescriptor.setUpdateInfo()
  }

  override fun run(): LoadJobFilterNodesOperationResult {
    val loadedNodes = NodeSyncService.getService().loadNodesForPlainFilter(this)
    return LoadJobFilterNodesOperationResult(loadedNodes)
  }
}
