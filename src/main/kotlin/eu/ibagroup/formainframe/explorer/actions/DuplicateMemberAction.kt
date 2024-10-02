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
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.checkFileForSync
import eu.ibagroup.formainframe.dataops.operations.RenameOperation
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.telemetry.NotificationsService

/**
 * Class which represents a duplicate member action
 */
class DuplicateMemberAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * The method of AnAction abstract class. Tells what to do if an action was submitted
   */
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val view = e.getExplorerView<FileExplorerView>() ?: return
    val selectedNode = view.mySelectedNodesData[0]
    val attributes = selectedNode.attributes ?: return
    val file = selectedNode.file ?: return
    if (checkFileForSync(e.project, file)) return
    val initialState = ""
    val dialog = RenameDialog(project, "Member", selectedNode, this, initialState)
    if (dialog.showAndGet()) {
      runDuplicateOperation(project, selectedNode, dialog.state)
      AnalyticsService.getService().trackAnalyticsEvent(FileEvent(attributes, FileAction.COPY))
    }
  }

  /**
   * Method to run duplicate operation. It passes the control to rename operation runner
   * @param project - current project
   * @param selectedNode - a current selected node
   * @param newName - a new name of the virtual file in VFS
   * @throws any throwable during the processing of the request
   * @return Void
   */
  private fun runDuplicateOperation(
    project: Project,
    selectedNode: NodeData<ConnectionConfig>,
    newName: String
  ) {
    val dataOpsManager = DataOpsManager.getService()
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
        NotificationsService.errorNotification(it, project)
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
    e.presentation.isVisible =
      selected.size == 1 && node is FileLikeDatasetNode && nodeAttributes is RemoteMemberAttributes
  }

}
