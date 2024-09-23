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
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.IdeFocusManager
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.files.FilesWorkingSetDialog
import eu.ibagroup.formainframe.config.ws.ui.files.toDialogState
import eu.ibagroup.formainframe.explorer.ui.FileExplorerView
import eu.ibagroup.formainframe.explorer.ui.FilesWorkingSetNode
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey

/**
 * Action class for edit working set act
 */
class EditFilesWorkingSetAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Called when edit working set option is chosen from context menu
   * Opens the working set table with elements to edit
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: return
    when (val node = view.mySelectedNodesData[0].node) {
      is FilesWorkingSetNode -> {
        val workingSetConfig =
          ConfigService.getService().crudable
            .getByUniqueKey<FilesWorkingSetConfig>(node.value.uuid)
            ?.clone() as FilesWorkingSetConfig
        service<IdeFocusManager>().runOnOwnContext(
          DataContext.EMPTY_CONTEXT
        ) {
          FilesWorkingSetDialog(
            ConfigService.getService().crudable,
            workingSetConfig.toDialogState().apply { mode = DialogMode.UPDATE }
          )
            .apply {
              if (showAndGet()) {
                val dialogState = state
                ConfigService.getService().crudable.update(dialogState.workingSetConfig)
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
