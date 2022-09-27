/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.FilesWorkingSet

/**
 * File Explorer root node, where the information about the connection is situated.
 * Provides interaction with the root node
 */
class FileExplorerTreeNodeRoot(
  explorer: Explorer<FilesWorkingSet>,
  project: Project,
  treeStructure: ExplorerTreeStructureBase
) :
  ExplorerTreeNode<Explorer<FilesWorkingSet>>(explorer, project, null, explorer, treeStructure) {

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
