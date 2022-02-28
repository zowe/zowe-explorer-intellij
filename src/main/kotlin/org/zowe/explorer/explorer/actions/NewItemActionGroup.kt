/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import org.zowe.explorer.explorer.ui.FILE_EXPLORER_CONTEXT_MENU

class NewItemActionGroup : DefaultActionGroup() {


  override fun update(e: AnActionEvent) {
    e.presentation.icon = if (e.place != FILE_EXPLORER_CONTEXT_MENU) {
      null
    } else {
      AllIcons.General.Add
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}
