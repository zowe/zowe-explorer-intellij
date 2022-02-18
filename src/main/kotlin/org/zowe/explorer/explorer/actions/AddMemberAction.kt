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
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.ui.Messages
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.getAttributesService
import org.zowe.explorer.dataops.operations.MemberAllocationOperation
import org.zowe.explorer.dataops.operations.MemberAllocationParams
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.utils.service
import org.zowe.explorer.vfs.MFVirtualFile

class AddMemberAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return
    val currentNode = view.mySelectedNodesData[0].node
    if (currentNode is ExplorerUnitTreeNodeBase<*, *>
      && currentNode.unit is FilesWorkingSet
      && currentNode is FetchNode
    ) {
      val connectionConfig = currentNode.unit.connectionConfig
      val dataOpsManager = currentNode.explorer.componentManager.service<DataOpsManager>()
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
              }.onSuccess { currentNode.cleanCache() }
                .onFailure {
                  runInEdt {
                    Messages.showErrorDialog(
                      "Cannot create member ${state.memberName} ${state.datasetName} on ${connectionConfig.name}",
                      "Cannot Allocate Dataset"
                    )
                  }
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

  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData.getOrNull(0)
    e.presentation.isEnabledAndVisible = selected?.node is LibraryNode || (
      selected?.node is FileLikeDatasetNode && selected.attributes is RemoteMemberAttributes
      )
  }

}
