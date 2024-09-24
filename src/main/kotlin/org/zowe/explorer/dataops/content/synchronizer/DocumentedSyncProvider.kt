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

package org.zowe.explorer.dataops.content.synchronizer

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.LineSeparator
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.utils.changeEncodingTo
import org.zowe.explorer.vfs.MFVirtualFile
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
     * @param file the file to get the name to display
     * @param th the throwable to show the error message
     */
    val defaultOnThrowableHandler: (VirtualFile, Throwable) -> Unit = { _, th ->
      NotificationsService.getService().notifyError(th)
    }

    /** Default sync success handler. Won't do anything after the sync action is completed until redefined */
    val defaultOnSyncSuccessHandler: () -> Unit = {}

    /**
     * Static function is used to determine if Document exists for the Virtual File provided
     * @param file - virtual file to check
     * @return Document instance or null is no document exists for the given file
     */
    fun findDocumentForFile(file: VirtualFile): Document? {
      return FileDocumentManager.getInstance().getDocument(file)
    }

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
   * Initialize the file properties (charset, line separator)
   * when the file is opened for the first time.
   * @param content bytes of the content to init.
   */
  private fun initFileProperties(content: ByteArray) {
    val charset = getCurrentCharset()
    val text = content.toString(charset)
    val detectedLineSeparator = runCatching { StringUtil.detectSeparators(text) }.getOrNull() ?: LineSeparator.LF
    file.detectedLineSeparator = detectedLineSeparator.separatorString
    changeEncodingTo(file, charset)
  }

  /**
   * Puts initial content in file document.
   * @see SyncProvider.putInitialContent
   */
  override fun putInitialContent(content: ByteArray) {
    if (isInitialContentSet.compareAndSet(false, true)) {
      runCatching {
        initFileProperties(content)
        loadNewContent(content)
      }.onFailure {
        isInitialContentSet.set(false)
      }
    }
  }

  /** Checks if the new content needs to be load */
  private fun isLoadNeeded(content: ByteArray): Boolean {
    val currentContent = retrieveCurrentContent()
    return !content.contentEquals(currentContent)
  }

  /**
   * Update content in file document.
   * @see SyncProvider.loadNewContent
   */
  override fun loadNewContent(content: ByteArray) {
    if (!isLoadNeeded(content)) return
    val document = getDocument()
    document.castOrNull<DocumentImpl>()?.setAcceptSlashR(true)
    val wasReadOnly = isReadOnly
    if (wasReadOnly) {
      document?.setReadOnly(false)
    }
    val charset = getCurrentCharset()
    val text = content.toString(charset)
    document?.setText(text)
    if (wasReadOnly) {
      document?.setReadOnly(true)
    }
    saveDocument()
  }

  /**
   * Extracts content from the file document.
   * @see SyncProvider.retrieveCurrentContent
   */
  override fun retrieveCurrentContent(): ByteArray {
    val text = getDocument()?.text ?: ""
    val lineSeparator = FileDocumentManager.getInstance().getLineSeparator(file, null)
    val content = StringUtilRt.convertLineSeparators(text, lineSeparator)
    val charset = getCurrentCharset()
    return content.toByteArray(charset)
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
      DataOpsManager.getService().tryToGetAttributes(file).castOrNull<RemoteUssAttributes>()
    return ussAttributes?.charset ?: DEFAULT_TEXT_CHARSET
  }
}
