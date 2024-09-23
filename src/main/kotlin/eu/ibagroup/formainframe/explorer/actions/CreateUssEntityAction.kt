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
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.common.ui.showUntilDone
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.fetch.UssQuery
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.dataops.operations.UssAllocationOperation
import eu.ibagroup.formainframe.dataops.operations.UssAllocationParams
import eu.ibagroup.formainframe.dataops.operations.UssChangeModeOperation
import eu.ibagroup.formainframe.dataops.operations.UssChangeModeParams
import eu.ibagroup.formainframe.explorer.ui.CreateFileDialog
import eu.ibagroup.formainframe.explorer.ui.CreateFileDialogState
import eu.ibagroup.formainframe.explorer.ui.ExplorerUnitTreeNodeBase
import eu.ibagroup.formainframe.explorer.ui.FileExplorerView
import eu.ibagroup.formainframe.explorer.ui.UssDirNode
import eu.ibagroup.formainframe.explorer.ui.UssFileNode
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import eu.ibagroup.formainframe.explorer.ui.toAllocationParams
import eu.ibagroup.formainframe.telemetry.NotificationsService
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import org.zowe.kotlinsdk.ChangeMode
import org.zowe.kotlinsdk.FileType

/**
 * Abstract action for creating Uss Entity (file or directory) through context menu.
 */
abstract class CreateUssEntityAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Uss file state which contains parameters for creating.
   */
  abstract val fileType: CreateFileDialogState

  /**
   * Uss file type (file or directory).
   */
  abstract val ussFileType: String

  /**
   * Called when create uss entity is chosen from context menu.
   * Parameters for creation are initialized depending on the entity type.
   * Runs uss allocation operation.
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: return
    val selected = view.mySelectedNodesData[0]
    val selectedNode = selected.node
    val project = e.project
    val node = if (selectedNode is UssFileNode) {
      selectedNode.parent as? UssDirNode
    } else {
      selectedNode as? UssDirNode
    } ?: return
    val file = node.virtualFile
    val connectionConfig = node.unit.connectionConfig.castOrNull<ConnectionConfig>() ?: return
    val dataOpsManager = DataOpsManager.getService()
    val filePath = if (file != null) {
      dataOpsManager.getAttributesService<RemoteUssAttributes, MFVirtualFile>()
        .getAttributes(file)?.path
    } else {
      node.value.path
    }
    if (filePath != null) {
      showUntilDone(
        initialState = fileType.apply { path = filePath },
        { initState -> CreateFileDialog(project, state = initState, filePath = filePath) }
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
          project = project,
          cancellable = true
        ) { indicator ->
          val ussDirNode = node.castOrNull<UssDirNode>()
          runCatching {
            dataOpsManager.performOperation(
              operation = UssAllocationOperation(
                request = allocationParams,
                connectionConfig = connectionConfig
              ),
              progressIndicator = indicator
            )

            val fileFetchProvider = dataOpsManager
              .getFileFetchProvider<UssQuery, RemoteQuery<ConnectionConfig, UssQuery, Unit>, MFVirtualFile>(
                UssQuery::class.java, RemoteQuery::class.java, MFVirtualFile::class.java
              )
            ussDirNode?.query?.let { query -> fileFetchProvider.reload(query) }

            changeFileModeIfNeeded(file, allocationParams, connectionConfig, indicator)

            AnalyticsService.getService().trackAnalyticsEvent(
              FileEvent(
                allocationParams.parameters.type,
                FileAction.CREATE
              )
            )
          }.onSuccess {
            ussDirNode?.cleanCache(false)
            res = true
          }.onFailure { t ->
            NotificationsService.getService().notifyError(t, project)
          }
        }
        res
      }
    }
  }

  /**
   * Changes the file mode if the wrong mode was specified when the file was created.
   */
  private fun changeFileModeIfNeeded(
    parentFile: VirtualFile?,
    params: UssAllocationParams,
    connectionConfig: ConnectionConfig,
    progressIndicator: ProgressIndicator
  ) {
    val dataOpsManager = DataOpsManager.getService()
    val fileName = params.fileName
    val createdFile = parentFile?.findChild(fileName)
    val attributes = createdFile?.let { vFile ->
      dataOpsManager.tryToGetAttributes(vFile)
    }.castOrNull<RemoteUssAttributes>()
    val fileMode = params.parameters.mode
    val filePath = params.path + "/" + params.fileName
    attributes?.let { attr ->
      if (attr.fileMode != fileMode) {
        dataOpsManager.performOperation(
          operation = UssChangeModeOperation(
            request = UssChangeModeParams(
              parameters = ChangeMode(mode = fileMode),
              path = filePath
            ),
            connectionConfig = connectionConfig
          ),
          progressIndicator = progressIndicator
        )
      }
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }

  /**
   * Makes action visible only if one node (uss file or uss directory) is selected.
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selectedNodes = view.mySelectedNodesData
    val node = selectedNodes.getOrNull(0)?.node
    e.presentation.isEnabledAndVisible = node is UssDirNode || node is UssFileNode

    if (node.castOrNull<ExplorerUnitTreeNodeBase<*, *, *>>()?.unit?.connectionConfig == null) {
      e.presentation.isEnabled = false
    }
  }
}
