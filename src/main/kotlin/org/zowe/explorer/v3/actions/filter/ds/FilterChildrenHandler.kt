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

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.zowe.explorer.explorer.ui.ExplorerTreeNode
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.LibraryNode
import org.zowe.explorer.explorer.ui.cleanCacheIfPossible
import org.zowe.explorer.explorer.ui.getExplorerView

/**
 * The "Filter Children" functionality.
 * Provides common functions to handle the [FilterChildrenNode] and it's changes
 */
class FilterChildrenHandler {
  companion object {
    /**
     * Change the filter node with either a user-provided filter, or the specified [newFilterAlreadySet]
     * @param parentFilterNode the parent [LibraryNode] to change the [FilterChildrenNode] in
     * @param project the project to show the change dialog in
     * @param newFilterAlreadySet specifies an already predefined filter to put to the node. Is "null" by default
     */
    fun changeFilter(
      parentFilterNode: LibraryNode,
      project: Project,
      newFilterAlreadySet: String? = null
    ) {
      if (newFilterAlreadySet != null) {
        parentFilterNode.savedFilter = newFilterAlreadySet
        parentFilterNode.cleanCacheIfPossible(cleanBatchedQuery = true)
      } else {
        val dialog = FilterChildrenDialog(project, parentFilterNode)
        val newFilter = dialog.waitForUserInput()
        if (newFilter != parentFilterNode.savedFilter) {
          parentFilterNode.savedFilter = newFilter
          parentFilterNode.cleanCacheIfPossible(cleanBatchedQuery = true)
        }
      }
    }

    /**
     * Get a single selected node from the list of selected nodes.
     * Will return the node only if it is a [FileExplorerView] and if there is exactly 1 node selected,
     * otherwise will return "null"
     */
    fun getSingleSelectedNode(e: AnActionEvent): ExplorerTreeNode<*, *>? {
      val view = e.getExplorerView<FileExplorerView>() ?: return null
      val selectedNodesData = view.mySelectedNodesData
      return if (selectedNodesData.size != 1) null else selectedNodesData[0].node
    }

    /**
     * Get a [LibraryNode] either as a current selected one, or as a parent of a selected [FilterChildrenNode].
     * If the selected node is not compatible to get the [LibraryNode],
     * or several items are selected, will return "null"
     */
    fun getCompatibleParentNodeFromActionEvent(e: AnActionEvent): LibraryNode? {
      val node = getSingleSelectedNode(e)
      // It does not work otherwise
      return when (node) {
        is LibraryNode -> node as LibraryNode
        is FilterChildrenNode<*> -> node.parent as LibraryNode
        else -> null
      }
    }
  }
}