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
import com.intellij.openapi.ui.Messages
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.checkFileForSync
import eu.ibagroup.formainframe.dataops.operations.ForceRenameOperation
import eu.ibagroup.formainframe.dataops.operations.RenameOperation
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/**
 * Base class implementation of the force rename action
 */
class ForceRenameAction : AnAction() {

  /**
   * Called when force rename is chosen from context menu
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: return
    val selectedNode = view.mySelectedNodesData[0]
    if (selectedNode.node is UssDirNode || selectedNode.node is UssFileNode) {
      val attributes = selectedNode.attributes as RemoteUssAttributes
      val file = selectedNode.file as MFVirtualFile
      if (checkFileForSync(e.project, file, checkDependentFiles = true)) return
      val type = if (attributes.isDirectory) {
        "Directory"
      } else {
        "File"
      }
      val renameDialog = RenameDialog(e.project, type, selectedNode, this, attributes.name)
      if (renameDialog.showAndGet()) {
        val confirmDialog = showConfirmDialogIfNecessary(renameDialog.state, selectedNode)
        if (confirmDialog == Messages.OK) {
          runRenameOperation(e.project, view.explorer, file, attributes, renameDialog.state, selectedNode.node, true)
          service<AnalyticsService>().trackAnalyticsEvent(FileEvent(attributes, FileAction.FORCE_RENAME))
        } else if (confirmDialog != Messages.CANCEL) {
          runRenameOperation(e.project, view.explorer, file, attributes, renameDialog.state, selectedNode.node, false)
          service<AnalyticsService>().trackAnalyticsEvent(FileEvent(attributes, FileAction.RENAME))
        } else {
          return
        }
      }
    }
  }

  /**
   * Determines if a confirmation dialog should be displayed before sending a request to mainframe
   * @param text represents an existing file name in VFS with name conflict
   * @param selectedNode represents a virtual file object in VFS
   */
  private fun showConfirmDialogIfNecessary(text: String, selectedNode: NodeData<*>): Int {
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

  /**
   * Base method for running rename operation
   * @param project represents the current project
   * @param explorer represents explorer object
   * @param file represents a virtual file which is going to be renamed
   * @param attributes represents a current file attributes
   * @param newName a new name for the virtual file
   * @param node represents a current node object in explorer view
   * @param override responsible for the file override behavior in VFS
   */
  private fun runRenameOperation(
    project: Project?,
    explorer: Explorer<ConnectionConfig, *>,
    file: MFVirtualFile,
    attributes: FileAttributes,
    newName: String,
    node: ExplorerTreeNode<ConnectionConfig, *>,
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
      }
        .onSuccess {
          node.parent?.cleanCacheIfPossible(cleanBatchedQuery = true)
        }
        .onFailure {
          node.explorer.reportThrowable(it, project)
        }
    }
  }

  /**
   * Determines for which nodes the force rename action is visible in the context menu
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selectedNodes = view.mySelectedNodesData
    e.presentation.isEnabledAndVisible = if (selectedNodes.size == 1) {
      val node = selectedNodes[0].node
      (node is UssDirNode && !node.isUssMask) || node is UssFileNode
    } else {
      false
    }
    if (e.presentation.isEnabledAndVisible) {
      e.presentation.text = "Force Rename"
    }
  }

  /**
   * This method is needed for interface implementation
   */
  override fun isDumbAware(): Boolean {
    return true
  }
}
