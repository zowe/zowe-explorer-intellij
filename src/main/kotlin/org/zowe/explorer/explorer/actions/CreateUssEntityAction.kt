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
import com.intellij.openapi.progress.runModalTask
import org.zowe.explorer.analytics.AnalyticsService
import org.zowe.explorer.analytics.events.FileAction
import org.zowe.explorer.analytics.events.FileEvent
import org.zowe.explorer.common.ui.showUntilDone
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.getAttributesService
import org.zowe.explorer.dataops.operations.UssAllocationOperation
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.utils.service
import org.zowe.explorer.vfs.MFVirtualFile
import eu.ibagroup.r2z.FileType

abstract class CreateUssEntityAction : AnAction() {

  abstract val fileType: CreateFileDialogState
  abstract val ussFileType: String

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return
    val selected = view.mySelectedNodesData[0]
    val selectedNode = selected.node
    val node = if (selectedNode is UssFileNode) {
      selectedNode.parent?.takeIf { it is UssDirNode }
    } else {
      selectedNode.takeIf { it is UssDirNode }
    } ?: return
    val file = node.virtualFile
    if (node is ExplorerUnitTreeNodeBase<*, *>) {
      val connectionConfig = node.unit.connectionConfig ?: return
      val dataOpsManager = node.unit.explorer.componentManager.service<DataOpsManager>()
      val filePath = if (file != null) {
        dataOpsManager.getAttributesService<RemoteUssAttributes, MFVirtualFile>()
          .getAttributes(file)?.path
      } else {
        (node as UssDirNode).value.path
      }
      if (filePath != null) {
        showUntilDone(
          initialState = fileType.apply { path = filePath },
          { initState -> CreateFileDialog(e.project, state = initState, filePath = filePath) }
        ) {
          var res = false
          val allocationParams = it.toAllocationParams()
          val fileType = if (allocationParams.parameters.type == FileType.FILE) {
            "File"
          } else {
            "Directory"
          }
          runModalTask(
            title = "Creating $fileType ${allocationParams.fileName}",
            project = e.project,
            cancellable = true
          ) {
            runCatching {
              dataOpsManager.performOperation(
                operation = UssAllocationOperation(
                  request = allocationParams,
                  connectionConfig = connectionConfig
                ),
                progressIndicator = it
              )
              val analyticsFileType = if (allocationParams.parameters.type == FileType.FILE)
                org.zowe.explorer.analytics.events.FileType.USS_FILE
              else org.zowe.explorer.analytics.events.FileType.USS_DIR

              service<AnalyticsService>().trackAnalyticsEvent(
                FileEvent(
                  allocationParams.parameters.type,
                  FileAction.CREATE
                )
              )
            }.onSuccess {
              node.castOrNull<UssDirNode>()?.cleanCache(false)
              res = true
            }.onFailure { t ->
              view.explorer.reportThrowable(t, e.project)
            }
          }
          res
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
    e.presentation.isEnabledAndVisible =
      selected.size == 1 && (selected[0].node is UssDirNode || selected[0].node is UssFileNode)
  }
}
