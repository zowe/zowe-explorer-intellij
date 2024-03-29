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
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.IdeFocusManager
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.config.configCrudable
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.ui.files.FilesWorkingSetDialog
import org.zowe.explorer.config.ws.ui.files.toDialogState
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.FilesWorkingSetNode
import org.zowe.explorer.explorer.ui.getExplorerView
import org.zowe.explorer.utils.clone
import org.zowe.explorer.utils.crudable.getByUniqueKey

/**
 * Action class for edit working set act
 */
class EditFilesWorkingSetAction : AnAction() {

  /**
   * Called when edit working set option is chosen from context menu
   * Opens the working set table with elements to edit
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: return
    when (val node = view.mySelectedNodesData[0].node) {
      is FilesWorkingSetNode -> {
        val workingSetConfig =
          configCrudable.getByUniqueKey<FilesWorkingSetConfig>(node.value.uuid)?.clone() as FilesWorkingSetConfig
        service<IdeFocusManager>().runOnOwnContext(
          DataContext.EMPTY_CONTEXT
        ) {
          FilesWorkingSetDialog(configCrudable, workingSetConfig.toDialogState().apply { mode = DialogMode.UPDATE })
            .apply {
              if (showAndGet()) {
                val dialogState = state
                configCrudable.update(dialogState.workingSetConfig)
              }
            }
        }
      }
      else -> {
        return
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
   * Determines which objects are working sets and therefore can be edited
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    e.presentation.isEnabledAndVisible =
      selected.size == 1 && (selected[0].node is FilesWorkingSetNode)
  }
}
