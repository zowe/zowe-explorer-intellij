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
 */

package org.zowe.explorer.v3.components.files

import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.NoItemsFoundNode
import org.zowe.explorer.v3.tree.nodes.RefreshInfoNodeData
import org.zowe.explorer.v3.tree.nodes.RefreshableNode

// TODO: doc
class DatasetMaskNode(
  project: Project,
  nodeData: RefreshInfoNodeData,
  parent: ExplorerTreeNode
) : RefreshableNode(project, nodeData, parent) {
  override fun fetchChildren(): List<ExplorerTreeNode> {
    return listOf(NoItemsFoundNode(project, parent = this))
  }
}
