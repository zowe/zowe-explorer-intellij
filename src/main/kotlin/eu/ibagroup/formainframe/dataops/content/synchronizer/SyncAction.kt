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

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.runWriteActionInEdtAndWait

// TODO: doc
class SyncAction : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val vFile = getSupportedVirtualFile(e) ?: return
    val editor = getEditor(e) ?: return
    val syncProvider = DocumentedSyncProvider(vFile, SaveStrategy.default(e.project))
    runBackgroundableTask(
      title = "Synchronizing ${vFile.name}...",
      project = e.project,
      cancellable = true
    ) { indicator ->
      service<DataOpsManager>().getContentSynchronizer(vFile)?.synchronizeWithRemote(syncProvider, indicator)
      runWriteActionInEdtAndWait {
        FileDocumentManager.getInstance().saveDocument(editor.document)
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val file = getSupportedVirtualFile(e) ?: let {
      makeDisabled(e)
      return
    }
    val editor = getEditor(e) ?: let {
      makeDisabled(e)
      return
    }
    e.presentation.isEnabledAndVisible = !service<ConfigService>().isAutoSyncEnabled
            && !(editor.document.text.toByteArray() contentEquals file.contentsToByteArray())
  }

  private fun makeDisabled(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
  }

  private fun getEditor(e: AnActionEvent): Editor? {
    return e.getData(CommonDataKeys.EDITOR)
  }

  private fun getVirtualFile(e: AnActionEvent): VirtualFile? {
    return e.getData(CommonDataKeys.VIRTUAL_FILE)
  }

  private fun getSupportedVirtualFile(e: AnActionEvent): VirtualFile? {
    return getVirtualFile(e)?.let {
      if (service<DataOpsManager>().isSyncSupported(it)) {
        it
      } else {
        null
      }
    }
  }

}
