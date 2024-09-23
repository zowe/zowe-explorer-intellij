/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeNode
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeStructureBase

open class TestExplorerTreeStructureBase(
  explorer: Explorer<*, *>,
  project: Project
) : ExplorerTreeStructureBase(explorer, project) {
  override fun registerNode(node: ExplorerTreeNode<*, *>) {
    TODO("Not yet implemented")
  }

  override fun refreshSimilarNodes(node: ExplorerTreeNode<*, *>) {
    TODO("Not yet implemented")
  }

  override fun <V : Any> findByValue(value: V): Collection<ExplorerTreeNode<*, V>> {
    TODO("Not yet implemented")
  }

  override fun findByPredicate(predicate: (ExplorerTreeNode<*, *>) -> Boolean): Collection<ExplorerTreeNode<*, *>> {
    TODO("Not yet implemented")
  }

  override fun findByVirtualFile(file: VirtualFile): Collection<ExplorerTreeNode<*, *>> {
    TODO("Not yet implemented")
  }

  override fun getRootElement(): Any {
    TODO("Not yet implemented")
  }

  override fun commit() {
    TODO("Not yet implemented")
  }

  override fun hasSomethingToCommit(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getProviders(): MutableList<TreeStructureProvider>? {
    TODO("Not yet implemented")
  }
}
