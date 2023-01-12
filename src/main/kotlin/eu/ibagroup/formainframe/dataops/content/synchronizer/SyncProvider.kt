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

import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import java.io.IOException

const val SYNC_NOTIFICATION_GROUP_ID = "eu.ibagroup.formainframe.explorer.SyncNotificationGroupId"

/**
 * Interface to provide synchronization of content downloaded
 * and prepared with mainframe with some storage in Intellij.
 * @author Viktar Mushtsin
 */
interface SyncProvider {

  /** file to synchronize */
  val file: VirtualFile

  /** strategy on what to do if someone changed content on mainframe in process the user works with it locally. */
  val saveStrategy: SaveStrategy

  /** Identifies if the file is read only. */
  val isReadOnly: Boolean

  /**
   *  Class of virtual file. Usually it is MFVirtualFile
   *  @see MFVirtualFile
   */
  val vFileClass: Class<out VirtualFile>

  /**
   * Returns document for the file.
   * @return document if it was found or null otherwise.
   */
  fun getDocument(): Document?

  /**
   * Saves document for the file.
   */
  fun saveDocument()

  /**
   * Puts the content of file in storage for the first time.
   * @param content bytes of the content to put.
   */
  @Throws(IOException::class)
  fun putInitialContent(content: ByteArray)

  /**
   * Updates the content of file in storage. Should be invoked after uploading initial content.
   * @see putInitialContent
   * @param content bytes of the content to put.
   */
  @Throws(IOException::class)
  fun loadNewContent(content: ByteArray)

  /**
   * Extracts content of file from the storage.
   * @return bytes of the required content.
   */
  @Throws(IOException::class)
  fun retrieveCurrentContent(): ByteArray

  /**
   * Function that will be invoked if some throwable object was thrown.
   * @param t object that was thrown.
   */
  fun onThrowable(t: Throwable)

  /** Function that will be invoked if the sync action is completed successfully */
  fun onSyncSuccess()

}
