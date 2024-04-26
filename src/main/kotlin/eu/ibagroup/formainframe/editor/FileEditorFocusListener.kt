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

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.ui.isComponentUnderMouse
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.content.synchronizer.AutoSyncFileListener
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.dataops.content.synchronizer.SaveStrategy
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import java.awt.IllegalComponentStateException
import java.awt.event.FocusEvent
import javax.swing.SwingUtilities

/**
 * File editor focus listener.
 * Need to handle focus lost event.
 */
class FileEditorFocusListener: FocusChangeListener {

  /**
   * Handle the focus lost event on which the file should be synchronized with the MF.
   * If the file has been changed, then an auto sync event is fired.
   * @param editor the editor in which the file is open.
   */
  override fun focusLost(editor: Editor, event: FocusEvent) {
    val mouseClickInEditor = editor.component.isComponentUnderMouse()
    if (!mouseClickInEditor) {
      event.oppositeComponent?.let { focusedComponent ->
        try {
          val point = focusedComponent.locationOnScreen
          SwingUtilities.convertPointFromScreen(point, editor.component)
          if (editor.component.contains(point)) {
            return
          }
        } catch (e : IllegalComponentStateException) {
          val diagnosticMessage = "Error happened while dispatching focusLost event. Content will be synchronized anyway.\n" +
              "Diagnostic info:\n" + "Editor component name: ${editor.component.name}\n" + "Focused component name: ${focusedComponent.name}\n" +
                  "Focused component location on screen: x coordinate is ${focusedComponent.location.x}, y coordinate is ${focusedComponent.location.y}.\n" +
                  "isShowing: ${focusedComponent.isShowing}\n"
          log<FileEditorFocusListener>().error(diagnosticMessage, e)
          return@let
        }
      }
      if (ConfigService.instance.isAutoSyncEnabled) {
        val project = editor.project
        project?.let {
          val file = (editor as? EditorEx)?.virtualFile
          if (file is MFVirtualFile && file.isWritable) {
            val syncProvider = DocumentedSyncProvider(file, SaveStrategy.default(project))
            val contentSynchronizer = service<DataOpsManager>().getContentSynchronizer(file)
            val currentContent = runReadAction { syncProvider.retrieveCurrentContent() }
            val previousContent = contentSynchronizer?.successfulContentStorage(syncProvider)
            val needToUpload = contentSynchronizer?.isFileUploadNeeded(syncProvider) == true
            if (!(currentContent contentEquals previousContent) && needToUpload) {
              runBackgroundableTask(
                title = "Synchronizing ${file.name}...",
                project = project,
                cancellable = true
              ) {
                val incompatibleEncoding = !checkEncodingCompatibility(file, project)
                runInEdt {
                  if (incompatibleEncoding && !showSaveAnywayDialog(file.charset)) {
                    return@runInEdt
                  }
                  runWriteActionInEdtAndWait { syncProvider.saveDocument() }
                  sendTopic(AutoSyncFileListener.AUTO_SYNC_FILE, project).sync(file)
                }
              }
            }
          }
        }
      }
    }
    super.focusLost(editor)
  }
}
