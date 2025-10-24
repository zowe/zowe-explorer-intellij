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
import org.zowe.explorer.v3.components.files.operations.LoadUssNodesOperation
import org.zowe.explorer.v3.components.files.operations.LoadUssNodesOperationData
import org.zowe.explorer.v3.components.files.operations.RefreshUssNodesOperation
import org.zowe.explorer.v3.components.files.operations.RefreshUssNodesOperationData
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import org.zowe.explorer.v3.tree.nodes.Renameable
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.FetcherNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.NoItemsFoundNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.Refreshable
import org.zowe.explorer.v3.tree.nodes.Traversable

// TODO: doc
class UssFolderNodeDescriptor(
  displayName: String,
  parentFetchFilter: String,
  override var connectionConfigUuid: String,
) : FetcherNodeDescriptor(
  displayName,
  basePath = UssFilterNodeDescriptor.formUssBasePathFromConnectionConfig(connectionConfigUuid),
  filterPath = UssFilterNodeDescriptor.formUssFilterPath(parentFetchFilter) + "$displayName/",
  "USS folder",
  AllIcons.Nodes.Folder,
  connectionConfigUuid=connectionConfigUuid
), Renameable, Refreshable, Traversable
{
  private val fetchPath = basePath + filterPath

  override val fetchFilter = filterPath.joinToString("").dropLast(1)

  override val placingPath = basePath + filterPath.dropLast(1)

  override val elemName = displayName

  override val invalidationPath = placingPath

  override val invalidationElem = displayName

  override fun getExactPath() = placingPath + elemName

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
          LoadUssNodesOperationData(node, fetchPath, fetchFilter)
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
    val operation = RefreshUssNodesOperation(
      RefreshUssNodesOperationData(node, fetchPath, fetchFilter)
    )
    operation.run()
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