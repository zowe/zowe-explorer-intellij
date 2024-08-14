/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.content.synchronizer

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.content.service.isFileSyncingNow
import eu.ibagroup.formainframe.utils.*

/** Sync action event. It will handle the manual sync button action when it is clicked */
class SyncAction : DumbAwareAction() {
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Get a virtual file on which the event was triggered
   * @param e the event to get the virtual file
   */
  private fun getVirtualFile(e: AnActionEvent): VirtualFile? {
    return e.getData(CommonDataKeys.VIRTUAL_FILE)
  }

  /**
   * Get a virtual file with the sync support on which the event was triggered
   * @param e the event to get the virtual file
   */
  private fun getSupportedVirtualFile(e: AnActionEvent): VirtualFile? {
    return getVirtualFile(e)?.let {
      if (service<DataOpsManager>().isSyncSupported(it)) {
        it
      } else {
        null
      }
    }
  }

  /**
   * Get an editor on which the event was triggered
   * @param e the event to get the editor
   */
  private fun getEditor(e: AnActionEvent): EditorEx? {
    return e.getData(CommonDataKeys.EDITOR).castOrNull()
  }

  /**
   * Perform the manual sync action. The action will be performed as a backgroundable task.
   * After the content of the file is synced with the mainframe, the document of the file will be saved
   * @param e the event instance to get the virtual file, the editor and the project where it was triggered
   */
  override fun actionPerformed(e: AnActionEvent) {
    val vFile = getSupportedVirtualFile(e) ?: return
    val incompatibleEncoding = e.project?.let { !checkEncodingCompatibility(vFile, it) } ?: false
    if (incompatibleEncoding && !showSaveAnywayDialog(vFile.charset)) return
    val syncProvider = DocumentedSyncProvider(vFile, SaveStrategy.default(e.project))
    runBackgroundableTask(
      title = "Synchronizing ${vFile.name}...",
      project = e.project,
      cancellable = true
    ) { indicator ->
      runInEdtAndWait { syncProvider.saveDocument() }
      service<DataOpsManager>().getContentSynchronizer(vFile)?.synchronizeWithRemote(syncProvider, indicator)
    }
  }

  /**
   * Make the sync action button disabled
   * @param e the action event to get the presentation of the button
   */
  private fun makeDisabled(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
  }

  /**
   * Disable or enable the sync action button according to the current editor and remote bytes' equality.
   * The button will be disabled also when the auto sync is enabled
   * @param e the action event to get additional data to check and disable the button
   */
  override fun update(e: AnActionEvent) {
    val file = getSupportedVirtualFile(e) ?: let {
      makeDisabled(e)
      return
    }
    val editor = getEditor(e) ?: return

    val contentSynchronizer = service<DataOpsManager>().getContentSynchronizer(file)
    val syncProvider = DocumentedSyncProvider(file)
    val currentContent = runReadAction { syncProvider.retrieveCurrentContent() }
    val previousContent = contentSynchronizer?.successfulContentStorage(syncProvider)
    val needToUpload = contentSynchronizer?.isFileUploadNeeded(syncProvider) == true
    e.presentation.isEnabledAndVisible = file.isWritable
        && !service<ConfigService>().isAutoSyncEnabled
        && !(currentContent contentEquals previousContent)
        && needToUpload
        && !isFileSyncingNow(file)
  }

  /**
   * Determines if an action is dumb aware or not
   */
  override fun isDumbAware(): Boolean {
    return true
  }
}
