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

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.FilesWorkingSet

/** File Explorer root node, that is hidden, but aggregates all nodes in JES Explorer. */
class FileExplorerTreeNodeRoot(
  explorer: Explorer<ConnectionConfig, FilesWorkingSet>,
  project: Project,
  treeStructure: ExplorerTreeStructureBase
) :
  ExplorerTreeNode<ConnectionConfig, Explorer<ConnectionConfig, FilesWorkingSet>>(explorer, project, null, explorer, treeStructure) {

  override fun isAlwaysExpand(): Boolean {
    return true
  }

  override fun update(presentation: PresentationData) {
  }

  /** Get the root node's files working set children */
  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return explorer.units.filterIsInstance<FilesWorkingSet>()
      .map { FilesWorkingSetNode(it, notNullProject, this, treeStructure) }.toMutableList()
  }
}
