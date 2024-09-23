/*
 * Copyright (c) 2024 IBA Group.
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

package eu.ibagroup.formainframe.testutils.testServiceImpl

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.content.service.SyncProcessService

open class TestSyncProcessServiceImpl : SyncProcessService {

  var testInstance = object : SyncProcessService {

    override fun startFileSync(file: VirtualFile, progressIndicator: ProgressIndicator) {
      TODO("Not yet implemented")
    }

    override fun stopFileSync(file: VirtualFile) {
      TODO("Not yet implemented")
    }

    override fun isFileSyncingNow(file: VirtualFile): Boolean {
      return false
    }

    override fun areDependentFilesSyncingNow(file: VirtualFile): Boolean {
      return false
    }

  }

  override fun startFileSync(file: VirtualFile, progressIndicator: ProgressIndicator) {
    testInstance.startFileSync(file, progressIndicator)
  }

  override fun stopFileSync(file: VirtualFile) {
    testInstance.stopFileSync(file)
  }

  override fun isFileSyncingNow(file: VirtualFile): Boolean {
    return testInstance.isFileSyncingNow(file)
  }

  override fun areDependentFilesSyncingNow(file: VirtualFile): Boolean {
    return testInstance.areDependentFilesSyncingNow(file)
  }

}