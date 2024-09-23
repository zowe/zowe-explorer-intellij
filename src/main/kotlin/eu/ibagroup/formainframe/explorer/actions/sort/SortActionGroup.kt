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

package eu.ibagroup.formainframe.explorer.actions.sort

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import eu.ibagroup.formainframe.explorer.ui.*

/**
 * Abstract class represents the custom sort action group in the FileExplorerView/JesExplorerView context menu
 */
abstract class SortActionGroup : DefaultActionGroup() {

  /**
   * Function gets the source view where the action is triggered
   * @param e
   * @return an instance of ExplorerTreeView
   */
  abstract fun getSourceView(e: AnActionEvent) : ExplorerTreeView<*, *, *>?

  /**
   * Function checks if the selected node is suitable to display the Sort actions in the ContextMenu group
   * @param node
   * @returns true if node is a candidate, false otherwise
   */
  abstract fun checkNode(node: ExplorerTreeNode<*, *>) : Boolean

  /**
   * Update method to determine if sorting is possible for particular item in the tree
   */
  override fun update(e: AnActionEvent) {
    val view = getSourceView(e)
    view ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selectedNodes = view.mySelectedNodesData
    val treePathFromModel = view.myTree.selectionPath
    e.presentation.apply {
      isEnabledAndVisible = selectedNodes.size == 1 && view.myTree.isExpanded(treePathFromModel) && checkNode(selectedNodes[0].node)
    }
  }

  /**
   * Tells that only UI component is affected
   */
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }
}
