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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.zowe.explorer.v3.newoperations.LoadNodesOperation
import org.zowe.explorer.v3.newoperations.RefreshNodesOperation
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import org.zowe.explorer.v3.tree.nodes.path.PathTree

/** Fetcher nodes loader. Provides functionality to manage nodes loading for [FetcherNodeDescriptor]'s */
class FetcherNodesLoader(
  pathTree: PathTree,
  filterNodeDescriptors: MutableMap<List<String>, MutableMap<String, FetcherNodeDescriptor>>
) : NodesLoader(pathTree, filterNodeDescriptors) {
  /**
   * Find real and filter node descriptors that are related by placing paths and element names
   * @param fetcherNodeDescriptor the original node descriptor to find the other descriptor by
   * @return a list with the descriptors if they are found
   */
  override fun findRelatedDescriptors(fetcherNodeDescriptor: FetcherNodeDescriptor): List<FetcherNodeDescriptor> {
    val realDescriptor = if (fetcherNodeDescriptor is RealNodeAssociation) {
      findRealNodeDescriptor(fetcherNodeDescriptor.placingPath, fetcherNodeDescriptor.elemName)
    } else null
    val filterDescriptor = findFilterNodeDescriptor(
      fetcherNodeDescriptor.basePath,
      fetcherNodeDescriptor.fetchFilter
    )
    return listOfNotNull(realDescriptor, filterDescriptor)
  }

  /**
   * Invalidate the path by the provided node descriptor to invalidate originally
   * @param fetcherNodeDescriptor the original node descriptor to invalidate and find the related nodes by
   */
  private fun invalidatePathByNodeDescriptor(fetcherNodeDescriptor: FetcherNodeDescriptor) {
    findRelatedDescriptors(fetcherNodeDescriptor)
      .forEach { it.invalidateAssociatedNodes() }
  }

  /**
   * Starts the children loading process.
   * It includes creation of a background task with a progress indicator and a possibility to cancel the task.
   * During the load process start, updates respective nodes with related info
   * (like "loading" animated icon, refresh info, etc.)
   * @param operation the [LoadNodesOperation] to start the loading process by
   * @return a list of [ExplorerTreeNode]'s formed from related descriptors
   * @see [NodesLoader.startChildrenLoading]
   */
  override fun startChildrenLoading(operation: LoadNodesOperation): List<ExplorerTreeNode> {
    val operationData = operation.operationData
    val parentNode = operationData.node
    val parentNodeData = parentNode.nodeDescriptor as FetcherNodeDescriptor

    val originalTaskTitle = "Loading children for ${parentNodeData.fetchFilter}..."
    val fetchChildrenTask = object : Task.Backgroundable(
      parentNode.project, originalTaskTitle, true
    ) {
      private var newChildren: List<ExplorerTreeNode>? = null

      override fun run(indicator: ProgressIndicator) {
        runBlocking {
          pathTree.setPathState(operationData.path, PathTree.PathState.BUSY)
          findRelatedDescriptors(parentNodeData)
            .forEach { foundDescriptor ->
              operation.setNodesRefreshInfo(foundDescriptor)
              foundDescriptor.invalidateAssociatedNodes()
            }
          invalidatePathByNodeDescriptor(parentNodeData)
          pathTree.applyToPathElements(operationData.path) {
            if (it is ExplorerTreeNodeDescriptor) {
              it.isBusy = true
            }
            if (it is FetcherNodeDescriptor) {
              invalidatePathByNodeDescriptor(it)
            }
          }

          newChildren = fetchChildrenWithCancellation(operation, indicator) ?: return@runBlocking
        }
      }

      override fun onSuccess() {
        val newChildrenNodeDescriptors = (newChildren?.mapNotNull { it.nodeDescriptor as? Traversable } ?: listOf())
          .ifEmpty { listOf(NoItemsFoundNodeDescriptor()) }
        pathTree.rewritePath(operationData.path, newChildrenNodeDescriptors)
      }

      override fun onCancel() {
        pathTree
          .resetPath(
            operationData.path,
            listOf(ErrorNodeDescriptor("Loading canceled"))
          )
      }

      override fun onThrowable(error: Throwable) {
        pathTree
          .resetPath(
            operationData.path,
            listOf(ErrorNodeDescriptor("Error: ${error.message ?: "Unknown error"}"))
          )
      }

      override fun onFinished() {
        pathTree.setPathState(operationData.path, PathTree.PathState.LOADED)
        invalidatePathByNodeDescriptor(parentNodeData)
        pathTree.applyToPathElements(operationData.path) {
          if (it is ExplorerTreeNodeDescriptor) {
            it.isBusy = false
          }
          if (it is FetcherNodeDescriptor) {
            invalidatePathByNodeDescriptor(it)
          }
        }
      }
    }

    val explorerComponent = ExplorerTreeComponentService.getService()
      .getExplorerComponentForNode(parentNode)
    val reason = NodeSyncService.getService()
      .runIfPathIsReady(parentNode.project, operationData.path, parentNodeData.displayName) {
        explorerComponent.explorerScope.launch {
          ProgressManager.getInstance().run(fetchChildrenTask)
        }
      }

    if (reason.isNotEmpty()) {
      pathTree.setPathState(operationData.path, PathTree.PathState.LOADED)
      pathTree.getPathElements(operationData.path)
        .ifEmpty {
          pathTree
            .resetPath(
              operationData.path,
              listOf(ErrorNodeDescriptor(reason))
            )
        }
      invalidatePathByNodeDescriptor(parentNodeData)
    }
    return pathTree
      .getPathElements(operationData.path)
      .filterIsInstance<ExplorerTreeNodeDescriptor>()
      .filter { it !is Ephemeral }
      .ifEmpty { listOf(LoadingNodeDescriptor()) }
      .map { ExplorerTreeNode(it, parentNode.project, parentNode) }
  }

  override fun getBusyChildren(operation: LoadNodesOperation): List<ExplorerTreeNode> {
    val loadNodesOperationData = operation.operationData
    val parentNode = loadNodesOperationData.node
    return pathTree
      .getPathElements(loadNodesOperationData.path)
      .filterIsInstance<ExplorerTreeNodeDescriptor>()
      .filter { it !is Ephemeral }
      .map { ExplorerTreeNode(it, parentNode.project, parentNode) }
      .ifEmpty {
        listOf(ExplorerTreeNode(LoadingNodeDescriptor(), parentNode.project, parentNode))
      }
  }

  override fun getLoadedChildren(operation: LoadNodesOperation): List<ExplorerTreeNode> {
    val loadNodesOperationData = operation.operationData
    val parentNode = loadNodesOperationData.node
    return pathTree
      .getPathElements(loadNodesOperationData.path)
      .filterIsInstance<ExplorerTreeNodeDescriptor>()
      .map { ExplorerTreeNode(it, parentNode.project, parentNode) }
  }

  override fun loadNodes(operation: LoadNodesOperation): List<ExplorerTreeNode> {
    val pathState = pathTree.getOrInitPathState(operation.operationData.path)
    return when (pathState) {
      PathTree.PathState.INIT -> startChildrenLoading(operation)
      PathTree.PathState.BUSY -> getBusyChildren(operation)
      PathTree.PathState.LOADED -> getLoadedChildren(operation)
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
        loadNodes(
          (parentNode.nodeDescriptor as FetcherNodeDescriptor)
            .generateLoadNodesOperation(refreshNodesOperationData.node)
        )
      }
  }
}