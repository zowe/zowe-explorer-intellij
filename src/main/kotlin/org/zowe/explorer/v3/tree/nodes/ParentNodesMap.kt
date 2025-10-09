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

package org.zowe.explorer.v3.tree.nodes

import com.intellij.openapi.project.Project
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService

// TODO: doc
class ParentNodesMap {
  /** Node path to the path parents map. Is needed to refresh existing parent nodes on the path invalidate request */
  private val pathToParents: MutableMap<List<String>, MutableList<ExplorerTreeNode>> by lazy { mutableMapOf() }

  private fun formProjectToNodeMap(nodes: List<ExplorerTreeNode>): Map<Project, List<ExplorerTreeNode>> {
    return nodes.fold(mutableMapOf<Project, MutableList<ExplorerTreeNode>>()) { projectsToNodes, node ->
      projectsToNodes.getOrPut(node.project) { mutableListOf() }.add(node)
      projectsToNodes
    }
  }

  private fun performFunOnPathAndInvalidate(
    path: List<String>,
    opFun: (ExplorerTreeNode) -> Unit,
    withChildren: Boolean = true
  ) {
    val nodesToUpdate = pathToParents.getOrDefault(path, mutableListOf())
    formProjectToNodeMap(nodesToUpdate)
      .forEach { (project, nodes) ->
        val filesExplorerComponent = ExplorerTreeComponentService.getService()
          .getFilesExplorerComponent(project)
        nodes.forEach { node ->
          opFun(node)
          filesExplorerComponent.invalidateNode(node, withChildren)
        }
      }
  }

  // TODO: a better mechanism to register/unregister parent nodes (because it is always possible to forget to do so)
  /**
   * Register a parent node to be able to refresh its view on the invalidation request
   * @param node the node to register. It is a mandatory for the node descriptor to be a [Traversable] in order to be
   *             able to register the node. If it is not - a warning is produced
   */
  fun registerParentNode(node: ExplorerTreeNode) {
    val traversableNodeDescriptor = node.nodeDescriptor as? Traversable
    if (traversableNodeDescriptor == null) {
      NotificationsService.getService()
        .notifyWarning(
          node.project,
          "Incorrect node provided as parent to register",
          "Make sure that the parent node has a Traversable interface implemented. Provided node: $node",
          ""
        )
    } else {
      pathToParents.getOrPut(traversableNodeDescriptor.path) { mutableListOf() }
        .add(node)
    }
  }

  /**
   * Unregister a parent node to prevent its refreshing.
   * Usually is needed when the node is being removed from the explorer
   * @param node the node to register. It is a mandatory for the node descriptor to be a [Traversable] in order to be
   *             able to register the node. If it is not - a warning is produced
   */
  fun unregisterParentNode(node: ExplorerTreeNode) {
    val traversableNodeDescriptor = node.nodeDescriptor as? Traversable
    if (traversableNodeDescriptor == null) {
      NotificationsService.getService()
        .notifyWarning(
          node.project,
          "Incorrect node provided as parent to unregister",
          "Make sure that the parent node has a Traversable interface implemented. Provided node: $node",
          ""
        )
    } else {
      pathToParents.getOrPut(traversableNodeDescriptor.path) { mutableListOf() }
        .remove(node)
    }
  }

  fun invalidatePath(path: List<String>, withChildren: Boolean = true) {
    performFunOnPathAndInvalidate(path, {}, withChildren)
  }

  fun setRefreshInfoForPath(path: List<String>) {
    val opFun = { node: ExplorerTreeNode ->
      val refreshInfoNodeDescriptor = node.nodeDescriptor as? RefreshInfoNodeDescriptor
      if (refreshInfoNodeDescriptor != null) {
        refreshInfoNodeDescriptor.refreshInfo = "last refresh: ${refreshInfoNodeDescriptor.getCurrentRefreshDateTime()}"
      }
    }
    performFunOnPathAndInvalidate(path, opFun, false)
  }
}