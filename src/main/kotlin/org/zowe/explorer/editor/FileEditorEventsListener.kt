/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.editor

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.content.synchronizer.DocumentedSyncProvider
import org.zowe.explorer.dataops.content.synchronizer.SaveStrategy
import org.zowe.explorer.utils.log
import org.zowe.explorer.vfs.MFVirtualFile

private val log = log<FileEditorEventsListener>()

/**
 * File editor events listener.
 * Needed to handle file close event
 */
class FileEditorEventsListener : FileEditorManagerListener.Before {

  /**
   * Handle synchronize before close if the file is not synchronized
   * @param source the source file editor manager to get the project where the file is being edited
   * @param file the file to be checked and synchronized
   */
  override fun beforeFileClosed(source: FileEditorManager, file: VirtualFile) {
    val configService = service<ConfigService>()
    val dataOpsManager = service<DataOpsManager>()
    val attributes = dataOpsManager.tryToGetAttributes(file)
    if (file is MFVirtualFile && !configService.isAutoSyncEnabled && file.isWritable &&
      attributes != null) {
      val document = FileDocumentManager.getInstance().getDocument(file) ?: let {
        log.info("Document cannot be used here")
        return
      }
      if (
        !(document.text.toByteArray() contentEquals file.contentsToByteArray()) &&
        showSyncOnCloseDialog(file.name, source.project)
      ) {
        runModalTask(
          title = "Syncing ${file.name}",
          project = source.project,
          cancellable = true
        ) {
          runInEdt {
            FileDocumentManager.getInstance().saveDocument(document)
            val syncProvider = DocumentedSyncProvider(file, SaveStrategy.default(source.project))
            service<DataOpsManager>().getContentSynchronizer(file)?.synchronizeWithRemote(syncProvider, it)
          }
        }
      }
    }
    super.beforeFileClosed(source, file)
  }
}
