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
import org.zowe.explorer.v3.newoperations.LoadNodesOperation
import org.zowe.explorer.v3.newoperations.RefreshNodesOperation
import org.zowe.explorer.v3.performWithProgressiveDelay
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import org.zowe.explorer.v3.tree.nodes.path.PathTree
import kotlin.collections.ifEmpty
import kotlin.collections.map

/** Plain filter nodes loader. Provides functionality to manage nodes loading for [PlainFilterNodeDescriptor]'s */
class PlainFilterNodesLoader(
  pathTree: PathTree,
  filterNodeDescriptors: MutableMap<List<String>, MutableMap<String, FetcherNodeDescriptor>>
) : NodesLoader(pathTree, filterNodeDescriptors) {
  /**
   * Find direct children node descriptors of the provided [PlainFilterNodeDescriptor].
   * Will match node descriptors against the provided filter and return the [ExplorerTreeNodeDescriptor]'s list
   */
  private fun findDirectChildrenNodeDescriptors(
    plainFilterNodeDescriptor: PlainFilterNodeDescriptor
  ): List<ExplorerTreeNodeDescriptor> {
    return pathTree.getPathElements(plainFilterNodeDescriptor.basePath)
      .filter { plainFilterNodeDescriptor.checkMatchesFilter(it.elemName) }
      .filterIsInstance<ExplorerTreeNodeDescriptor>()
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

  // TODO: doc
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
          findRelatedDescriptors(parentNodeData)
            .forEach { foundDescriptor ->
              operation.setNodesRefreshInfo(foundDescriptor)
            }
          val directChildrenDescriptors = findDirectChildrenNodeDescriptors(parentNodeData)
          // Update direct + next level children
          pathTree.applyToPathElements(
            operationData.path,
            false
          ) { firstLvlChild ->
            if (firstLvlChild is ExplorerTreeNodeDescriptor && directChildrenDescriptors.contains(firstLvlChild)) {
              firstLvlChild.isBusy = true
              val secondLvlChildrenPath = operationData.path + firstLvlChild.elemName
              if (firstLvlChild is FetcherNodeDescriptor) {
                pathTree.getOrInitPathState(secondLvlChildrenPath)
                pathTree.applyToPathElements(
                  secondLvlChildrenPath,
                  false
                ) { secondLvlChild ->
                  if (secondLvlChild is ExplorerTreeNodeDescriptor) {
                    secondLvlChild.isBusy = true
                  }
                }
              }
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
        val directChildrenDescriptors = findDirectChildrenNodeDescriptors(parentNodeData)
        // Update direct + next level children
        pathTree.applyToPathElements(
          operationData.path,
          false
        ) { firstLvlChild ->
          if (firstLvlChild is ExplorerTreeNodeDescriptor && directChildrenDescriptors.contains(firstLvlChild)) {
            firstLvlChild.isBusy = false
            val secondLvlChildrenPath = operationData.path + firstLvlChild.elemName
            if (firstLvlChild is FetcherNodeDescriptor) {
              pathTree.getOrInitPathState(secondLvlChildrenPath)
              pathTree.applyToPathElements(
                firstLvlChild.basePath + firstLvlChild.elemName,
                false
              ) { secondLvlChild ->
                if (secondLvlChild is ExplorerTreeNodeDescriptor) {
                  secondLvlChild.isBusy = false
                }
              }
            }
          }
        }
        filterNodeDescriptors.getOrDefault(operationData.path, mapOf())
          .values
          .forEach { it.invalidateAssociatedNodes() }
      }
    }

    val explorerComponent = ExplorerTreeComponentService.getService()
      .getExplorerComponentForNode(parentNode)
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
        .filterIsInstance<ExplorerTreeNodeDescriptor>()
        .filter { it !is Ephemeral }
        .ifEmpty { listOf(LoadingNodeDescriptor()) }
        .map { ExplorerTreeNode(it, parentNode.project, parentNode) }
    }
  }

  // TODO: doc
  override fun getBusyChildren(operation: LoadNodesOperation): List<ExplorerTreeNode> {
    val loadNodesOperationData = operation.operationData
    val filterNode = loadNodesOperationData.node
    val filterNodeDescriptor = filterNode.nodeDescriptor as PlainFilterNodeDescriptor
    return if (filterNodeDescriptor.wasLoadedBefore) {
      findDirectChildrenNodeDescriptors(filterNodeDescriptor)
        .map { ExplorerTreeNode(it, filterNode.project, filterNode) }
        .ifEmpty {
          listOf(
            ExplorerTreeNode(
              LoadingNodeDescriptor(),
              filterNode.project,
              filterNode
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

  /**
   * Get direct loaded children nodes for the respective operation
   * @param operation the load nodes operation to return loaded nodes for
   * @return the loaded nodes list or list with "No items found" node only if there is no loaded nodes for the operation
   */
  override fun getLoadedChildren(operation: LoadNodesOperation): List<ExplorerTreeNode> {
    val loadNodesOperationData = operation.operationData
    val filterNode = loadNodesOperationData.node
    val filterNodeDescriptor = filterNode.nodeDescriptor as PlainFilterNodeDescriptor
    return findDirectChildrenNodeDescriptors(filterNodeDescriptor)
      .map { ExplorerTreeNode(it, filterNode.project, filterNode) }
      .ifEmpty {
        listOf(
          ExplorerTreeNode(
            NoItemsFoundNodeDescriptor(),
            filterNode.project,
            filterNode
          )
        )
      }
  }

  /**
   * Load nodes operation loop to trigger the next operation step basing on the current operation state.
   * The current operation triggers the next step with the next rules:
   * - INIT -> start children loading, return list with the 'loading...' item
   * - BUSY -> just return the current list of items, blocked by the operation, or the list with 'loading...' item only
   * - LOADED -> just return the list of loaded items or the list with 'No items found' item only
   * - ERROR -> just return the list with the 'Error' item only
   * @param operation the load nodes operation to return the respective list of nodes for
   * @return one of the lists basing on the state of the node descriptor associated with the operation:
   * - INIT -> the list of 'loading...' item only
   * - BUSY -> the current list of items, blocked by the operation, or the list with 'loading...' item only
   * - LOADED -> the list of loaded items or the list with 'No items found' item only
   * - ERROR -> the list with the 'Error' item only
   */
  override fun loadNodes(operation: LoadNodesOperation): List<ExplorerTreeNode> {
    val loadNodesOperationData = operation.operationData
    val parentNode = loadNodesOperationData.node
    val filterNodeDescriptor = parentNode.nodeDescriptor as PlainFilterNodeDescriptor
    return when (filterNodeDescriptor.filterState) {
      PlainFilterNodeDescriptor.FilterState.INIT -> startChildrenLoading(operation)
      PlainFilterNodeDescriptor.FilterState.BUSY -> getBusyChildren(operation)
      PlainFilterNodeDescriptor.FilterState.LOADED -> getLoadedChildren(operation)
      PlainFilterNodeDescriptor.FilterState.ERROR -> listOf(
        ExplorerTreeNode(
          ErrorNodeDescriptor(filterNodeDescriptor.filterError),
          parentNode.project,
          parentNode
        )
      )
    }
  }

  // TODO: doc
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
        val filterNodeDescriptor = findFilterNodeDescriptor(
          refreshNodesOperationData.path,
          refreshNodesOperationData.filter
        ) as? PlainFilterNodeDescriptor
        filterNodeDescriptor?.filterState = PlainFilterNodeDescriptor.FilterState.INIT
        loadNodes(
          (refreshNodesOperationData.node.nodeDescriptor as FetcherNodeDescriptor)
            .generateLoadNodesOperation(refreshNodesOperationData.node)
        )
      }
  }
}