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

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.runBackgroundableTask
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.getAttributesService
import org.zowe.explorer.dataops.operations.DeleteMemberOperation
import org.zowe.explorer.dataops.operations.DeleteMemberOperationParams
import org.zowe.explorer.dataops.operations.MemberAllocationOperation
import org.zowe.explorer.dataops.operations.MemberAllocationParams
import org.zowe.explorer.explorer.ExplorerUnit
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.AddMemberDialog
import org.zowe.explorer.explorer.ui.ExplorerUnitTreeNodeBase
import org.zowe.explorer.explorer.ui.FetchNode
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.FileLikeDatasetNode
import org.zowe.explorer.explorer.ui.LibraryNode
import org.zowe.explorer.explorer.ui.getExplorerView
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.vfs.MFVirtualFile

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
                dataOpsManager.performOperation(
                  operation = MemberAllocationOperation(
                    connectionConfig = connectionConfig,
                    request = state
                  ),
                  progressIndicator = it
                )
              }.onSuccess {
                currentNode.cleanCache(cleanBatchedQuery = true)
              }.onFailure { failObj: Throwable ->
                var throwable = failObj
                if (failObj is CallException && failObj.code == 500 && failObj.message?.contains("Directory full") == true) {
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
                  }.onFailure { innerFailObj: Throwable ->
                    throwable = Throwable("Directory is FULL. Invalid member created.\n" + innerFailObj.message)
                    currentNode.cleanCache(cleanBatchedQuery = true)
                  }
                }
                NotificationsService.getService().notifyError(throwable, e.project)
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
    e.presentation.isEnabledAndVisible = selected?.node is LibraryNode || (
      selected?.node is FileLikeDatasetNode && selected.attributes is RemoteMemberAttributes
      )
  }

}
