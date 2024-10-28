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
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.common.ui.showUntilDone
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.fetch.UssQuery
import org.zowe.explorer.dataops.getAttributesService
import org.zowe.explorer.dataops.operations.UssAllocationOperation
import org.zowe.explorer.dataops.operations.UssAllocationParams
import org.zowe.explorer.dataops.operations.UssChangeModeOperation
import org.zowe.explorer.dataops.operations.UssChangeModeParams
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.utils.service // TODO: remove in v1.*.*-223 and greater
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.ChangeMode
import org.zowe.kotlinsdk.FileModeValue
import org.zowe.kotlinsdk.FileType

/**
 * Abstract action for creating Uss Entity (file or directory) through context menu.
 */
abstract class CreateUssEntityAction : AnAction() {

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

    val node: UssDirNode = if (selectedNode is UssFileNode) {
      selectedNode.parent as? UssDirNode
    } else {
      selectedNode as UssDirNode
    } ?: return

    val connectionConfig = node.unit.connectionConfig ?: return
    val dataOpsManager = DataOpsManager.instance

    val file = node.virtualFile
    val attributes = file?.let {
      dataOpsManager.getAttributesService<RemoteUssAttributes, MFVirtualFile>()
        .getAttributes(it)
    }

    val filePath = attributes?.path ?: node.value.path

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
      ) { indicator ->
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

          attributes?.fileMode?.let { fm ->
            if (checkReadPermissionsBeforeReload(fm.owner)) {
              node.query?.let { query -> fileFetchProvider.reload(query) }
            }
          }

          changeFileModeIfNeeded(file, allocationParams, connectionConfig, indicator)
        }.onSuccess {
          attributes?.fileMode?.let { fm ->
            if (checkReadPermissionsBeforeReload(fm.owner)) {
              node.cleanCache(false)
            }
          }
          res = true
        }.onFailure { t ->
          view.explorer.reportThrowable(t, e.project)
        }
      }
      res
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
    val dataOpsManager = service<DataOpsManager>()
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

  private fun checkReadPermissionsBeforeReload(permission: Int): Boolean {
    return permission == FileModeValue.READ.mode ||
      permission == FileModeValue.READ_EXECUTE.mode ||
      permission == FileModeValue.READ_WRITE.mode ||
      permission == FileModeValue.READ_WRITE_EXECUTE.mode
  }
}
