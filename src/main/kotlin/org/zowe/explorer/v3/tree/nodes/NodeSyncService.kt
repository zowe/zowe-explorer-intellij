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

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.v3.newoperations.LoadNodesOperation
import org.zowe.explorer.v3.newoperations.LoadNodesOperationData
import org.zowe.explorer.v3.newoperations.RefreshNodesOperation
import org.zowe.explorer.v3.newoperations.RefreshNodesOperationData
import org.zowe.explorer.v3.performWithProgressiveDelay
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import org.zowe.explorer.v3.tree.nodes.path.PathTree

/**
 * Nodes synchronization service.
 * Provides a single way of controlling all the plugin's explorer nodes to avoid collisions between their interactions
 * and representations
 */
@Service
class NodeSyncService {
  companion object {
    fun getService(): NodeSyncService = service()
  }

  /** Map to track filter nodes to load their children as a single node */
  private val filterNodesMap by lazy { mutableMapOf<List<String>, MutableMap<String, FetcherNodeDescriptor>>() }

  /** A single tree of node paths. Contains node descriptors for each of the loaded paths, as well as path states */
  private val pathTree by lazy { PathTree() }

  /**
   * Run the task if the node and all parent nodes are not busy with other tasks
   * @param operationData the operation data to get the node's path and the project the node in
   * @param coroutineScope the coroutine scope to run the task in
   * @param taskProducer the task producer that produces the task to run if the run is possible
   * @return empty string if the run is succeeded, string with the reason why the run did not happen otherwise
   */
  private fun runIfNoOtherJobs(
    operationData: LoadNodesOperationData,
    coroutineScope: CoroutineScope,
    taskProducer: () -> Task.Backgroundable
  ): String {
    if (pathTree.getOrInitPathState(operationData.path) == PathTree.PathState.BUSY) {
      NotificationsService.getService()
        .notifyWarning(
          operationData.node.project,
          "Path '${operationData.node.nodeDescriptor.displayName}' is busy",
          "Operation is not possible while the path has an active job in progress",
          ""
        )
      return "Path was busy"
    } else if(pathTree.isAnyParentBusy(operationData.path)) {
      NotificationsService.getService()
        .notifyWarning(
          operationData.node.project,
          "Parent node path is busy",
          "Operation is not possible while one of the path's parent nodes has an active job in progress",
          ""
        )
      return "Parent path was busy"
    } else {
      val task = taskProducer()
      coroutineScope.launch {
        ProgressManager.getInstance().run(task)
      }
      return ""
    }
  }

  /**
   * Start loading children, modifying already existing nodes in the parent node.
   * Will execute the loading operation, providing the way of cancel it, and waiting if there is already operations
   * on any child or parent node is in progress
   * @param operation the operation object to execute the operation, specific to the node
   * @return the immediate list of already existing children to display.
   *         If there were children already, modifies the view to display busy children.
   *         If there were no children, displays a loading node
   */
  private fun startChildrenLoading(operation: LoadNodesOperation): List<ExplorerTreeNode> {
    val operationData = operation.operationData as LoadNodesOperationData
    val parentNode = operationData.node
    val parentNodeData = parentNode.nodeDescriptor as FetcherNodeDescriptor
    val explorerComponent = ExplorerTreeComponentService.getService()
      .getFilesExplorerComponent(parentNode.project)

    val reason = runIfNoOtherJobs(operationData, explorerComponent.explorerScope) {
      val originalTitle = "Loading children for ${parentNodeData.fetchFilter}..."

      object : Task.Backgroundable(parentNode.project, originalTitle, true) {
        private var newChildren: List<ExplorerTreeNode>? = null

        override fun run(indicator: ProgressIndicator) {
          runBlocking {
            pathTree.setPathState(operationData.path, PathTree.PathState.BUSY)
            updateRelatedDescriptorsAndInvalidate(
              parentNodeData,
              operation::setNodesRefreshInfo
            )
            invalidatePath(parentNodeData)
            pathTree.updatePathWithChildren(operationData.path) {
              if (it is ExplorerTreeNodeDescriptor) {
                it.isBusy = true
              }
              if (it is FetcherNodeDescriptor) {
                invalidatePath(it)
              }
            }

            title = "Waiting until all children operations are complete..."
            performWithProgressiveDelay {
              pathTree.isAnyChildBusy(operationData.path)
            }
            title = originalTitle

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
          val newChildrenNodeDescriptors = (newChildren?.mapNotNull { it.nodeDescriptor as? Traversable } ?: listOf())
            .ifEmpty { listOf(NoItemsFoundNodeDescriptor()) }
          pathTree.updatePath(operationData.path, newChildrenNodeDescriptors)
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
          invalidatePath(parentNodeData)
          pathTree.updatePathWithChildren(operationData.path) {
            if (it is ExplorerTreeNodeDescriptor) {
              it.isBusy = false
            }
            if (it is FetcherNodeDescriptor) {
              invalidatePath(it)
            }
          }
        }
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
      invalidatePath(parentNodeData)
    }
    return pathTree
      .getPathElements(operationData.path)
      .mapNotNull { it as? ExplorerTreeNodeDescriptor }
      .filter { it !is Ephemeral }
      .ifEmpty { listOf(LoadingNodeDescriptor()) }
      .map { ExplorerTreeNode(it, parentNode.project, parentNode) }
  }

  /**
   * Load nodes for the specified operation, depending on the state of the path to operate on.
   * If it is in INIT state -> start children fetch from a remote host.
   * If it is in BUSY state -> returns the existing children nodes, or "loading..." if there is no children nodes.
   * If it is in LOADED state -> returns the existing children nodes, or "No items found" if there is no children nodes
   * @param operation the operation object with operation data to get the parent node and the path to operate on
   * @return the list of explorer tree nodes in the respective state
   */
  fun loadNodes(operation: LoadNodesOperation): List<ExplorerTreeNode> {
    val loadNodesOperationData = operation.operationData as LoadNodesOperationData
    val parentNode = loadNodesOperationData.node
    val pathState = pathTree.getOrInitPathState(loadNodesOperationData.path)
    return when (pathState) {
      PathTree.PathState.INIT -> startChildrenLoading(operation)
      PathTree.PathState.BUSY -> {
        pathTree
          .getPathElements(loadNodesOperationData.path)
          .mapNotNull { it as? ExplorerTreeNodeDescriptor }
          .filter { it !is Ephemeral }
          .map { ExplorerTreeNode(it, parentNode.project, parentNode) }
          .ifEmpty {
            listOf(ExplorerTreeNode(LoadingNodeDescriptor(), parentNode.project, parentNode))
          }
      }
      PathTree.PathState.LOADED -> {
        pathTree
          .getPathElements(loadNodesOperationData.path)
          .mapNotNull { it as? ExplorerTreeNodeDescriptor }
          .map { ExplorerTreeNode(it, parentNode.project, parentNode) }
      }
    }
  }

  /**
   * Refresh children nodes of by the provided operation.
   * Will not start a refresh operation if either any parent or any child node is already being operated on
   * @param operation the operation to get info about the nodes to refresh from
   */
  fun refreshNodes(operation: RefreshNodesOperation) {
    val refreshNodesOperationData = operation.operationData as RefreshNodesOperationData
    val parentNode = refreshNodesOperationData.node

    if (pathTree.getOrInitPathState(refreshNodesOperationData.path) == PathTree.PathState.BUSY) {
      NotificationsService.getService()
        .notifyWarning(
          parentNode.project,
          "Path '${parentNode.nodeDescriptor.displayName}' is busy",
          "Refresh operation is not possible while the path has an active job in progress",
          ""
        )
    } else if (pathTree.isAnyParentBusy(refreshNodesOperationData.path)) {
      NotificationsService.getService()
        .notifyWarning(
          parentNode.project,
          "Parent node path is busy",
          "Refresh operation is not possible while one of the path's parent nodes has an active job in progress",
          ""
        )
    } else if (pathTree.isAnyChildBusy(refreshNodesOperationData.path)) {
      NotificationsService.getService()
        .notifyWarning(
          parentNode.project,
          "Child node path is busy",
          "Refresh operation is not possible while one of the path's child nodes has an active job in progress",
          ""
        )
    } else {
      pathTree.setPathState(refreshNodesOperationData.path, PathTree.PathState.INIT)
      loadNodes(operation.generateLoadNodesOperation())
    }
  }

  /**
   * Create a new node descriptor, enhance it with update info if it is a [FetcherNodeDescriptor]
   * @param descriptorCreatorFn the function to create a new node descriptor
   * @return the newly created node descriptor
   */
  private fun createDescriptorWithUpdateInfo(descriptorCreatorFn: () -> ExplorerTreeNodeDescriptor): ExplorerTreeNodeDescriptor {
    val createdDescriptor = descriptorCreatorFn()
    if (createdDescriptor is FetcherNodeDescriptor) {
      findRelatedDescriptors(createdDescriptor)
        .find { descriptor -> descriptor.currentUpdateInfo != null }
        ?.currentUpdateInfo
        ?.let { currentUpdateInfo ->
          createdDescriptor.setUpdateInfo(currentUpdateInfo)
        }
    }
    return createdDescriptor
  }

  /**
   * Get or put a new filter node descriptor
   * @param basePath the base path to get or put the node by
   * @param filterName the name of the filter node
   * @param descriptorCreatorFn the function to create a new filter node descriptor if it does not exist in the map
   * @return the filter node descriptor, found or created
   */
  fun getOrCreateFilterNodeDescriptor(
    basePath: List<String>,
    filterName: String,
    descriptorCreatorFn: () -> FetcherNodeDescriptor
  ): FetcherNodeDescriptor {
    return filterNodesMap.getOrPut(basePath) { mutableMapOf() }
      .getOrPut(filterName) { createDescriptorWithUpdateInfo(descriptorCreatorFn) as FetcherNodeDescriptor }
  }

  /**
   * Get or put a new real node descriptor
   * @param descriptorPlacingPath the path where the descriptor is meant to be placed
   * @param descriptorName the descriptor name to find in the [pathTree]
   * @param descriptorCreatorFn the function to create a new real node descriptor if it does not exist in the map
   * @return the real node descriptor, found or created
   */
  fun getOrCreateRealNodeDescriptor(
    descriptorPlacingPath: List<String>,
    descriptorName: String,
    descriptorCreatorFn: () -> ExplorerTreeNodeDescriptor
  ): ExplorerTreeNodeDescriptor {
    return pathTree.getPathElement(descriptorPlacingPath, descriptorName)
      as? ExplorerTreeNodeDescriptor
      ?: createDescriptorWithUpdateInfo(descriptorCreatorFn)
  }

  /**
   * Find real and filter node descriptors to that are related by invalidation info
   * @param fetcherNodeDescriptor the original node descriptor to find the other descriptor by
   * @return a list with the descriptors if they are found
   */
  private fun findRelatedDescriptors(fetcherNodeDescriptor: FetcherNodeDescriptor): List<FetcherNodeDescriptor> {
    val realDescriptorToInvalidate = pathTree.getPathElement(
      fetcherNodeDescriptor.invalidationPath,
      fetcherNodeDescriptor.invalidationElem
    ) as? FetcherNodeDescriptor
    val filterDescriptorToInvalidate = filterNodesMap.getOrDefault(fetcherNodeDescriptor.basePath, mapOf())
      .values
      .find { it.fetchFilter == fetcherNodeDescriptor.fetchFilter }
    return listOfNotNull(realDescriptorToInvalidate, filterDescriptorToInvalidate)
  }

  /**
   * Invalidate the path by the provided node descriptor to invalidate originally
   * @param fetcherNodeDescriptor the original node descriptor to invalidate and find the related nodes by
   */
  fun invalidatePath(fetcherNodeDescriptor: FetcherNodeDescriptor) {
    findRelatedDescriptors(fetcherNodeDescriptor)
      .forEach { it.invalidateAssociatedNodes() }
  }

  /**
   * Update the nodes, related to the descriptor, real and filter
   * @param fetcherNodeDescriptor the node descriptor to find related nodes by
   * @param operationToPerformFn the operation function to perform on the nodes
   */
  fun updateRelatedDescriptorsAndInvalidate(
    fetcherNodeDescriptor: FetcherNodeDescriptor,
    operationToPerformFn: (FetcherNodeDescriptor) -> Unit
  ) {
    findRelatedDescriptors(fetcherNodeDescriptor)
      .forEach {
        operationToPerformFn(it)
        it.invalidateAssociatedNodes()
      }
  }
}
