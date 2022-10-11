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
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.*
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.vfs.MFVirtualFile

private val log = log<FileEditorEventsListener>()

/**
 * File editor events listener.
 * Needed to handle file close event
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
    val attributes = dataOpsManager.tryToGetAttributes(file)
    if (file is MFVirtualFile && !configService.isAutoSyncEnabled && file.isWritable && attributes != null) {
      val document = FileDocumentManager.getInstance().getDocument(file) ?: let {
        log.info("Document cannot be used here")
        return
      }

      val ussAttributes = attributes.castOrNull<RemoteUssAttributes>()
      val syncProvider = DocumentedSyncProvider(file, SaveStrategy.default(source.project))
      val currentContent = runReadActionInEdtAndWait { syncProvider.retrieveCurrentContent() }
      if (!(currentContent contentEquals file.contentsToByteArray()) || ussAttributes?.encodingChanged == true) {
        if (showSyncOnCloseDialog(file.name, source.project)) {
          runModalTask(
            title = "Syncing ${file.name}",
            project = source.project,
            cancellable = true
          ) {
            runWriteActionInEdtAndWait {
              FileDocumentManager.getInstance().saveDocument(document)
              service<DataOpsManager>().getContentSynchronizer(file)?.synchronizeWithRemote(syncProvider, it)
            }
          }
        }
      }
    }
    super.beforeFileClosed(source, file)
  }

  /**
   * Called every time before opening the file in the editor
   * @param source the source file editor manager to get the project where the file is being edited
   * @param file file that opens in the editor
   */
  override fun beforeFileOpened(source: FileEditorManager, file: VirtualFile) {
    val ussAttributes = dataOpsManager.tryToGetAttributes(file).castOrNull<RemoteUssAttributes>()
    ussAttributes?.encodingChanged = false
    ussAttributes?.contentEncodingMode = null
    super.beforeFileOpened(source, file)
  }
}
