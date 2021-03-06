/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.content.synchronizer

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.vfs.MFVirtualFile
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

// TODO: doc
class DocumentedSyncProvider (
  override val file: VirtualFile,
  override val saveStrategy: SaveStrategy = SaveStrategy.default(),
  val onThrowableHandler: (Throwable) -> Unit = {
    Notifications.Bus.notify(
      Notification(
        SYNC_NOTIFICATION_GROUP_ID,
        "Cannot synchronize file \"${file.name}\" with mainframe",
        it.message ?: "",
        NotificationType.ERROR
      )
    )
  }
) : SyncProvider {

  override fun getDocument(): Document {
    return FileDocumentManager.getInstance().getDocument(file) ?: throw IOException("Unsupported file ${file.path}")
  }

  override val vFileClass = MFVirtualFile::class.java

  private val isInitialContentSet = AtomicBoolean(false)

  override val isReadOnly: Boolean
    get() = !getDocument().isWritable

  override fun putInitialContent(content: ByteArray) {
    if (isInitialContentSet.compareAndSet(false, true)) {
      runCatching {
        file.getOutputStream(null).use {
          it.write(content)
        }
        val document = getDocument()
        document.castOrNull<DocumentImpl>()?.setAcceptSlashR(true)
        val wasReadOnly = isReadOnly
        if (wasReadOnly) {
          document.setReadOnly(false)
        }
        document.setText(String(content))
        if (wasReadOnly) {
          document.setReadOnly(true)
        }
      }.onFailure {
        isInitialContentSet.set(false)
      }
    }
  }

  override fun loadNewContent(content: ByteArray) {
    CommandProcessor.getInstance().runUndoTransparentAction { getDocument().setText(String(content)) }
  }

  override fun retrieveCurrentContent(): ByteArray {
    return getDocument().text.toByteArray()
  }

  override fun onThrowable(t: Throwable) {
    onThrowableHandler(t)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is DocumentedSyncProvider) return false

    if (file != other.file) return false

    return true
  }

  override fun hashCode(): Int {
    return file.hashCode()
  }

}
