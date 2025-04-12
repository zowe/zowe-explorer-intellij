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
 *   Dzianis Lisiankou
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.utils

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.dataops.content.service.SyncProcessService
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.zowe.explorer.testutils.AppInitShouldSpec

class OpenapiUtilsTestSpec : AppInitShouldSpec("utils/openapiUtils", {
  context("runBackgroundableSyncTask") {
    var isStartFileSyncPerformed = false
    var isStopFileSyncPerformed = false
    var didFileSyncTaskRun = false

    val syncProcessService = SyncProcessService.getService()
    every {
      syncProcessService.startFileSync(any<VirtualFile>(), any<ProgressIndicator>())
    } answers {
      isStartFileSyncPerformed = true
    }
    every {
      syncProcessService.stopFileSync(any<VirtualFile>())
    } answers {
      isStopFileSyncPerformed = true
    }

    beforeEach {
      isStartFileSyncPerformed = false
      isStopFileSyncPerformed = false
      didFileSyncTaskRun = false
    }

    should("run backgroundable sync task when virtual file is null") {
      runBackgroundableSyncTask(
        "Title",
        null,
        true,
        null,
      ) {
        didFileSyncTaskRun = true
      }

      assertSoftly {
        isStartFileSyncPerformed shouldBe false
        didFileSyncTaskRun shouldBe true
        isStopFileSyncPerformed shouldBe false
      }
    }
    should("run backgroundable sync task when virtual file is not null") {
      runBackgroundableSyncTask(
        "Title",
        null,
        true,
        mockk(),
      ) {
        didFileSyncTaskRun = true
      }

      assertSoftly {
        isStartFileSyncPerformed shouldBe true
        didFileSyncTaskRun shouldBe true
        isStopFileSyncPerformed shouldBe true
      }
    }
  }
})
