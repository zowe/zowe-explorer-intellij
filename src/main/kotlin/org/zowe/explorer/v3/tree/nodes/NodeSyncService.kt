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
import com.intellij.openapi.project.Project
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.v3.newoperations.LoadNodesOperation
import org.zowe.explorer.v3.newoperations.RefreshNodesOperation
import org.zowe.explorer.v3.tree.nodes.path.PathTree

/** Nodes synchronization service. Provides an implementation of the synchronization-related functions */
@Service
class NodeSyncService {
  companion object {
    fun getService(): NodeSyncService = service()
  }

  /** A single tree of node paths. Contains node descriptors for each of the loaded paths, as well as path states */
  private val pathTree by lazy { PathTree() }

  /**
   * Registry of [FetcherNodeDescriptor]s, indexed by base path and filter name.
   * The outer key is the base path (e.g. `["host", "files", "ds"]`),
   * the inner key is the filter/mask name (e.g. `"SYS1.**"`).
   * Used to look up related filter descriptors when checking busy state
   * and to prefill update info on newly created descriptors
   */
  private val filterNodeDescriptors by lazy { mutableMapOf<List<String>, MutableMap<String, FetcherNodeDescriptor>>() }

  /**
   * Loader for plain filter nodes (e.g. dataset masks, USS path filters).
   * Handles nodes that represent a filter/mask applied to a flat list of elements.
   * Uses [PathTree.updatePath] to merge loaded children while preserving existing references
   */
  private val plainFilterNodesLoader by lazy { PlainFilterNodesLoader(pathTree, filterNodeDescriptors) }

  /**
   * Loader for fetcher nodes (e.g. partitioned datasets, USS folders).
   * Handles nodes that fetch their own children from the server (members, nested files).
   * Uses [PathTree.rewritePath] to fully synchronize children with the server response
   */
  private val fetcherFilterNodesLoader by lazy { FetcherNodesLoader(pathTree, filterNodeDescriptors) }

  /**
   * Find descriptors, related to the provided [FetcherNodeDescriptor]
   * @param fetcherNodeDescriptor the node descriptor to find related descriptors by
   */
  private fun findRelatedDescriptors(fetcherNodeDescriptor: FetcherNodeDescriptor): List<FetcherNodeDescriptor> {
    return plainFilterNodesLoader.findRelatedDescriptors(fetcherNodeDescriptor) +
      fetcherFilterNodesLoader.findRelatedDescriptors(fetcherNodeDescriptor)
  }

  /**
   * Create a new node descriptor, enhanced with a specific to the fetcher node info,
   * if it is a [FetcherNodeDescriptor].
   * Just creates the node and returns it if it is a basic [ExplorerTreeNodeDescriptor]
   * @param originalCreatorFn the original function to create a new node descriptor
   * @return the newly created node descriptor
   */
  private fun createDescriptorWithPrefill(
    originalCreatorFn: () -> ExplorerTreeNodeDescriptor
  ): ExplorerTreeNodeDescriptor {
    val createdDescriptor = originalCreatorFn()
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
   * Check if there is a [PlainFilterNodeDescriptor], associated with the node, busy
   * @param path the path to get a base path from to find the [PlainFilterNodeDescriptor]
   * @param nodeName the node name to check if the filter is busy
   * @return true if there is a busy filter, associated with the node
   */
  private fun checkPlainFilterBusy(path: List<String>, nodeName: String): Boolean {
    val basePath = path.take(3)
    return filterNodeDescriptors.getOrDefault(basePath, mutableMapOf())
      .values
      .mapNotNull { it as? PlainFilterNodeDescriptor }
      .any {
        it.checkMatchesFilter(nodeName) && it.filterState == PlainFilterNodeDescriptor.FilterState.BUSY
      }
  }

  /**
   * Run the provided function if the path is ready
   * (meaning all the children and parent nodes are not busy as well as the node to operate on)
   * @param project the [Project] to produce a warning notification in when it should be produced
   * @param path the path to check
   * @param nodeName the node name to produce a meaningful reason in the error details
   * @param checkParentNodes if "true", will check that parent nodes are ready as well (default is "true")
   * @param checkChildrenNodes if "true", will check that children nodes are ready as well (default is "true")
   * @param successFn the function to execute when the path is ready
   * @return empty string if the run is succeeded, string with the reason why the run did not happen otherwise
   */
  fun runIfPathIsReady(
    project: Project,
    path: List<String>,
    nodeName: String,
    checkParentNodes: Boolean = true,
    checkChildrenNodes: Boolean = true,
    successFn: () -> Unit
  ): String {
    return if (pathTree.getOrInitPathState(path) == PathTree.PathState.BUSY) {
      val pathIsBusy = "Path '$path' for node '$nodeName' is busy"
      NotificationsService.getService()
        .notifyWarning(
          project,
          pathIsBusy,
          "Operation is not possible while the path has an active job in progress",
          ""
        )
      pathIsBusy
    } else if (
      checkParentNodes
      && (pathTree.isAnyParentBusy(path) || checkPlainFilterBusy(path, nodeName))
    ) {
      val parentPathIsBusy = "Parent node path is busy for node '$nodeName' in path '$path'"
        NotificationsService.getService()
          .notifyWarning(
            project,
            parentPathIsBusy,
            "Operation is not possible while one of the path's parent nodes has an active job in progress",
            ""
          )
      parentPathIsBusy
    } else if (checkChildrenNodes && pathTree.isAnyChildBusy(path)) {
      val childPathIsBusy = "Child node path is busy for node '$nodeName' in path '$path'"
      NotificationsService.getService()
        .notifyWarning(
          project,
          childPathIsBusy,
          "Operation is not possible while one of the path's child nodes has an active job in progress",
          ""
        )
      childPathIsBusy
    } else {
      successFn()
      ""
    }
  }

  /**
   * Get or put a new real node descriptor
   * @param descriptorPlacingPath the path where the descriptor is meant to be placed
   * @param descriptorName the descriptor name to find in the [pathTree]
   * @param descriptorCreatorFn the function to create a new real node descriptor if it does not exist in the map
   * @return the real node descriptor, found or created
   */
  fun getOrPutRealNodeDescriptor(
    descriptorPlacingPath: List<String>,
    descriptorName: String,
    descriptorCreatorFn: () -> ExplorerTreeNodeDescriptor
  ): ExplorerTreeNodeDescriptor {
    return pathTree.getPathElement(descriptorPlacingPath, descriptorName)
      as? ExplorerTreeNodeDescriptor
      ?: createDescriptorWithPrefill(descriptorCreatorFn)
  }

  /**
   * Get or put a new filter node descriptor
   * @param basePath the base path to get or put the node by
   * @param filterName the name of the filter node
   * @param descriptorCreatorFn the function to create a new filter node descriptor if it does not exist in the map
   * @return the filter node descriptor, found or created
   */
  fun getOrPutFilterNodeDescriptor(
    basePath: List<String>,
    filterName: String,
    descriptorCreatorFn: () -> FetcherNodeDescriptor
  ): FetcherNodeDescriptor {
    return filterNodeDescriptors.getOrPut(basePath) { mutableMapOf() }
      .getOrPut(filterName) {
        createDescriptorWithPrefill(descriptorCreatorFn) as FetcherNodeDescriptor
      }
  }

  /**
   * Load nodes for a fetcher filter with the provided operation parameters
   * @param operation the [LoadNodesOperation] to get operation parameters from
   * @return a list of [ExplorerTreeNode]'s, formed as a result of the operation trigger
   */
  fun loadNodesForFetcherFilter(operation: LoadNodesOperation): List<ExplorerTreeNode> {
    return fetcherFilterNodesLoader.loadNodes(operation)
  }

  /**
   * Load nodes for a plain filter with the provided operation parameters
   * @param operation the [LoadNodesOperation] to get operation parameters from
   * @return a list of [ExplorerTreeNode]'s, formed as a result of the operation trigger
   */
  fun loadNodesForPlainFilter(operation: LoadNodesOperation): List<ExplorerTreeNode> {
    return plainFilterNodesLoader.loadNodes(operation)
  }

  /**
   * Refresh nodes for a fetcher filter with the provided operation parameters
   * @param operation the [RefreshNodesOperation] to get operation parameters from
   * @return a list of [ExplorerTreeNode]'s, formed as a result of the operation trigger
   */
  fun refreshNodesForFetcherFilter(operation: RefreshNodesOperation) {
    fetcherFilterNodesLoader.refreshNodes(operation)
  }

  /**
   * Refresh nodes for a plain filter with the provided operation parameters
   * @param operation the [RefreshNodesOperation] to get operation parameters from
   * @return a list of [ExplorerTreeNode]'s, formed as a result of the operation trigger
   */
  fun refreshNodesForPlainFilter(operation: RefreshNodesOperation) {
    plainFilterNodesLoader.refreshNodes(operation)
  }
}
