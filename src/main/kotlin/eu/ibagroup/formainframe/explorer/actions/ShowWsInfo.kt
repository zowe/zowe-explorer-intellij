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

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import eu.ibagroup.formainframe.explorer.ui.FileExplorerView
import eu.ibagroup.formainframe.explorer.ui.getExplorerView

/** Action class to represent the action that will show a working set contents on the action is performed */
class ShowWsInfo : ToggleAction() {

  /**
   * Is the working set contents are shown
   * @param e an action event to get the file explorer view to check whether the working set contents are shown
   * @return true if the working set contents are shown
   */
  override fun isSelected(e: AnActionEvent): Boolean {
    return e.getExplorerView<FileExplorerView>()?.myFsTreeStructure?.showWorkingSetInfo == true
  }

  /**
   * Update the file explorer view to show the working set contents on the action selected
   * @param e an action event to get the view to update
   * @param state a variable to represent the toggle selection action
   */
  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    view.myFsTreeStructure.showWorkingSetInfo = state
    view.myFsTreeStructure.findByValue(view.explorer).forEach {
      view.myStructure.invalidate(it, true)
    }
  }

  /** Show the action until there is a file explorer view present */
  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = e.getExplorerView<FileExplorerView>() != null
  }

}
