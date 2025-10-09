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

package org.zowe.explorer.v3.tree.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project

// TODO: doc
class RootNode(project: Project) : ExplorerTreeNode(RootNodeDescriptor(), project) {
  val workingSetNodes = hashSetOf<ExplorerTreeNode>()

  override fun getChildren(): Collection<AbstractTreeNode<*>?> {
    return workingSetNodes
  }

  override fun isAlwaysExpand() = true

  // Must always be hidden
  override fun update(presentation: PresentationData) {}
}