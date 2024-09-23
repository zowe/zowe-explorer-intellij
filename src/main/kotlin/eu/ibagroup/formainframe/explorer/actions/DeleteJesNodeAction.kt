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

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.showYesNoDialog
import eu.ibagroup.formainframe.explorer.JesWorkingSetImpl
import eu.ibagroup.formainframe.explorer.ui.JesExplorerView
import eu.ibagroup.formainframe.explorer.ui.JesFilterNode
import eu.ibagroup.formainframe.explorer.ui.JesWsNode
import eu.ibagroup.formainframe.explorer.ui.getExplorerView

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
    // Delete selected JES working sets
    selected
      .map { it.node }
      .filterIsInstance<JesWsNode>()
      .forEach {
        if (
          showYesNoDialog(
            title = "Deletion of JES Working Set ${it.unit.name}",
            message = "Do you want to delete this JES Working Set from configs? Note: all data under it will be untouched",
            project = e.project,
            icon = AllIcons.General.QuestionDialog
          )
        ) {
          view.explorer.disposeUnit(it.unit as JesWorkingSetImpl)
        }
      }
    // Delete selected job filters
    selected
      .map { it.node }
      .filterIsInstance<JesFilterNode>()
      .filter { view.explorer.isUnitPresented(it.unit) }
      .forEach {
        if (
          showYesNoDialog(
            title = "Deletion Of Jobs Filter",
            message = "Do you want to delete this jobs filter with ${it.value} from configs? Note: all data under the filter will be untouched",
            project = e.project,
            icon = AllIcons.General.QuestionDialog
          )
        ) {
          it.unit.removeFilter(it.value)
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
