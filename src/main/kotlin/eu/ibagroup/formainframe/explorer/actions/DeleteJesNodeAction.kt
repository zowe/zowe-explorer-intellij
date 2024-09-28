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

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.explorer.ui.JesExplorerView
import eu.ibagroup.formainframe.explorer.ui.JesFilterNode
import eu.ibagroup.formainframe.explorer.ui.JesWsNode
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import eu.ibagroup.formainframe.utils.performUnitsDeletionBasedOnSelection

/**
 * Action class for delete JES node action (working set or filter)
 */
class DeleteJesNodeAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Called when delete JES element option is chosen from context menu
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: return
    val selected = view.mySelectedNodesData

    // Find items to delete (working set node or filter node)
    val suitableToDelete = selected.map { it.node }
      .filter { node -> node is JesWsNode || node is JesFilterNode }

    val (workingSetsToDelete, selectedFilters) = suitableToDelete.partition { node -> node is JesWsNode }
    val jesFiltersToDelete = selectedFilters.filter { jesFilter -> !workingSetsToDelete.contains(jesFilter.parent) }

    // Delete working sets and filters that do not belong to them
    (workingSetsToDelete + jesFiltersToDelete).apply {
      if (isNotEmpty()) {
        performUnitsDeletionBasedOnSelection(e.project, null, view)
      }
    }
  }

  /**
   * This method is needed for interface implementation
   */
  override fun isDumbAware(): Boolean {
    return true
  }

  /**
   * Determines which objects are JES nodes and therefore can be deleted
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    e.presentation.isEnabledAndVisible = selected.isNotEmpty()
      && (selected[0].node is JesWsNode || selected[0].node is JesFilterNode)
  }
}
