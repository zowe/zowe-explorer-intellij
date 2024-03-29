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
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.operations.RenameOperation
import org.zowe.explorer.explorer.ui.*

/**
 * Class which represents a duplicate member action
 */
class DuplicateMemberAction : AnAction() {

  /**
   * The method of AnAction abstract class. Tells what to do if an action was submitted
   */
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val view = e.getExplorerView<FileExplorerView>() ?: return
    val selectedNode = view.mySelectedNodesData[0]
    val attributes = selectedNode.attributes ?: return
    val initialState = ""
    val dialog = RenameDialog(project, "Member", selectedNode, this, initialState)
    if (dialog.showAndGet()) {
      runDuplicateOperation(project, view, selectedNode, dialog.state)
    }
  }

  /**
   * Method to run duplicate operation. It passes the control to rename operation runner
   * @param project - current project
   * @param view - an explorer tree view object
   * @param selectedNode - a current selected node
   * @param newName - a new name of the virtual file in VFS
   * @throws any throwable during the processing of the request
   * @return Void
   */
  private fun runDuplicateOperation(project : Project, view : ExplorerTreeView<ConnectionConfig, *,*>, selectedNode : NodeData<ConnectionConfig>, newName: String) {
    val dataOpsManager = view.explorer.componentManager.getService(DataOpsManager::class.java)
    val attributes = selectedNode.attributes ?: return
    val file = selectedNode.file ?: return
    val parent = selectedNode.node.parent
    runBackgroundableTask(
      title = "Copying file ${file.name}",
      project = project,
      cancellable = true
    ) {
      runCatching {
        dataOpsManager.performOperation(
          operation = RenameOperation(
            file = file,
            attributes = attributes,
            newName = newName,
            requester = this
          ),
          progressIndicator = it
        )
      }.onSuccess {
        if (parent is FetchNode) {
          parent.cleanCacheIfPossible(cleanBatchedQuery = true)
        }
      }.onFailure {
        selectedNode.node.explorer.reportThrowable(it, project)
      }
    }
  }

  /**
   * Determines if an action is dumb aware
   */
  override fun isDumbAware(): Boolean {
    return true
  }

  /**
   * Method determines if an action is visible for particular virtual file in VFS
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val node = selected.getOrNull(0)?.node
    val nodeAttributes = selected.getOrNull(0)?.attributes
    e.presentation.isVisible = selected.size == 1 && node is FileLikeDatasetNode && nodeAttributes is RemoteMemberAttributes
  }

}
