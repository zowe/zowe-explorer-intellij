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

import com.intellij.ide.util.treeView.PresentableNodeDescriptor
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.v3.newoperations.LoadNodesOperation
import org.zowe.explorer.v3.newoperations.RefreshNodesOperation
import org.zowe.explorer.v3.state.config.ConnectionConfigRelated
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import javax.swing.Icon

// TODO: doc
abstract class FetcherNodeDescriptor(
  displayName: String,
  val basePath: List<String>,
  tooltip: String,
  icon: Icon,
  override val connectionConfigUuid: String
) : ExplorerTreeNodeDescriptor(displayName, tooltip, icon, isLeaf=false, hasExpandChevron=true),
  LazyExpandable, Refreshable, ConnectionConfigRelated, UpdateInfoHolder
{
  abstract val fetchFilter: String

  override val textToPreserve = listOf(
    PresentableNodeDescriptor.ColoredFragment(
      displayName,
      SimpleTextAttributes.REGULAR_ATTRIBUTES
    )
  )

  override var currentUpdateInfo: PresentableNodeDescriptor.ColoredFragment? = null

  override var wasExpanded = false

  fun setUpdateInfo(updateInfoToSet: PresentableNodeDescriptor.ColoredFragment? = null) {
    this.setUpdateInfo(genuinePresentationData, updateInfoToSet)
  }

  override fun expandNode(node: ExplorerTreeNode) {
    if (!wasExpanded) {
      wasExpanded = true
      ExplorerTreeComponentService.getService()
        .getFilesExplorerComponent(node.project)
        .invalidateNode(node)
    }
  }

  abstract fun generateLoadNodesOperation(node: ExplorerTreeNode): LoadNodesOperation

  override fun getNodeChildren(node: ExplorerTreeNode): List<ExplorerTreeNode> {
    return if (connectionConfigUuid.isEmpty()) {
      listOf(
        ExplorerTreeNode(
          NoItemsFoundNodeDescriptor("connection is not set"),
          node.project,
          node
        )
      )
    } else {
      if (wasExpanded) {
        generateLoadNodesOperation(node).run().loadedNodes
      } else {
        listOf(ExplorerTreeNode(NoItemsFoundNodeDescriptor(), node.project, node))
      }
    }
  }

  override fun isNodeReadyForRefresh(node: ExplorerTreeNode): Boolean {
    return connectionConfigUuid.isNotEmpty() && wasExpanded
  }

  abstract fun generateRefreshNodesOperation(node: ExplorerTreeNode): RefreshNodesOperation

  override fun refreshNode(node: ExplorerTreeNode) {
    generateRefreshNodesOperation(node).run()
  }
}
