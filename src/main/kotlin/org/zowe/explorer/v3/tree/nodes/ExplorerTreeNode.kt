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

// TODO: doc
open class ExplorerTreeNode(
  var nodeDescriptor: ExplorerTreeNodeDescriptor,
  project: Project,
  parent: ExplorerTreeNode? = null
) : AbstractTreeNode<ExplorerTreeNodeDescriptor>(project, nodeDescriptor) {
  override fun isAlwaysLeaf(): Boolean {
    return nodeDescriptor.isLeaf
  }

  override fun isAlwaysExpand(): Boolean {
    return nodeDescriptor.hasExpandChevron
  }

  override fun getName(): String {
    return nodeDescriptor.displayName
  }

  override fun getChildren(): Collection<AbstractTreeNode<*>?> {
    return nodeDescriptor.getNodeChildren(this)
  }

  override fun update(presentation: PresentationData) {
    nodeDescriptor.updatePresentation(presentation)
  }

  init {
    this.parent = parent
    nodeDescriptor.associateNode(this)
  }
}