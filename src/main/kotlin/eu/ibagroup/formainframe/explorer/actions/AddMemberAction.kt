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
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.analytics.events.FileType
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.dataops.operations.DeleteMemberOperation
import eu.ibagroup.formainframe.dataops.operations.DeleteMemberOperationParams
import eu.ibagroup.formainframe.dataops.operations.MemberAllocationOperation
import eu.ibagroup.formainframe.dataops.operations.MemberAllocationParams
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.telemetry.NotificationsService
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/** Class that represents "Add member" action */
class AddMemberAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Create a new member in the dataset library
   * @param e an action event to get the file explorer view and the project
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: return
    var currentNode = view.mySelectedNodesData[0].node
    if (currentNode !is FetchNode) {
      currentNode = currentNode.parent ?: return
      if (currentNode !is LibraryNode) return
    }
    if ((currentNode as ExplorerUnitTreeNodeBase<ConnectionConfig, *, out ExplorerUnit<ConnectionConfig>>).unit is FilesWorkingSet) {
      val connectionConfig = currentNode.unit.connectionConfig
      val dataOpsManager = DataOpsManager.getService()
      if (currentNode is LibraryNode && connectionConfig != null) {
        val parentName = dataOpsManager
          .getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
          .getAttributes(currentNode.virtualFile)
          ?.name
        if (parentName != null) {
          val dialog = AddMemberDialog(e.project, MemberAllocationParams(datasetName = parentName))
          if (dialog.showAndGet()) {
            val state = dialog.state
            runBackgroundableTask(
              title = "Allocating member ${state.memberName}",
              project = e.project,
              cancellable = true
            ) {
              runCatching {
                AnalyticsService.getService().trackAnalyticsEvent(FileEvent(FileType.MEMBER, FileAction.CREATE))
                dataOpsManager.performOperation(
                  operation = MemberAllocationOperation(
                    connectionConfig = connectionConfig,
                    request = state
                  ),
                  progressIndicator = it
                )
              }.onSuccess {
                currentNode.cleanCache(cleanBatchedQuery = true)
              }.onFailure {
                var throwable = it
                if (it is CallException && it.code == 500 && it.message?.contains("Directory full") == true) {
                  runCatching {
                    dataOpsManager.performOperation(
                      operation = DeleteMemberOperation(
                        request = DeleteMemberOperationParams(
                          datasetName = state.datasetName,
                          memberName = state.memberName
                        ),
                        connectionConfig = connectionConfig
                      )
                    )
                  }.onFailure { th ->
                    throwable = Throwable("Directory is FULL. Invalid member created.\n" + th.message)
                    currentNode.cleanCache(cleanBatchedQuery = true)
                  }
                }
                NotificationsService.errorNotification(throwable, e.project)
              }
            }
          }
        }
      }
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }

  /**
   * Show the action only for those places, where a member creation is possible
   * @param e an action event to get the presentation so show and the file explorer view
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData.getOrNull(0)
    e.presentation.isEnabledAndVisible = selected?.node is LibraryNode
      || (selected?.node is FileLikeDatasetNode && selected.attributes is RemoteMemberAttributes)
  }

}
