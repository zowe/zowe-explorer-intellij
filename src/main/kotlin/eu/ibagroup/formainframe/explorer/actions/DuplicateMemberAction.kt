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

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.operations.RenameOperation
import eu.ibagroup.formainframe.explorer.ui.*

class DuplicateMemberAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return
    val selectedNode = view.mySelectedNodesData[0]
    val attributes = selectedNode.attributes ?: return
    val initialState = ""
    val dialog = RenameDialog(project, "Member", selectedNode, this, initialState)
    if (dialog.showAndGet()) {
      runDuplicateOperation(project, view, selectedNode, dialog.state)
      service<AnalyticsService>().trackAnalyticsEvent(FileEvent(attributes, FileAction.COPY))
    }
  }

  private fun runDuplicateOperation(project : Project, view : ExplorerTreeView<*,*>, selectedNode : NodeData, newName: String) {
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

  override fun isDumbAware(): Boolean {
    return true
  }

  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val node = selected.getOrNull(0)?.node
    val nodeAttributes = selected.getOrNull(0)?.attributes
    e.presentation.isVisible = selected.size == 1 && node is FileLikeDatasetNode && nodeAttributes is RemoteMemberAttributes
  }

}
