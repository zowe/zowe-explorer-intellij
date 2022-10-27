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

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Provides synchronization with file document.
 * @param file file to synchronize content.
 * @param saveStrategy strategy on what to do if someone changed content
 *                     on mainframe in process the user works with it locally.
 * @param onThrowableHandler Function that will be invoked if some throwable object was thrown.
 * @author Viktar Mushtsin
 * @author Valiantsin Krus
 */
class DocumentedSyncProvider(
  override val file: VirtualFile,
  override val saveStrategy: SaveStrategy = SaveStrategy.default(),
  val onThrowableHandler: (Throwable) -> Unit = { defaultOnThrowableHandler(file, it) },
  val onSyncSuccessHandler: () -> Unit = { defaultOnSyncSuccessHandler() }
) : SyncProvider {

  companion object {
    /**
     * Default throwable handler to show error notification that the file cannot be synchronized due to some error appeared
     * param file - the file to get the name to display
     * param th - the throwable to show the error message
     */
    val defaultOnThrowableHandler: (VirtualFile, Throwable) -> Unit = { file, th ->
      Notifications.Bus.notify(
        Notification(
          SYNC_NOTIFICATION_GROUP_ID,
          "Cannot synchronize file \"${file.name}\" with mainframe",
          th.message ?: "",
          NotificationType.ERROR
        )
      )
    }

    /** Default sync success handler. Won't do anything after the sync action is completed until redefined */
    val defaultOnSyncSuccessHandler: () -> Unit = {}
  }

  /**
   * Finds document from FileDocumentManager.
   * @see FileDocumentManager
   * @see SyncProvider.getDocument
   */
  override fun getDocument(): Document? {
    return FileDocumentManager.getInstance().getDocument(file)
  }

  override val vFileClass = MFVirtualFile::class.java

  private val isInitialContentSet = AtomicBoolean(false)

  override val isReadOnly: Boolean
    get() = getDocument()?.isWritable != true

  /**
   * Puts initial content in file document.
   * @see SyncProvider.putInitialContent
   */
  override fun putInitialContent(content: ByteArray) {
    if (isInitialContentSet.compareAndSet(false, true)) {
      runCatching {
        file.getOutputStream(null).use {
          it.write(content)
        }
        loadNewContent(content)
      }.onFailure {
        isInitialContentSet.set(false)
      }
    }
  }

  /**
   * Update content in file document.
   * @see SyncProvider.loadNewContent
   */
  override fun loadNewContent(content: ByteArray) {
    val document = getDocument()
    document.castOrNull<DocumentImpl>()?.setAcceptSlashR(true)
    val wasReadOnly = isReadOnly
    if (wasReadOnly) {
      document?.setReadOnly(false)
    }
    document?.setText(String(content))
    if (wasReadOnly) {
      document?.setReadOnly(true)
    }
  }

  /**
   * Extracts content from the file document.
   * @see SyncProvider.retrieveCurrentContent
   */
  override fun retrieveCurrentContent(): ByteArray {
    return getDocument()?.text?.toByteArray() ?: ByteArray(0)
  }

  override fun onThrowable(t: Throwable) {
    onThrowableHandler(t)
  }

  override fun onSyncSuccess() {
    onSyncSuccessHandler()
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
