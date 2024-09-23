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

package eu.ibagroup.formainframe.dataops.content.synchronizer

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsComponentFactory

/**
 * Factory interface for creating [ContentSynchronizer] instances.
 * @author Valentine Krus
 */
interface ContentSynchronizerFactory: DataOpsComponentFactory<ContentSynchronizer>

/**
 * Base interface for synchronization specific file with mainframe.
 * @author Valentine Krus
 */
interface ContentSynchronizer {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<ContentSynchronizerFactory>("eu.ibagroup.formainframe.contentSynchronizer")
  }

  /**
   * Virtual file class (MFVirtualFile in most cases).
   */
  val vFileClass: Class<out VirtualFile>

  /**
   * Checks if current implementation of content synchronizer can synchronize passed file with mainframe.
   * @param file virtual file to check compatibility with synchronizer.
   * @return true if file can be synchronized by current instance or false otherwise.
   */
  fun accepts(file: VirtualFile): Boolean

  /**
   * Performs synchronization of file with mainframe.
   * @param syncProvider instance of [SyncProvider] class that contains necessary data and handler to start synchronize.
   * @param progressIndicator indicator to reflect synchronization process (not required).
   */
  fun synchronizeWithRemote(
    syncProvider: SyncProvider,
    progressIndicator: ProgressIndicator? = null
  )

  /**
   * Returns the last successfully synced file content with MF.
   * @param syncProvider instance of [SyncProvider] class required to get content from the content storage.
   * @return byte array with content.
   */
  fun successfulContentStorage(syncProvider: SyncProvider): ByteArray


  /**
   * Checks if a file needs to be uploaded to MF.
   * @param syncProvider instance of [SyncProvider] class that contains the necessary data to check.
   * @return true if the upload is needed or false otherwise.
   */
  fun isFileUploadNeeded(syncProvider: SyncProvider): Boolean

  /**
   * Marks file as not needed for synchronisation until the next time file is modified.
   * @param syncProvider instance of [SyncProvider] class that contains file to mark.
   */
  fun markAsNotNeededForSync(syncProvider: SyncProvider)

}
