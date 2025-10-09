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

package org.zowe.explorer.v3.tree

import com.intellij.ide.util.treeView.AbstractTreeStructureBase
import com.intellij.openapi.project.Project
import org.zowe.explorer.explorer.ExplorerViewSettings
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.RootNode
import org.zowe.explorer.v3.tree.nodes.WorkingSetNodeDescriptor

// TODO: doc
abstract class ExplorerTreeStructure(project: Project) : AbstractTreeStructureBase(project), ExplorerViewSettings {
  protected abstract val rootNode: RootNode

  /**
   * Register a working set node as the first-level children to the root node
   * @param node the node to register
   */
  fun registerWorkingSetNode(node: ExplorerTreeNode) {
    node.nodeDescriptor as? WorkingSetNodeDescriptor ?: throw Exception("Incorrect node to register: $node")
    rootNode.workingSetNodes.add(node)
    // TODO: notify others
  }

  /**
   * Unregister a working set node as the first-level children to the root node
   * @param node the node to unregister
   */
  fun unregisterWorkingSetNode(node: ExplorerTreeNode) {
    // TODO: notify others
    node.nodeDescriptor as? WorkingSetNodeDescriptor ?: throw Exception("Incorrect node to unregister: $node")
    rootNode.workingSetNodes.remove(node)
  }

  override fun getRootElement() = rootNode

  override fun getProviders() = null

  override fun commit() {}

  override fun hasSomethingToCommit() = false

}
