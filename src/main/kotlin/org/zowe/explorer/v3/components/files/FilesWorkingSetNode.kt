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

import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeRootNode
import org.zowe.explorer.v3.tree.nodes.NoItemsFoundNode
import org.zowe.explorer.v3.tree.nodes.WorkingSetNode

// TODO: doc
class FilesWorkingSetNode(
  project: Project,
  nodeData: FilesWorkingSetNodeData,
  parent: ExplorerTreeRootNode
) : WorkingSetNode(project, nodeData, parent) {
  override fun fetchChildren(): List<ExplorerTreeNode> {
    nodeData as FilesWorkingSetNodeData
    val dsMasks = nodeData.config.dsMasks
    val ussFilters = nodeData.config.ussPaths
    val dsMaskNodes = dsMasks
      .map {
        DatasetMaskNode(
          project,
          DatasetMaskNodeData(
            it.mask,
            connectionConfigUuid=nodeData.config.connectionConfigUuid
          ),
          this
        )
      }
    val ussFilterNodes = ussFilters
      .map {
        UssFilterNode(
          project,
          UssFilterNodeData(
            it.path,
            connectionConfigUuid=nodeData.config.connectionConfigUuid
          ),
          this
        )
      }
    return (dsMaskNodes + ussFilterNodes)
      .ifEmpty { listOf(NoItemsFoundNode(project, parent=this)) }
  }
}
