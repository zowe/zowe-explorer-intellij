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

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.config.configCrudable
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.ui.files.WorkingSetDialog
import org.zowe.explorer.config.ws.ui.files.toDialogState
import org.zowe.explorer.explorer.ui.FILE_EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.FilesWorkingSetNode
import org.zowe.explorer.utils.clone
import org.zowe.explorer.utils.crudable.getByUniqueKey


class EditWorkingSetAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val node = view.mySelectedNodesData[0].node
    if (node is FilesWorkingSetNode) {
      var selected = configCrudable.getByUniqueKey<FilesWorkingSetConfig>(node.value.uuid)?.clone() as FilesWorkingSetConfig
      WorkingSetDialog(configCrudable, selected.toDialogState().apply { mode = DialogMode.UPDATE }).apply {
        if (showAndGet()) {
          selected = state.workingSetConfig
          configCrudable.update(selected)
        }
      }
    }

  }

  override fun isDumbAware(): Boolean {
    return true
  }

  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    e.presentation.isEnabledAndVisible = selected.size == 1 && (selected[0].node is FilesWorkingSetNode)
  }
}
