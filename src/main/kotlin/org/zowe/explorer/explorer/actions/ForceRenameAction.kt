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
import com.intellij.openapi.ui.Messages
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.operations.ForceRenameOperation
import org.zowe.explorer.dataops.operations.RenameOperation
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.utils.validateUssFileName
import org.zowe.explorer.vfs.MFVirtualFile

class ForceRenameAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return
    val selectedNode = view.mySelectedNodesData[0]
    if (selectedNode.node is UssDirNode || selectedNode.node is UssFileNode) {
      val attributes = selectedNode.attributes as RemoteUssAttributes
      val file = selectedNode.file as MFVirtualFile
      val renameDialog = RenameDialog(e.project, if (attributes.isDirectory) "Directory" else "File", attributes.name)
        .withValidationOnInput { validateUssFileName(it) }
        .withValidationForBlankOnApply()
      if (renameDialog.showAndGet() && file != null) {
        val confirmDialog = showConfirmDialogIfNecessary(renameDialog.state, selectedNode)
        if (confirmDialog == Messages.OK) {
          runRenameOperation(e.project, view.explorer, file, attributes, renameDialog.state, selectedNode.node, true)
        } else {
          if (confirmDialog != Messages.CANCEL && confirmDialog != Messages.OK) {
            runRenameOperation(e.project, view.explorer, file, attributes, renameDialog.state, selectedNode.node, false)
          } else {
            return
          }
        }
      }
    }
  }

  private fun showConfirmDialogIfNecessary(text: String, selectedNode: NodeData): Int {
    val childrenNodesFromParent = selectedNode.node.parent?.children
    val virtualFilePath = selectedNode.node.virtualFile?.canonicalPath
    when (selectedNode.node) {
      is UssFileNode -> {
        childrenNodesFromParent?.forEach {
          if (it is UssFileNode && it.value.filenameInternal == text) {
            val confirmTemplate =
              "You are going to rename file $virtualFilePath \n" +
                  "into existing one. This operation cannot be undone. \n" +
                  "Would you like to proceed?"
            return Messages.showOkCancelDialog(
              confirmTemplate,
              "Warning",
              "Ok",
              "Cancel",
              Messages.getWarningIcon()
            )
          }
        }
      }
      is UssDirNode -> {
        childrenNodesFromParent?.forEach {
          if (it is UssDirNode && text == it.value.path.split("/").last()) {
            val confirmTemplate =
              "You are going to rename directory $virtualFilePath \n"
                .plus("into existing one. This operation cannot be undone. \n".plus("Would you like to proceed?"))
            return Messages.showOkCancelDialog(
              confirmTemplate,
              "Warning",
              "Ok",
              "Cancel",
              Messages.getWarningIcon()
            )
          }
        }
      }
    }
    return Messages.NO
  }

  private fun runRenameOperation(
    project: Project?,
    explorer: Explorer<*>,
    file: MFVirtualFile,
    attributes: FileAttributes,
    newName: String,
    node: ExplorerTreeNode<*>,
    override: Boolean
  ) {
    runBackgroundableTask(
      title = "Renaming file ${file.name} to $newName",
      project = project,
      cancellable = true
    ) {
      runCatching {
        if (override) {
          service<DataOpsManager>().performOperation(
            operation = ForceRenameOperation(
              file = file,
              attributes = attributes,
              newName = newName,
              override = override,
              explorer = explorer
            ),
            progressIndicator = it
          )
        } else {
          service<DataOpsManager>().performOperation(
            operation = RenameOperation(
              file = file,
              attributes = attributes,
              newName = newName
            ),
            progressIndicator = it
          )
        }
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
    val node = selectedNodes[0].node
    e.presentation.isEnabledAndVisible =
      selectedNodes.size == 1 && ((node is UssDirNode && !node.isConfigUssPath) || node is UssFileNode)
    if (e.presentation.isEnabledAndVisible) {
      e.presentation.text = "Force Rename"
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}
