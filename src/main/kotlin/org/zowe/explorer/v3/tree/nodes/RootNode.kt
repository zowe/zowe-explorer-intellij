/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.tree.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project

/**
 * A base root node of an explorer view. Is invisible and holds profile nodes as direct children
 * @property project the [Project] where the node is initialized
 * @property treeNodes the child nodes of the explorer tree view
 */
class RootNode(project: Project) : ExplorerTreeNode(RootNodeDescriptor(), project) {
  val treeNodes = linkedSetOf<ExplorerTreeNode>()

  override fun getChildren(): Collection<AbstractTreeNode<*>?> {
    return treeNodes
  }

  override fun isAlwaysExpand() = true

  // Must always be hidden
  override fun update(presentation: PresentationData) {}
}