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
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import java.nio.charset.Charset
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

  /**
   * Saves document in FileDocumentManager if it was found.
   * @see FileDocumentManager
   * @see SyncProvider.saveDocument
   */
  override fun saveDocument() {
    getDocument()?.let { FileDocumentManager.getInstance().saveDocument(it) }
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
    val charset = getCurrentCharset()
    document?.setText(String(content, charset))
    if (wasReadOnly) {
      document?.setReadOnly(true)
    }
  }

  /**
   * Extracts content from the file document.
   * @see SyncProvider.retrieveCurrentContent
   */
  override fun retrieveCurrentContent(): ByteArray {
    val charset = getCurrentCharset()
    return convertContentWithLineSeparator(getDocument()?.text ?: "").toByteArray(charset)
  }

  override fun onThrowable(t: Throwable) {
    if (t.message?.contains("Permission denied") == true) {
      onThrowableHandler(Throwable("Permission denied. " + t.message))
      return
    }
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

  /**
   * Get the current file charset.
   * For uss files this is [RemoteUssAttributes.charset], for other files - [DEFAULT_TEXT_CHARSET].
   * @return charset of the file.
   */
  private fun getCurrentCharset(): Charset {
    val ussAttributes =
      service<DataOpsManager>().tryToGetAttributes(file).castOrNull<RemoteUssAttributes>()
    return ussAttributes?.charset ?: DEFAULT_TEXT_CHARSET
  }

  /**
   * Converts text to text with selected line separator.
   * Necessary to correctly retrieve the contents of the document from the editor.
   * @param content current editor text.
   * @return converted text.
   */
  private fun convertContentWithLineSeparator(content: String): String {
    if (content.isEmpty()) {
      return content
    }
    val attributes =
      service<DataOpsManager>().tryToGetAttributes(file).castOrNull<RemoteUssAttributes>()
    if (attributes != null) {
      val lfLineSeparator = LF_LINE_SEPARATOR
      val crLineSeparator = CR_LINE_SEPARATOR
      val contentLineSeparator = if (content.contains(crLineSeparator)) {
        if (content.contains(lfLineSeparator)) {
          crLineSeparator + lfLineSeparator
        } else {
          crLineSeparator
        }
      } else {
        lfLineSeparator
      }
      file.detectedLineSeparator?.let {
        if (contentLineSeparator != it) {
          return content.replace(contentLineSeparator, it)
        }
      }
    }
    return content
  }
}
