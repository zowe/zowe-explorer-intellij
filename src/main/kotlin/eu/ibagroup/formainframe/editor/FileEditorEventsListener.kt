/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.editor

import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.content.service.isFileSyncingNow
import eu.ibagroup.formainframe.dataops.content.synchronizer.*
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/**
 * File editor events listener.
 * Needed to handle file editor events
 */
class FileEditorEventsListener : FileEditorManagerListener {

  private val focusListener = FileEditorFocusListener()

  /**
   * Adds the file editor focus listener [FileEditorFocusListener] after the file is opened.
   * @param source the source file editor manager to get the editor in which the file is open.
   * @param file the file that was opened.
   */
  override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
    if (file is MFVirtualFile) {
      val editor = source.selectedTextEditor as? EditorEx
      editor?.addFocusListener(focusListener)
    }
    super.fileOpened(source, file)
  }
}

/**
 * File editor before events listener.
 * Needed to handle before file editor events
 */
class FileEditorBeforeEventsListener : FileEditorManagerListener.Before {

  private val dataOpsManager = service<DataOpsManager>()

  /**
   * Handle synchronize before close if the file is not synchronized
   * @param source the source file editor manager to get the project where the file is being edited
   * @param file the file to be checked and synchronized
   */
  override fun beforeFileClosed(source: FileEditorManager, file: VirtualFile) {
    val project = source.project
    val configService = service<ConfigService>()
    val syncProvider = DocumentedSyncProvider(file, SaveStrategy.default(project))
    val attributes = dataOpsManager.tryToGetAttributes(file)
    if (file is MFVirtualFile && file.isWritable && attributes != null) {
      val contentSynchronizer = service<DataOpsManager>().getContentSynchronizer(file)
      val currentContent = runReadAction { syncProvider.retrieveCurrentContent() }
      val previousContent = contentSynchronizer?.successfulContentStorage(syncProvider)
      val needToUpload = contentSynchronizer?.isFileUploadNeeded(syncProvider) == true
      if (!(currentContent contentEquals previousContent) && needToUpload && !isFileSyncingNow(file)) {
        val incompatibleEncoding = !checkEncodingCompatibility(file, project)
        if (!configService.isAutoSyncEnabled) {
          if (showSyncOnCloseDialog(file.name, project)) {
            if (incompatibleEncoding && !showSaveAnywayDialog(file.charset)) {
              return
            }
            runModalTask(
              title = "Syncing ${file.name}",
              project = project,
              cancellable = true
            ) {
              runInEdtAndWait { syncProvider.saveDocument() }
              contentSynchronizer?.synchronizeWithRemote(syncProvider, it)
            }
          }
        } else {
          if (incompatibleEncoding && !showSaveAnywayDialog(file.charset)) {
            return
          }
          sendTopic(AutoSyncFileListener.AUTO_SYNC_FILE, project).sync(file)
        }
      }
    }
    super.beforeFileClosed(source, file)
  }

  /**
   * Handle before file open event
   * @param source the source file editor manager to get the project where the file is being edited
   * @param file the file that opens
   */
  override fun beforeFileOpened(source: FileEditorManager, file: VirtualFile) {
    if (file is MFVirtualFile) {
      putUserDataInFile(file)
    }
    super.beforeFileOpened(source, file)
  }
}
