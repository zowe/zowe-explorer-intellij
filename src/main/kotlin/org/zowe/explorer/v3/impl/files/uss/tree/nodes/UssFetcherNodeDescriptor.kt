/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.uss.tree.nodes

import org.zowe.explorer.v3.impl.files.tree.nodes.FilesExplorerRelated
import org.zowe.explorer.v3.impl.files.uss.operations.LoadUssNodesOperation
import org.zowe.explorer.v3.impl.files.uss.operations.RefreshUssNodesOperation
import org.zowe.explorer.v3.impl.formUssBasePath
import org.zowe.explorer.v3.impl.files.uss.operations.LoadUssNodesOperationData
import org.zowe.explorer.v3.impl.files.uss.operations.RefreshUssNodesOperationData
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.FetcherNodeDescriptor
import javax.swing.Icon

/**
 * Base node descriptor for USS fetcher nodes.
 * Fetches USS file system entries from the mainframe when expanded
 * @param displayName the USS path or filter shown in the tree
 * @param filterPath the parsed path segments derived from [displayName]
 * @param tooltip the tooltip text
 * @param icon the node icon
 * @param connectionProfile the connection profile path from the Zowe Team Config
 */
abstract class UssFetcherNodeDescriptor(
  displayName: String,
  protected val filterPath: List<String>,
  tooltip: String,
  icon: Icon,
  connectionProfile: String
) : FetcherNodeDescriptor(
  displayName,
  basePath = formUssBasePath(connectionProfile),
  tooltip,
  icon,
  connectionProfile
), FilesExplorerRelated {
  companion object {
    fun formUssFilterPath(displayName: String): List<String> {
      return if (displayName == "/") listOf(displayName)
        else displayName.split("/").map { "$it/"}
    }
  }

  protected val fetchPath = basePath + filterPath

  override fun generateLoadNodesOperation(node: ExplorerTreeNode): LoadUssNodesOperation {
    return LoadUssNodesOperation(
      LoadUssNodesOperationData(node, fetchPath, fetchFilter)
    )
  }

  override fun generateRefreshNodesOperation(node: ExplorerTreeNode): RefreshUssNodesOperation {
    return RefreshUssNodesOperation(
      RefreshUssNodesOperationData(node, fetchPath, fetchFilter)
    )
  }
}
