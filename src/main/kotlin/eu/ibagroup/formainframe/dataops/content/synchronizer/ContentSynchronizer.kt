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
   * @param forceReload flag indicating that the content should be force reloaded.
   */
  fun synchronizeWithRemote(
    syncProvider: SyncProvider,
    progressIndicator: ProgressIndicator? = null,
    forceReload: Boolean = false
  )

  /**
   * Returns the last successfully synced file content with MF.
   * @param syncProvider instance of [SyncProvider] class required to get content from the content storage.
   * @return byte array with content.
   */
  fun successfulContentStorage(syncProvider: SyncProvider): ByteArray

  /**
   * Checks if it is possible to synchronize the file with MF.
   * Trying to upload file content into MF.
   * @param syncProvider instance of [SyncProvider] class that contains the necessary data to check.
   * @return true if it is possible to sync the file or false otherwise.
   */
  fun isFileSyncPossible(syncProvider: SyncProvider): Boolean

}
