/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.actions.sort.uss

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import eu.ibagroup.formainframe.explorer.ui.FileExplorerView
import eu.ibagroup.formainframe.explorer.ui.UssDirNode
import eu.ibagroup.formainframe.explorer.ui.getExplorerView

/**
 * Represents the custom USS files sort action group in the FileExplorerView context menu
 */
class UssSortActionGroup : DefaultActionGroup() {

  /**
   * Update method to determine if sorting is possible for particular item in the tree
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>()
    view ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selectedNodes = view.mySelectedNodesData
    val treePathFromModel = view.myTree.selectionPath
    e.presentation.apply {
      isEnabledAndVisible = selectedNodes.size == 1 && selectedNodes.any {
        it.node is UssDirNode && view.myTree.isExpanded(treePathFromModel)
      }
    }
  }

  /**
   * Tells that only UI component is affected
   */
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }
}