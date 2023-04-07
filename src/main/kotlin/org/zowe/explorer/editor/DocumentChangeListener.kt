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

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.utils.isBeingEditingNow
import org.zowe.explorer.utils.sendTopic
import org.zowe.explorer.vfs.MFVirtualFile

/**
 * File content change listener that provides information about whether the file has been modified by the user.
 */
interface FileContentChangeListener {

  companion object {
    @JvmField
    val FILE_CONTENT_CHANGED = Topic.create("fileContentChanged", FileContentChangeListener::class.java)
  }

  fun onUpdate(file: VirtualFile)
}

/**
 * Document change listener that listens for all document changes and
 * sends an event about it if the user has made changes.
 */
class DocumentChangeListener: DocumentListener {

  override fun documentChanged(event: DocumentEvent) {
    val file = FileDocumentManager.getInstance().getFile(event.document)
    if (file is MFVirtualFile && file.isBeingEditingNow()) {
      sendTopic(FileContentChangeListener.FILE_CONTENT_CHANGED, DataOpsManager.instance.componentManager)
        .onUpdate(file)
    }
    super.documentChanged(event)
  }
}
