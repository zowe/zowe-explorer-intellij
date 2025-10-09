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

package org.zowe.explorer.v3.components.files

import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.tree.nodes.Renameable
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.FileFetcherNodeDescriptor

// TODO: doc
class DatasetMaskNodeDescriptor(
  displayName: String,
  connectionConfigUuid: String
) : FileFetcherNodeDescriptor(
  displayName,
  "Data set mask",
  ZoweExplorerIcons.datasetMask,
  connectionConfigUuid=connectionConfigUuid
), Renameable {
//  override suspend fun fetchChildren(): List<org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode> {
//    nodeDescriptor as DatasetMaskNodeData
//    return if (nodeDescriptor.connectionConfigUuid.isEmpty()) {
//      listOf(
//        ErrorNode(
//          project,
//          "Connection config is not defined",
//          "Check a config in the parent working set",
//          this
//        )
//      )
//    } else {
//      produceNoItemsFoundChildren()
//    }
//  }

  override fun getNodeChildren(node: ExplorerTreeNode): List<ExplorerTreeNode> {
    TODO("Not yet implemented")
  }

  override fun renameNode() {
    TODO("Not yet implemented")
  }
}