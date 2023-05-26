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

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.*
import eu.ibagroup.formainframe.explorer.ui.FILE_EXPLORER_VIEW
import eu.ibagroup.formainframe.explorer.ui.FileExplorerView
import eu.ibagroup.formainframe.utils.sendTopic
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.XIBMDataType

/**
 * Base class implementation of the change content mode action
 */
class ChangeContentModeAction : ToggleAction() {

  override fun isSelected(e: AnActionEvent): Boolean {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return false
    return getMappedNodes(view)
      .mapNotNull {
        view.explorer.componentManager.service<DataOpsManager>()
          .getAttributesService(it.first::class.java, it.second::class.java)
          .getAttributes(it.second)
      }
      .all {
        it.contentMode == XIBMDataType(XIBMDataType.Type.BINARY)
      }
  }

  /**
   * Determines the scope of nodes(virtual files) based on the current selection to be passed in setSelected method
   * @param view represents a file explorer view object
   * @return list of pairs <attributes, virtualFile>
   */
  private fun getMappedNodes(view: FileExplorerView): List<Pair<FileAttributes, VirtualFile>> {
    return view.mySelectedNodesData
      .mapNotNull {
        val vFile = it.file
        if (vFile != null) {
          when (val attributes = service<DataOpsManager>().tryToGetAttributes(vFile)) {
            is RemoteDatasetAttributes -> {
              val isMigrated = attributes.isMigrated
              if (isMigrated) {
                return@mapNotNull null
              }
            }
          }
        }
        if (vFile?.isDirectory == true) {
          return@mapNotNull null
        }
        Pair(it.attributes ?: return@mapNotNull null, vFile ?: return@mapNotNull null)
      }
  }

  /**
   * Determines what nodes should be marked as selected in the context menu.
   * Selected means that content mode has been changed to binary for particular virtual file
   */
  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return
    if (showConfirmDialog(state) == Messages.CANCEL) {
      return
    } else {
      getMappedNodes(view)
        .forEach {
          val service = view.explorer.componentManager.service<DataOpsManager>()
            .getAttributesService(it.first::class.java, it.second::class.java)
          val vFile = it.second as MFVirtualFile
          when (val oldAttributes = service.getAttributes(vFile)) {
            is RemoteUssAttributes -> {
              val ussService = service as RemoteUssAttributesService
              val newAttributes = oldAttributes.apply {
                if (state) {
                  this.contentMode = XIBMDataType(XIBMDataType.Type.BINARY)
                } else {
                  this.contentMode = XIBMDataType(XIBMDataType.Type.TEXT)
                }
              }
              ussService.updateWritableFlagAfterContentChanged(vFile, newAttributes)
              sendTopic(AttributesService.FILE_CONTENT_CHANGED, DataOpsManager.instance.componentManager)
                .onUpdate(oldAttributes, newAttributes, vFile)
            }
            else -> {
              val newAttributes = oldAttributes.apply {
                if (state) {
                  this?.contentMode = XIBMDataType(XIBMDataType.Type.BINARY)
                  vFile.isWritable = false
                } else {
                  this?.contentMode = XIBMDataType(XIBMDataType.Type.TEXT)
                  vFile.isWritable = true
                }
              }
              if (newAttributes != null && oldAttributes != null) {
                service.updateAttributes(vFile, newAttributes)
                sendTopic(AttributesService.FILE_CONTENT_CHANGED, DataOpsManager.instance.componentManager)
                  .onUpdate(oldAttributes, newAttributes, vFile)
              }
            }
          }
        }
    }
    // TODO: For what ???? investigate
//        ApplicationManager.getApplication()
//          .messageBus
//          .syncPublisher(FileAttributesChangeListener.TOPIC)
//          .onFileAttributesChange(it.second)
  }

  /**
   * Shows a confirmation dialog when content mode is going to be changed
   * @param state represents selected content - binary or text
   */
  private fun showConfirmDialog(state: Boolean): Int {
    val mode = if (state) "binary" else "plain text"
    val confirmTemplate =
      "You are going to switch the file content to $mode. \n" +
              "The file content will be loaded from mainframe in $mode format. \n" +
              "Would you like to proceed?"
    return Messages.showOkCancelDialog(
      confirmTemplate,
      "Warning",
      "Ok",
      "Cancel",
      Messages.getWarningIcon()
    )
  }

  /**
   * Determines for which nodes the content mode action is visible in the context menu
   */
  override fun update(e: AnActionEvent) {
    super.update(e)
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    e.presentation.isEnabledAndVisible = getMappedNodes(view).isNotEmpty()
  }

  /**
   * This method is needed for interface implementation
   */
  override fun isDumbAware(): Boolean {
    return true
  }
}
