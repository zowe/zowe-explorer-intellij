/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.tree

import com.intellij.ide.util.treeView.AbstractTreeStructureBase
import com.intellij.openapi.project.Project
import org.zowe.explorer.explorer.ExplorerViewSettings
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.RootNode

// TODO: doc
abstract class ExplorerTreeStructure(project: Project) : AbstractTreeStructureBase(project), ExplorerViewSettings {
  protected abstract val rootNode: RootNode

  fun registerNode(node: ExplorerTreeNode) {
    rootNode.treeNodes.add(node)
  }

  fun unregisterNode(node: ExplorerTreeNode) {
    rootNode.treeNodes.remove(node)
  }

  abstract fun addEntriesFromConfig()

  abstract fun syncEntriesWithConfig()

  override fun getRootElement() = rootNode

  override fun getProviders() = null

  override fun commit() {}

  override fun hasSomethingToCommit() = false
}
