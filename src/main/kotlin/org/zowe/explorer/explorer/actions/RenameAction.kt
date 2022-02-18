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
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.config.configCrudable
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.operations.RenameOperation
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.utils.*
import org.zowe.explorer.utils.crudable.getByUniqueKey

class RenameAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return
    val selectedNode = view.mySelectedNodesData[0]
    val node = selectedNode.node
    var initialState = ""
    if (node is DSMaskNode) {
      initialState = (selectedNode.node.value as DSMask).mask
      val dialog = RenameDialog(e.project, "Dataset Mask", initialState).withValidationOnInput {
        validateDatasetMask(it.text, it)
      }.withValidationForBlankOnApply()
      if (dialog.showAndGet()) {
        val parentValue = selectedNode.node.parent?.value as FilesWorkingSet
        val wsToUpdate = configCrudable.getByUniqueKey<FilesWorkingSetConfig>(parentValue.uuid)?.clone()
        if (wsToUpdate != null) {
          wsToUpdate.dsMasks.filter { it.mask == initialState }[0].mask = dialog.state
          configCrudable.update(wsToUpdate)
        }
      }
    } else if (node is LibraryNode || node is FileLikeDatasetNode) {
      val attributes = selectedNode.attributes ?: return
      var type = ""
      if (attributes is RemoteDatasetAttributes) {
        initialState = attributes.datasetInfo.name
        type = "Dataset"
      } else if (attributes is RemoteMemberAttributes) {
        initialState = attributes.info.name
        type = "Member"
      }
      val dialog = RenameDialog(e.project, type, initialState).withValidationOnInput {
        if (attributes is RemoteDatasetAttributes) {
          validateDatasetNameOnInput(it)
        } else {
          validateMemberName(it)
        }
      }.withValidationForBlankOnApply()
      val file = node.virtualFile
      if (dialog.showAndGet() && file != null) {
        runRenameOperation(e.project, file, attributes, dialog.state, node)
      }
    } else if (selectedNode.node is UssDirNode && selectedNode.node.isConfigUssPath) {
      initialState = selectedNode.node.value.path
      val dialog = RenameDialog(e.project, "Directory", initialState).withValidationOnInput {
        validateUssMask(it.text, it)
      }.withValidationForBlankOnApply()
      if (dialog.showAndGet()) {
        val parentValue = selectedNode.node.parent?.value as FilesWorkingSet
        val wsToUpdate = configCrudable.getByUniqueKey<FilesWorkingSetConfig>(parentValue.uuid)?.clone()
        if (wsToUpdate != null) {
          wsToUpdate.ussPaths.filter { it.path == initialState }[0].path = dialog.state
          configCrudable.update(wsToUpdate)
        }
      }

    } else if (selectedNode.node is UssDirNode || selectedNode.node is UssFileNode) {
      val attributes = selectedNode.attributes as RemoteUssAttributes
      val file = selectedNode.file
      val dialog = RenameDialog(
        e.project,
        if (attributes.isDirectory) "Directory" else "File",
        attributes.name
      ).withValidationOnInput {
        validateUssFileName(it)
        validateUssFileNameAlreadyExists(it, selectedNode)
      }.withValidationForBlankOnApply()
      if (dialog.showAndGet() && file != null) {
        runRenameOperation(e.project, file, attributes, dialog.state, node)
      }
    }
  }

  private fun runRenameOperation(
    project: Project?,
    file: VirtualFile,
    attributes: FileAttributes,
    newName: String,
    node: ExplorerTreeNode<*>
  ) {
    runBackgroundableTask(
      title = "Renaming file ${file.name} to $newName",
      project = project,
      cancellable = true
    ) {
      runCatching {
        node.explorer.componentManager.service<DataOpsManager>().performOperation(
          operation = RenameOperation(
            file = file,
            attributes = attributes,
            newName = newName
          ),
          progressIndicator = it
        )
      }.onSuccess {
        node.parent?.cleanCacheIfPossible()
      }.onFailure {
        node.explorer.reportThrowable(it, project)
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selectedNodes = view.mySelectedNodesData
    e.presentation.isEnabledAndVisible = if (selectedNodes.size == 1 && selectedNodes[0].node !is FilesWorkingSetNode) {
      val file = selectedNodes[0].file
      var isMigrated = false
      if (file != null) {
        val attributes = service<DataOpsManager>().tryToGetAttributes(file) as? RemoteDatasetAttributes
        isMigrated = attributes?.isMigrated ?: false
      }
      !isMigrated
    } else {
      false
    }
    if (e.presentation.isEnabledAndVisible) {
      val selectedNode = selectedNodes[0]
      if (selectedNode.node is DSMaskNode || (selectedNode.node is UssDirNode && selectedNode.node.isConfigUssPath)) {
        e.presentation.text = "Edit"
      } else {
        e.presentation.text = "Rename"
      }
    }
  }


  override fun isDumbAware(): Boolean {
    return true
  }
}
