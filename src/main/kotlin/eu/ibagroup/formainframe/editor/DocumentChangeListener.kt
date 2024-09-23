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

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.isBeingEditingNow
import eu.ibagroup.formainframe.utils.sendTopic
import eu.ibagroup.formainframe.vfs.MFVirtualFile

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
class DocumentChangeListener : DocumentListener {

  override fun documentChanged(event: DocumentEvent) {
    val file = FileDocumentManager.getInstance().getFile(event.document)
    if (file is MFVirtualFile && file.isBeingEditingNow()) {
      sendTopic(
        FileContentChangeListener.FILE_CONTENT_CHANGED,
        DataOpsManager.getService().componentManager
      )
        .onUpdate(file)
    }
    super.documentChanged(event)
  }
}
