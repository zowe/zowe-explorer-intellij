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

import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.tree.nodes.RealNodeAssociation

// TODO: doc
class UssFilterNodeDescriptor(
  displayName: String,
  connectionConfigUuid: String
) : UssFetcherNodeDescriptor(
  displayName,
  filterPath = formUssFilterPath(displayName),
  "USS filter",
  ZoweExplorerIcons.ussFilter,
  connectionConfigUuid = connectionConfigUuid
), RealNodeAssociation
{
  override val fetchFilter = if (filterPath.size > 1) filterPath.joinToString("").dropLast(1) else filterPath[0]

  override val elemName = if (filterPath.size > 1) filterPath.last().dropLast(1) else ""
  override val placingPath = basePath + if (filterPath.size > 1) filterPath.dropLast(1) else filterPath

//  override fun renameNode() {
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
//  }

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
