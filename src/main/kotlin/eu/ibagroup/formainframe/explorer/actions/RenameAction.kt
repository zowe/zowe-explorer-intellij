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
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.operations.RenameOperation
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey

/**
 * Class which represents a rename action
 */
class RenameAction : AnAction() {

  /**
   * Overloaded method of AnAction abstract class. Tells what to do if an action was submitted
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return
    val selectedNode = view.mySelectedNodesData[0]
    val node = selectedNode.node
    var initialState = ""
    if (node is DSMaskNode) {
      initialState = (selectedNode.node.value as DSMask).mask
      val dialog = RenameDialog(e.project, "Dataset Mask", selectedNode, this, initialState)
      if (dialog.showAndGet()) {
        val parentValue = selectedNode.node.parent?.value as FilesWorkingSet
        val wsToUpdate = configCrudable.getByUniqueKey<FilesWorkingSetConfig>(parentValue.uuid)?.clone()
        if (wsToUpdate != null) {
          wsToUpdate.dsMasks.filter { it.mask == initialState }[0].mask = dialog.state.uppercase()
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
      val dialog = RenameDialog(e.project, type, selectedNode, this, initialState)
      val file = node.virtualFile
      if (dialog.showAndGet() && file != null) {
        runRenameOperation(e.project, file, attributes, dialog.state, node)
        service<AnalyticsService>().trackAnalyticsEvent(FileEvent(attributes, FileAction.RENAME))
      }
    } else if (selectedNode.node is UssDirNode && selectedNode.node.isConfigUssPath) {
      initialState = selectedNode.node.value.path
      val dialog = RenameDialog(e.project, "USS Mask", selectedNode, this, initialState)
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
        selectedNode,
        this,
        attributes.name
      )
      if (dialog.showAndGet() && file != null) {
        runRenameOperation(e.project, file, attributes, dialog.state, node)
        service<AnalyticsService>().trackAnalyticsEvent(FileEvent(attributes, FileAction.RENAME))
      }
    }
  }

  /**
   * Method to run rename operation. It passes the control to rename operation runner
   * @param project - current project
   * @param file - a virtual file to be renamed
   * @param attributes - remote attributes of the given virtual file
   * @param newName - a new name of the virtual file in VFS
   * @param node - an instance of explorer unit
   * @throws any throwable during the processing of the request
   * @return Void
   */
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
        node.parent?.cleanCacheIfPossible(cleanBatchedQuery = true)
      }.onFailure {
        node.explorer.reportThrowable(it, project)
      }
    }
  }

  /**
   * Method determines if an action is visible for particular virtual file in VFS
   */
  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selectedNodes = view.mySelectedNodesData
    e.presentation.isEnabledAndVisible = if (selectedNodes.size == 1) {
      val node = selectedNodes[0].node
      if (node is FilesWorkingSetNode || node is LoadingNode || node is LoadMoreNode) {
        false
      } else {
        val file = selectedNodes[0].file
        var isMigrated = false
        if (file != null) {
          val attributes = service<DataOpsManager>().tryToGetAttributes(file) as? RemoteDatasetAttributes
          isMigrated = attributes?.isMigrated ?: false
        }
        !isMigrated
      }
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

  /**
   * Determines if an action is dumb aware
   */
  override fun isDumbAware(): Boolean {
    return true
  }
}
