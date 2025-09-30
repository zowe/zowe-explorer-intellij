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

package org.zowe.explorer.v3.tree.nodes

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import javax.swing.Icon

// TODO: doc
abstract class ExplorerTreeNode(
  project: Project,
  protected val nodeData: ExplorerTreeNodeData,
  parent: ExplorerTreeNode? = null,
) : AbstractTreeNode<ExplorerTreeNodeData>(project, nodeData) {
  override fun getName(): String? {
    return nodeData.displayName
  }

  override fun setIcon(closedIcon: Icon?) {
    nodeData.icon = closedIcon
    super.setIcon(nodeData.icon)
    presentation.setIcon(nodeData.icon)
  }

  init {
    this.parent = parent
    icon = nodeData.icon
  }
}
