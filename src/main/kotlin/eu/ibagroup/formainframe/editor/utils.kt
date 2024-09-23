/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.editor

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/**
 * Show dialog which prevents user from closing the file without synchronizing
 * @param fileName the name of the file for the dialog message
 * @param project the project on which to issue the dialog
 */
fun showSyncOnCloseDialog(fileName: String, project: Project): Boolean {
  return MessageDialogBuilder
    .yesNo(
      title = "File $fileName Is Not Synced",
      message = "Do you want to sync the file with the Mainframe before it is closed?"
    )
    .asWarning()
    .ask(project = project)
}

// This code should not be present in Zowe repo
val zoweExplorerInstalled = PluginManager.isPluginInstalled(PluginId.getId("org.zowe.explorer"))

const val MF_VIRTUAL_FILE_KEY_NAME = "mfVirtualFile"
const val USS_VIRTUAL_FILE_KEY_NAME = "ussVirtualFile"

val MF_VIRTUAL_FILE = Key.create<Boolean>(MF_VIRTUAL_FILE_KEY_NAME)
val USS_VIRTUAL_FILE = Key.create<Boolean>(USS_VIRTUAL_FILE_KEY_NAME)

/** Puts user data in file. */
fun putUserDataInFile(file: MFVirtualFile) {
  file.putUserData(MF_VIRTUAL_FILE, true)
  DataOpsManager.getService()
    .tryToGetAttributes(file)
    ?.takeIf { it is RemoteUssAttributes }
    ?.let { file.putUserData(USS_VIRTUAL_FILE, true) }
}

/**
 * Checks if a file is mf virtual file by key.
 * @return true if it is or false otherwise.
 */
fun VirtualFile.isMfVirtualFile(): Boolean {
  return this.get()?.keys?.find { it.toString() == MF_VIRTUAL_FILE_KEY_NAME } != null
}

/**
 * Checks if a file is uss virtual file by key.
 * @return true if it is or false otherwise.
 */
fun VirtualFile.isUssVirtualFile(): Boolean {
  return this.get()?.keys?.find { it.toString() == USS_VIRTUAL_FILE_KEY_NAME } != null
}

/**
 * Function checks the current editor status for the opened document and returns true if document is writable, false otherwise.
 * If document is read-only shows the information hint on the TOP of the current caret position
 * @param editor - current opened file editor
 */
fun requestDocumentWriting(editor: Editor): Boolean {
  val writeAccess = FileDocumentManager.getInstance().requestWritingStatus(editor.document, editor.project)
  if (!writeAccess.hasWriteAccess()) {
    EditorModificationUtil.setReadOnlyHint(editor, "File is read-only while indexing is in progress.").apply {
      EditorModificationUtil.checkModificationAllowed(editor)
    }
    return false
  }
  return true
}