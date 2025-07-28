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

package org.zowe.explorer.v3.actions.filter.ds

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.config.connect.ConnectionConfigBase
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.ui.ExplorerTreeNode
import org.zowe.explorer.explorer.ui.ExplorerTreeStructureBase
import org.zowe.explorer.explorer.ui.LibraryNode

/**
 * Node with a specified filter to filter dataset members
 * @param project the project to show the node in
 * @param parent the parent node to show the filter node in
 * @param explorer the explorer to show the node in
 * @param treeStructure the tree structure to put the filter in
 * @param savedFilter the current filter applied to the node
 */
class FilterChildrenNode<Connection : ConnectionConfigBase>(
  project: Project,
  parent: ExplorerTreeNode<Connection, *>,
  explorer: Explorer<Connection, *>,
  treeStructure: ExplorerTreeStructureBase,
  var savedFilter: String = ""
) : ExplorerTreeNode<Connection, Any>(Any(), project, parent, explorer, treeStructure) {
  override fun isAlwaysLeaf() = true
  override fun getChildren() = mutableListOf<AbstractTreeNode<*>>()
  override fun canNavigate() = true

  override fun update(presentation: PresentationData) {
    presentation.addText("Filter: ${savedFilter.ifEmpty { "none" }}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    presentation.setIcon(AllIcons.General.Filter)
  }

  override fun navigate(requestFocus: Boolean) {
    FilterChildrenHandler.changeFilter(this.parent as LibraryNode, project)
  }
}