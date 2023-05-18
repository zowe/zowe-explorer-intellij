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

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.vfs.MFVirtualFile

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

const val MF_VIRTUAL_FILE_KEY_NAME = "mfVirtualFile"
const val USS_VIRTUAL_FILE_KEY_NAME = "ussVirtualFile"

val MF_VIRTUAL_FILE = Key.create<Boolean>(MF_VIRTUAL_FILE_KEY_NAME)
val USS_VIRTUAL_FILE = Key.create<Boolean>(USS_VIRTUAL_FILE_KEY_NAME)

/** Puts user data in file. */
fun VirtualFile.putUserData() {
  val file = this
  if (file is MFVirtualFile) {
    file.putUserData(MF_VIRTUAL_FILE, true)
    val attributes = DataOpsManager.instance.tryToGetAttributes(file)
    if (attributes is RemoteUssAttributes) {
      file.putUserData(USS_VIRTUAL_FILE, true)
    }
  }
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
