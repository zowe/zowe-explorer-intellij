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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import org.zowe.explorer.v3.newoperations.LoadNodesOperation
import org.zowe.explorer.v3.newoperations.RefreshNodesOperation
import org.zowe.explorer.v3.performWithProgressiveDelay
import org.zowe.explorer.v3.tree.nodes.path.PathTree

/**
 * Nodes loader abstraction. Provides common functions to operate on the nodes loading mechanism
 * for any kind of filters or fetchers
 * @property pathTree the path tree to operate on during nodes loading
 * @property filterNodeDescriptors the filter nodes to produce respective changes during nodes loading
 */
abstract class NodesLoader(
  protected val pathTree: PathTree,
  protected val filterNodeDescriptors: MutableMap<List<String>, MutableMap<String, FetcherNodeDescriptor>>
) {
  /**
   * Find a real node descriptor by the provided info
   * @param placingPath the path where the element is expected to be
   * @param elemName the element name to find in the specified path
   * @return [FetcherNodeDescriptor] if the real node is found or null otherwise
   */
  protected fun findRealNodeDescriptor(placingPath: List<String>, elemName: String): FetcherNodeDescriptor? {
    return pathTree.getPathElement(placingPath, elemName) as? FetcherNodeDescriptor
  }

  /**
   * Find both real and filter node descriptors by the provided [FetcherNodeDescriptor]
   * @param fetcherNodeDescriptor the node descriptor to find the other descriptors by
   * @return the list of found real and filter node descriptors, related to the provided node descriptor
   */
  abstract fun findRelatedDescriptors(fetcherNodeDescriptor: FetcherNodeDescriptor): List<FetcherNodeDescriptor>

  /**
   * Start children loading process. Expects to load nodes from a remote endpoint
   * @param operation the [LoadNodesOperation] instance to get all the parameters to start the nodes loading process
   * @return the list of [ExplorerTreeNode]'s as the result of the process triggering
   */
  protected abstract fun startChildrenLoading(operation: LoadNodesOperation): List<ExplorerTreeNode>

  /**
   * Get busy children list for by the respective parameters
   * @param operation the [LoadNodesOperation] to get the respective busy nodes by
   * @return the list of [ExplorerTreeNode]'s with the respective state
   */
  protected abstract fun getBusyChildren(operation: LoadNodesOperation): List<ExplorerTreeNode>

  /**
   * Get already loaded children for the specified parameters
   * @param operation the [LoadNodesOperation] to get the nodes by
   * @return the list of previously loaded [ExplorerTreeNode]'s
   */
  protected abstract fun getLoadedChildren(operation: LoadNodesOperation): List<ExplorerTreeNode>

  /**
   * Load nodes with the provided operation parameters
   * @param operation the [LoadNodesOperation] to get all the necessary info to complete the operation
   * @return the list of [ExplorerTreeNode]'s, produced during loading
   */
  abstract fun loadNodes(operation: LoadNodesOperation): List<ExplorerTreeNode>

  /**
   * Refresh nodes by the specified parameters
   * @param operation the [RefreshNodesOperation] to refresh respective nodes by
   */
  abstract fun refreshNodes(operation: RefreshNodesOperation)

  /**
   * Find a filter node descriptor by the provided info
   * @param basePath the base path the filter is expected to be under
   * @param filterName the filter name to find the filter by
   * @return [FetcherNodeDescriptor] if the filter node is found or null otherwise
   */
  fun findFilterNodeDescriptor(basePath: List<String>, filterName: String): FetcherNodeDescriptor? {
    return filterNodeDescriptors.getOrDefault(basePath, mapOf())[filterName]
  }

  protected suspend fun CoroutineScope.fetchChildrenWithCancellation(
    operation: LoadNodesOperation,
    indicator: ProgressIndicator
  ): List<ExplorerTreeNode>? {
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
      return null
    }

    return fetchJob.await()
  }
}