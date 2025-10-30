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

import org.zowe.explorer.v3.components.files.operations.LoadMemberNodesOperation
import org.zowe.explorer.v3.components.files.operations.LoadMemberNodesOperationData
import org.zowe.explorer.v3.components.files.operations.RefreshMemberNodesOperation
import org.zowe.explorer.v3.components.files.operations.RefreshMemberNodesOperationData
import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.newoperations.LoadNodesOperation
import org.zowe.explorer.v3.newoperations.RefreshNodesOperation
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.FetcherNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.Traversable

// TODO: doc
class PartitionedDatasetNodeDescriptor(
  displayName: String,
  connectionConfigUuid: String,
) : FetcherNodeDescriptor(
  displayName,
  basePath = DatasetMaskNodeDescriptor.formDsBasePathFromConnectionConfig(connectionConfigUuid),
  "Partitioned data set",
  ZoweExplorerIcons.libraryDataset,
  connectionConfigUuid
), Traversable
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
