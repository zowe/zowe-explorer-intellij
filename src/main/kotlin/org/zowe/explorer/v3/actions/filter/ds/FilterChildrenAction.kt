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

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.zowe.explorer.dataops.content.synchronizer.checkForSync

/** Filter children (members) action in a [org.zowe.explorer.explorer.ui.LibraryNode] */
class FilterChildrenAction : DumbAwareAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.EDT

  /**
   * Perform a filter children action.
   * Will create a filter string if a user specifies the member name pattern correctly in a respective dialog.
   * Will proceed only if there is no sync actions in progress.
   * Will refresh the parent node after the filter is added
   */
  override fun actionPerformed(e: AnActionEvent) {
    if (checkForSync(e.project)) return
    val node = FilterChildrenHandler.getCompatibleParentNodeFromActionEvent(e) ?: return
    val project = e.project ?: return
    FilterChildrenHandler.changeFilter(node, project)
  }

  /**
   * Show an "Apply Filter" item in a context menu for a [org.zowe.explorer.explorer.ui.LibraryNode] if the filter
   * was not yet specified, and an "Edit Filter" item both for [org.zowe.explorer.explorer.ui.LibraryNode]
   * and [FilterChildrenNode] if the filter is already specified
   */
  override fun update(e: AnActionEvent) {
    val node = FilterChildrenHandler.getCompatibleParentNodeFromActionEvent(e)
    e.presentation.isEnabledAndVisible = node != null
    e.presentation.text = if (node?.savedFilter?.isNotEmpty() ?: false) "Edit Filter"
      else "Apply Filter"
  }
}