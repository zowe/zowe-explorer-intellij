/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.components.files

import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.PresentableNodeDescriptor
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.v3.components.files.operations.LoadUssNodesOperation
import org.zowe.explorer.v3.components.files.operations.LoadUssNodesOperationData
import org.zowe.explorer.v3.components.files.operations.RefreshUssNodesOperation
import org.zowe.explorer.v3.components.files.operations.RefreshUssNodesOperationData
import org.zowe.explorer.v3.tree.nodes.Renameable
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.FileFetcherNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.LazyExpandable
import org.zowe.explorer.v3.tree.nodes.NoItemsFoundNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.Refreshable
import org.zowe.explorer.v3.tree.nodes.UpdateInfoHolder

// TODO: doc
class UssFolderNodeDescriptor(
  displayName: String,
  var pathFilter: String,
  override var path: List<String>,
  override var connectionConfigUuid: String,
) : FileFetcherNodeDescriptor(
  displayName,
  "USS folder",
  AllIcons.Nodes.Folder,
  connectionConfigUuid=connectionConfigUuid
), Renameable, LazyExpandable, Refreshable, UpdateInfoHolder {
  override var wasExpanded = false
  override val textToPreserve = listOf(
    PresentableNodeDescriptor.ColoredFragment(
      displayName,
      SimpleTextAttributes.REGULAR_ATTRIBUTES
    )
  )

  override fun expandNode(node: ExplorerTreeNode) {
    if (!wasExpanded) {
      setUpdateInfo(genuinePresentationData)
    }
    wasExpanded = true
    getNodeChildren(node)
    invalidateAssociatedNodes()
  }

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
        val operation = LoadUssNodesOperation(
          LoadUssNodesOperationData(node, path, pathFilter)
        )
        operation.run().loadedNodes
      } else {
        listOf(ExplorerTreeNode(NoItemsFoundNodeDescriptor(), node.project, node))
      }
    }
  }

  override fun isNodeReadyForRefresh(node: ExplorerTreeNode): Boolean {
    return connectionConfigUuid.isNotEmpty() && wasExpanded
  }

  override fun refreshNode(node: ExplorerTreeNode) {
    setUpdateInfo(genuinePresentationData)
    val operation = RefreshUssNodesOperation(
      RefreshUssNodesOperationData(node, path, pathFilter)
    )
    operation.run()
    invalidateAssociatedNodes()
  }

  override fun renameNode() {
    TODO("Not yet implemented")
//    if (nodeState == State.BUSY) {
//      NotificationsService.getService()
//        .notifyWarning(
//          project,
//          "Node is busy",
//          "Rename is not possible while the node has an active job in progress",
//          ""
//        )
//    } else {
//      val dialog = RenameUssEntityDialog(project, this, nodeDescriptor.displayName)
//      if (dialog.showAndGet()) {
//        NodeSyncService.getService()
//          .unregisterNode(this)
//        nodeDescriptor.displayName = dialog.state
//        // TODO: update config, update the node by the config after?
//        if (nodeState == State.LOADED) {
//          refreshNode()
//        } else {
//          ExplorerTreeComponentService.getService()
//            .getFilesExplorerComponent(project)
//            .invalidateNode(this, false)
//        }
//      }
//    }
  }

//  override fun updateNode(presentationData: PresentationData) {
//    presentationData.tooltip = tooltip
//    presentationData.addText(displayName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
//
//    val fetchedFilesInfo = if (fetchedFilesCount != 0) "$fetchedFilesCount file(s)" else ""
//    val additionalInfo = listOf(fetchedFilesInfo, updateInfo).filterNot { it.isEmpty() }.joinToString(", ")
//    if (additionalInfo.isNotEmpty()) {
//      presentationData.addText(" $additionalInfo", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
//    }
//  }
}