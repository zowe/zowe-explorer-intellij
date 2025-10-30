/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.tree.nodes

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.zowe.explorer.v3.components.files.DatasetMaskNodeDescriptor
import org.zowe.explorer.v3.newoperations.LoadNodesOperation
import org.zowe.explorer.v3.newoperations.RefreshNodesOperation
import org.zowe.explorer.v3.performWithProgressiveDelay
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import org.zowe.explorer.v3.tree.nodes.path.PathTree
import kotlin.collections.ifEmpty
import kotlin.collections.map

// TODO: doc
class PlainFilterNodesLoader(
  pathTree: PathTree,
  filterNodeDescriptors: MutableMap<List<String>, MutableMap<String, FetcherNodeDescriptor>>
) : NodesLoader(pathTree, filterNodeDescriptors) {
  // TODO: doc
  private fun findDirectChildrenNodeDescriptors(
    plainFilterNodeDescriptor: PlainFilterNodeDescriptor
  ): List<ExplorerTreeNodeDescriptor> {
    return pathTree.getPathElements(plainFilterNodeDescriptor.basePath)
      .filter { plainFilterNodeDescriptor.checkMatchesFilter(it.elemName) }
      .mapNotNull { it as? ExplorerTreeNodeDescriptor }
  }

  /**
   * Find related descriptors for the provided [FetcherNodeDescriptor].
   * It will find the related [PlainFilterNodeDescriptor]'s only
   * because there is no reason to operate on the real node descriptors in this loader
   * @param fetcherNodeDescriptor the original node descriptor to find the related descriptors by
   * @return a list of found related [FetcherNodeDescriptor]'s or null
   */
  override fun findRelatedDescriptors(fetcherNodeDescriptor: FetcherNodeDescriptor): List<FetcherNodeDescriptor> {
    return listOfNotNull(
      when (fetcherNodeDescriptor) {
        is PlainFilterNodeDescriptor -> {
          findFilterNodeDescriptor(fetcherNodeDescriptor.basePath, fetcherNodeDescriptor.fetchFilter)
        }
        else -> null
      }
    )
  }

  override fun startChildrenLoading(operation: LoadNodesOperation): List<ExplorerTreeNode> {
    val operationData = operation.operationData
    val parentNode = operationData.node
    val parentNodeData = parentNode.nodeDescriptor as PlainFilterNodeDescriptor

    val originalTaskTitle = "Loading children for ${parentNodeData.fetchFilter}..."
    val fetchChildrenTask = object : Task.Backgroundable(parentNode.project, originalTaskTitle, true) {
      private var newChildren: List<ExplorerTreeNode>? = null

      override fun run(indicator: ProgressIndicator) {
        runBlocking {
          parentNodeData.filterState = PlainFilterNodeDescriptor.FilterState.BUSY
          // TODO: update children nodes as well
          val assocRealNodeDescriptors = findDirectChildrenNodeDescriptors(parentNodeData)
          findRelatedDescriptors(parentNodeData)
            .forEach { foundDescriptor ->
              operation.setNodesRefreshInfo(foundDescriptor)
            }
          pathTree.updatePathWithChildren(operationData.path) {
            if (it is ExplorerTreeNodeDescriptor && assocRealNodeDescriptors.contains(it)) {
              it.isBusy = true
            }
          }
          filterNodeDescriptors.getOrDefault(operationData.path, mapOf())
            .values
            .forEach { it.invalidateAssociatedNodes() }

          val fetchJob = async(Dispatchers.IO) {
            operation.fetchChildren()
          }

          performWithProgressiveDelay {
            if (fetchJob.isActive) {
              if (indicator.isCanceled || !isActive) {
                fetchJob.cancel()
                false
              } else {
                true
              }
            } else {
              false
            }
          }
          if (fetchJob.isCancelled) {
            return@runBlocking
          }

          newChildren = fetchJob.await()
        }
      }

      override fun onSuccess() {
        parentNodeData.filterState = PlainFilterNodeDescriptor.FilterState.LOADED
        val newChildrenNodeDescriptors = (newChildren?.mapNotNull { it.nodeDescriptor as? Traversable } ?: listOf())
          .ifEmpty { listOf(NoItemsFoundNodeDescriptor()) }
        pathTree.updatePath(operationData.path, newChildrenNodeDescriptors)
      }

      override fun onCancel() {
        parentNodeData.filterState = PlainFilterNodeDescriptor.FilterState.ERROR
        parentNodeData.filterError = "Fetch is cancelled by user"
      }

      override fun onThrowable(error: Throwable) {
        parentNodeData.filterState = PlainFilterNodeDescriptor.FilterState.ERROR
        parentNodeData.filterError = "Error: ${error.message ?: "Unknown error"}"
      }

      override fun onFinished() {
        parentNodeData.wasLoadedBefore = true
        // TODO: update children nodes as well
        val assocRealNodeDescriptors = findDirectChildrenNodeDescriptors(parentNodeData)
        pathTree.updatePathWithChildren(operationData.path) {
          if (it is ExplorerTreeNodeDescriptor && assocRealNodeDescriptors.contains(it)) {
            it.isBusy = false
          }
        }
        filterNodeDescriptors.getOrDefault(operationData.path, mapOf())
          .values
          .forEach { it.invalidateAssociatedNodes() }
      }
    }

    val explorerComponent = ExplorerTreeComponentService.getService()
      .getFilesExplorerComponent(parentNode.project)
    val reason = NodeSyncService.getService()
      .runIfPathIsReady(
        parentNode.project,
        operationData.path,
        parentNodeData.displayName
      ) {
        explorerComponent.explorerScope.launch {
          ProgressManager.getInstance().run(fetchChildrenTask)
        }
      }

    if (reason.isNotEmpty()) {
      parentNodeData.filterState = PlainFilterNodeDescriptor.FilterState.ERROR
      parentNodeData.filterError = reason
      filterNodeDescriptors.getOrDefault(operationData.path, mapOf())
        .values
        .forEach { it.invalidateAssociatedNodes() }
    }
    return if (parentNodeData.filterState == PlainFilterNodeDescriptor.FilterState.ERROR) {
      listOf(
        ExplorerTreeNode(
          ErrorNodeDescriptor(parentNodeData.filterError),
          parentNode.project,
          parentNode
        )
      )
    } else {
      pathTree
        .getPathElements(operationData.path)
        .mapNotNull { it as? ExplorerTreeNodeDescriptor }
        .filter { it !is Ephemeral }
        .ifEmpty { listOf(LoadingNodeDescriptor()) }
        .map { ExplorerTreeNode(it, parentNode.project, parentNode) }
    }
  }

  override fun getBusyChildren(operation: LoadNodesOperation): List<ExplorerTreeNode> {
    val loadNodesOperationData = operation.operationData
    val datasetFilterNode = loadNodesOperationData.node
    val datasetFilterNodeDescriptor = datasetFilterNode.nodeDescriptor as DatasetMaskNodeDescriptor
    return if (datasetFilterNodeDescriptor.wasLoadedBefore) {
      findDirectChildrenNodeDescriptors(datasetFilterNodeDescriptor)
        .map { ExplorerTreeNode(it, datasetFilterNode.project, datasetFilterNode) }
        .ifEmpty {
          listOf(
            ExplorerTreeNode(
              LoadingNodeDescriptor(),
              datasetFilterNode.project,
              datasetFilterNode
            )
          )
        }
    } else {
      listOf(
        ExplorerTreeNode(
          LoadingNodeDescriptor(),
          loadNodesOperationData.node.project,
          loadNodesOperationData.node
        )
      )
    }
  }

  override fun getLoadedChildren(operation: LoadNodesOperation): List<ExplorerTreeNode> {
    val loadNodesOperationData = operation.operationData
    val datasetFilterNode = loadNodesOperationData.node
    val datasetFilterNodeDescriptor = datasetFilterNode.nodeDescriptor as DatasetMaskNodeDescriptor
    return findDirectChildrenNodeDescriptors(datasetFilterNodeDescriptor)
      .map { ExplorerTreeNode(it, datasetFilterNode.project, datasetFilterNode) }
      .ifEmpty {
        listOf(
          ExplorerTreeNode(
            NoItemsFoundNodeDescriptor(),
            datasetFilterNode.project,
            datasetFilterNode
          )
        )
      }
  }

  override fun loadNodes(operation: LoadNodesOperation): List<ExplorerTreeNode> {
    val loadNodesOperationData = operation.operationData
    val parentNode = loadNodesOperationData.node
    val datasetFilterNodeDescriptor = parentNode.nodeDescriptor as DatasetMaskNodeDescriptor
    return when (datasetFilterNodeDescriptor.filterState) {
      PlainFilterNodeDescriptor.FilterState.INIT -> startChildrenLoading(operation)
      PlainFilterNodeDescriptor.FilterState.BUSY -> getBusyChildren(operation)
      PlainFilterNodeDescriptor.FilterState.LOADED -> getLoadedChildren(operation)
      PlainFilterNodeDescriptor.FilterState.ERROR -> listOf(
        ExplorerTreeNode(
          ErrorNodeDescriptor(datasetFilterNodeDescriptor.filterError),
          parentNode.project,
          parentNode
        )
      )
    }
  }

  override fun refreshNodes(operation: RefreshNodesOperation) {
    val refreshNodesOperationData = operation.operationData
    val parentNode = refreshNodesOperationData.node

    NodeSyncService.getService()
      .runIfPathIsReady(
        parentNode.project,
        refreshNodesOperationData.path,
        parentNode.nodeDescriptor.displayName,
      ) {
        pathTree.setPathState(refreshNodesOperationData.path, PathTree.PathState.INIT)
        val datasetMaskNodeDescriptor = findFilterNodeDescriptor(
          refreshNodesOperationData.path,
          refreshNodesOperationData.filter
        ) as? DatasetMaskNodeDescriptor
        datasetMaskNodeDescriptor?.filterState = PlainFilterNodeDescriptor.FilterState.INIT
        loadNodes(
          (refreshNodesOperationData.node.nodeDescriptor as FetcherNodeDescriptor)
            .generateLoadNodesOperation(refreshNodesOperationData.node)
        )
      }
  }
}