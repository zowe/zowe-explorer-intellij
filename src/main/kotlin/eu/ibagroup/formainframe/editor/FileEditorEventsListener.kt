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

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.content.synchronizer.*
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/**
 * File editor events listener.
 * Needed to handle before file events
 */
class FileEditorEventsListener : FileEditorManagerListener.Before {

  private val dataOpsManager = service<DataOpsManager>()

  /**
   * Handle synchronize before close if the file is not synchronized
   * @param source the source file editor manager to get the project where the file is being edited
   * @param file the file to be checked and synchronized
   */
  override fun beforeFileClosed(source: FileEditorManager, file: VirtualFile) {
    val configService = service<ConfigService>()
    val syncProvider = DocumentedSyncProvider(file, SaveStrategy.default(source.project))
    if (!configService.isAutoSyncEnabled) {
      val attributes = dataOpsManager.tryToGetAttributes(file)
      if (file is MFVirtualFile && file.isWritable && attributes != null) {
        val contentSynchronizer = service<DataOpsManager>().getContentSynchronizer(file)
        val currentContent = runReadActionInEdtAndWait { syncProvider.retrieveCurrentContent() }
        val previousContent = contentSynchronizer?.successfulContentStorage(syncProvider)
        val needToUpload = contentSynchronizer?.isFileUploadNeeded(syncProvider) == true
        if (!(currentContent contentEquals previousContent) && needToUpload) {
          if (showSyncOnCloseDialog(file.name, source.project)) {
            runModalTask(
              title = "Syncing ${file.name}",
              project = source.project,
              cancellable = true
            ) {
              runWriteActionInEdtAndWait {
                syncProvider.saveDocument()
                contentSynchronizer?.synchronizeWithRemote(syncProvider, it)
              }
            }
          }
        }
      }
    } else {
      syncProvider.saveDocument()
    }
    super.beforeFileClosed(source, file)
  }

  /**
   * Handle before file open event
   * @param source the source file editor manager to get the project where the file is being edited
   * @param file the file that opens
   */
  override fun beforeFileOpened(source: FileEditorManager, file: VirtualFile) {
    file.putUserData()
    super.beforeFileOpened(source, file)
  }
}
