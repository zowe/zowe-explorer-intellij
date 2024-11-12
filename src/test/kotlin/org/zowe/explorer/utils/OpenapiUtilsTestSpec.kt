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

package org.zowe.explorer.utils

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.dataops.content.service.SyncProcessService
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.testutils.testServiceImpl.TestSyncProcessServiceImpl
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class OpenapiUtilsTestSpec : WithApplicationShouldSpec({

  context("runBackgroundableSyncTask") {

    var isStartFileSyncPerformed = false
    var isStopFileSyncPerformed = false

    beforeEach {
      isStartFileSyncPerformed = false
      isStopFileSyncPerformed = false

      val syncProcessService = SyncProcessService.getService() as TestSyncProcessServiceImpl
      syncProcessService.testInstance = object : TestSyncProcessServiceImpl() {
        override fun startFileSync(file: VirtualFile, progressIndicator: ProgressIndicator) {
          isStartFileSyncPerformed = true
        }

        override fun stopFileSync(file: VirtualFile) {
          isStopFileSyncPerformed = true
        }
      }
    }

    should("run backgroundable sync task when virtual file is null") {
      runBackgroundableSyncTask(
        "Title",
        null,
        true,
        null,
      ) { }

      assertSoftly {
        isStartFileSyncPerformed shouldBe false
        isStopFileSyncPerformed shouldBe false
      }
    }
    should("run backgroundable sync task when virtual file is not null") {
      runBackgroundableSyncTask(
        "Title",
        null,
        true,
        mockk(),
      ) { }

      assertSoftly {
        isStartFileSyncPerformed shouldBe true
        isStopFileSyncPerformed shouldBe true
      }
    }

  }
})
