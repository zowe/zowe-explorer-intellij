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

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.ConcurrentHashMap

/**
 * Base implementation of [SyncProcessService].
 */
class SyncProcessServiceImpl: SyncProcessService {

  private val fileToProgressIndicatorMap = ConcurrentHashMap<VirtualFile, ProgressIndicator>()

  /**
   * Base implementation of [SyncProcessService.startFileSync] method.
   */
  override fun startFileSync(file: VirtualFile, progressIndicator: ProgressIndicator) {
    fileToProgressIndicatorMap[file] = progressIndicator
  }

  /**
   * Base implementation of [SyncProcessService.stopFileSync] method.
   */
  override fun stopFileSync(file: VirtualFile) {
    fileToProgressIndicatorMap.remove(file)
  }

  /**
   * Base implementation of [SyncProcessService.isFileSyncingNow] method.
   */
  override fun isFileSyncingNow(file: VirtualFile): Boolean {
    return fileToProgressIndicatorMap[file]?.isRunning == true
  }

  /**
   * Base implementation of [SyncProcessService.areDependentFilesSyncingNow] method.
   */
  override fun areDependentFilesSyncingNow(file: VirtualFile): Boolean {
    return fileToProgressIndicatorMap.any { (syncingFile, progressIndicator) ->
      progressIndicator.isRunning && VfsUtilCore.isAncestor(file, syncingFile, true)
    }
  }

}
