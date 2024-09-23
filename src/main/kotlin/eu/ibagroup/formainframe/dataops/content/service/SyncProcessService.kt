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

package eu.ibagroup.formainframe.dataops.content.service

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile

/**
 * Service to track the synchronization process.
 */
interface SyncProcessService {

  companion object {
    @JvmStatic
    fun getService(): SyncProcessService = service()
  }

  /**
   * Mark that the file has started synchronization and save the progress information.
   * @param file file to mark
   * @param progressIndicator progress indicator that contains progress information
   */
  fun startFileSync(file: VirtualFile, progressIndicator: ProgressIndicator)

  /**
   * Mark that the file has finished synchronization.
   * @param file file to mark
   */
  fun stopFileSync(file: VirtualFile)

  /**
   * Checks if the specified file is currently synchronized.
   * @param file virtual file to check synchronization process.
   * @return true if the file is syncing now or false otherwise.
   */
  fun isFileSyncingNow(file: VirtualFile): Boolean

  /**
   * Checks if the dependent files of the specified file are currently synchronized.
   * @param file virtual file to check synchronization process.
   * @return true if the dependent files are syncing now or false otherwise.
   */
  fun areDependentFilesSyncingNow(file: VirtualFile): Boolean

}
