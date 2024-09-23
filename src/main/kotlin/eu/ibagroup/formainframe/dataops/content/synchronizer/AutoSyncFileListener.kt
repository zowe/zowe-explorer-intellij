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

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic

/**
 * Auto sync file listener that provides information about whether the file should be synced now.
 */
interface AutoSyncFileListener {

  companion object {
    @JvmField
    val AUTO_SYNC_FILE = Topic.create("autoSyncFile", AutoSyncFileListener::class.java)
  }

  fun sync(file: VirtualFile)
}
