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
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.showYesNoDialog
import org.zowe.explorer.dataops.content.synchronizer.checkForSync

/** Action to delete children filter for a dataset members filter */
class DeleteChildrenFilterAction : DumbAwareAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.EDT

  /**
   * Perform a filter delete action.
   * The "Delete" action for the filter means to make it an empty string.
   * Will proceed only if there is no sync actions in progress.
   * Will refresh the parent node after the filter is deleted
   */
  override fun actionPerformed(e: AnActionEvent) {
    if (checkForSync(e.project)) return
    val node = FilterChildrenHandler.getCompatibleParentNodeFromActionEvent(e) ?: return
    val project = e.project ?: return
    val shouldDelete = showYesNoDialog(
      title = "Delete Children Filter",
      message = "Do you want to delete the '${node.savedFilter}' filter?\n\nNote: the filter's parent node will be refreshed afterwards.",
      project = project,
      icon = AllIcons.General.WarningDialog
    )
    if (shouldDelete) {
      FilterChildrenHandler.changeFilter(node, project, "")
    }
  }

  /** Show a "Delete Filter" item in a context menu for a [FilterChildrenNode] instance */
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = FilterChildrenHandler.getSingleSelectedNode(e) is FilterChildrenNode<*>
  }
}