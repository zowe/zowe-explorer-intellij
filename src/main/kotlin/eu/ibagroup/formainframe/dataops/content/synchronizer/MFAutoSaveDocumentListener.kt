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

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.vfs.MFVirtualFile

// TODO: need to rework
/**
 * Listener that reacts on document saving to local file system and synchronizes it with mainframe.
 * Needed to fully implement auto-save.
 * @author Valentine Krus
 */
class MFAutoSaveDocumentListener: BulkFileListener {
  /**
   * Filters file events for saving events and synchronizes file from these events.
   */
  override fun after(events: MutableList<out VFileEvent>) {
    if (!service<ConfigService>().isAutoSyncEnabled){
      return
    }
    events.forEach {
      if (!it.isFromSave && it.requestor !is MFVirtualFile) {
        return
      }
      val vFile = it.file
      val mfFile = vFile.castOrNull<MFVirtualFile>() ?: return
      val dataOpsManager = service<DataOpsManager>()

      if (dataOpsManager.isSyncSupported(mfFile)) {
        val contentSynchronizer = dataOpsManager.getContentSynchronizer(mfFile) ?: return
        runBackgroundableTask("Synchronizing file ${mfFile.name} with mainframe") { indicator ->
          contentSynchronizer.synchronizeWithRemote(DocumentedSyncProvider(mfFile), indicator)
        }
      }
    }
  }
}
