/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import eu.ibagroup.formainframe.explorer.ui.FILE_EXPLORER_VIEW

// TODO: doc
class ShowWsInfo : ToggleAction() {

  override fun isSelected(e: AnActionEvent): Boolean {
    return e.getData(FILE_EXPLORER_VIEW)?.myFsTreeStructure?.showWorkingSetInfo == true
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    view.myFsTreeStructure.showWorkingSetInfo = state
    view.myFsTreeStructure.findByValue(view.explorer).forEach {
      view.myStructure.invalidate(it, true)
    }
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = e.getData(FILE_EXPLORER_VIEW) != null
  }

}