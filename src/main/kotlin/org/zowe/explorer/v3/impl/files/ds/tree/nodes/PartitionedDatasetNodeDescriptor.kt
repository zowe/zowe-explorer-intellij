/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.ds.tree.nodes

import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.impl.files.ds.operations.LoadMemberNodesOperation
import org.zowe.explorer.v3.impl.files.ds.operations.LoadMemberNodesOperationData
import org.zowe.explorer.v3.impl.files.ds.operations.RefreshMemberNodesOperation
import org.zowe.explorer.v3.impl.files.ds.operations.RefreshMemberNodesOperationData
import org.zowe.explorer.v3.impl.files.tree.nodes.FilesExplorerRelated
import org.zowe.explorer.v3.impl.formDsBasePath
import org.zowe.explorer.v3.newoperations.LoadNodesOperation
import org.zowe.explorer.v3.newoperations.RefreshNodesOperation
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.FetcherNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.Traversable

/**
 * Node descriptor for a partitioned data set (PDS).
 * Fetches members from the mainframe when expanded
 * @param displayName the data set name shown in the tree
 * @param connectionProfile the connection profile path from the Zowe Team Config
 */
class PartitionedDatasetNodeDescriptor(
  displayName: String,
  connectionProfile: String,
) : FetcherNodeDescriptor(
  displayName,
  basePath = formDsBasePath(connectionProfile),
  "Partitioned data set",
  ZoweExplorerIcons.libraryDataset,
  connectionProfile
), Traversable, FilesExplorerRelated
{
  override val fetchFilter = displayName

  override val placingPath = basePath
  override val elemName = displayName

  private val fetchPath = basePath + fetchFilter

  override fun generateLoadNodesOperation(node: ExplorerTreeNode): LoadNodesOperation {
    return LoadMemberNodesOperation(
      LoadMemberNodesOperationData(node, fetchPath, fetchFilter)
    )
  }

  override fun generateRefreshNodesOperation(node: ExplorerTreeNode): RefreshNodesOperation {
    return RefreshMemberNodesOperation(
      RefreshMemberNodesOperationData(node, fetchPath, fetchFilter)
    )
  }
}
