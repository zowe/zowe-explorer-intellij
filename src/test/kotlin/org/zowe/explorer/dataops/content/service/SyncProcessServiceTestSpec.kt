/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Dzianis Lisiankou
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.dataops.content.service

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.zowe.explorer.testutils.AppInitShouldSpec
import kotlin.reflect.KFunction

class SyncProcessServiceTestSpec : AppInitShouldSpec("dataops/content/SyncProcessService", {
  context("all functions") {
    val syncProcessService = SyncProcessServiceImpl()

    val virtualFileMock = mockk<VirtualFile>()
    val progressIndicatorMock = mockk<ProgressIndicator>()

    beforeEach {
      syncProcessService.startFileSync(virtualFileMock, progressIndicatorMock)

      val isAncestorRef: (VirtualFile, VirtualFile, Boolean) -> Boolean = VfsUtilCore::isAncestor
      mockkStatic(isAncestorRef as KFunction<*>)
    }

    afterEach {
      syncProcessService.stopFileSync(virtualFileMock)
    }

    context("isFileSyncingNow") {
      should("file is syncing now") {
        every { progressIndicatorMock.isRunning } returns true

        val result = syncProcessService.isFileSyncingNow(virtualFileMock)

        assertSoftly { result shouldBe true }
      }

      should("file is not syncing now") {
        every { progressIndicatorMock.isRunning } returns false

        val result = syncProcessService.isFileSyncingNow(virtualFileMock)

        assertSoftly { result shouldBe false }
      }
    }

    context("areDependentFilesSyncingNow") {
      should("dependent files are syncing now") {
        every { progressIndicatorMock.isRunning } returns true
        every { VfsUtilCore.isAncestor(virtualFileMock, any(), true) } returns true

        val result = syncProcessService.areDependentFilesSyncingNow(virtualFileMock)

        assertSoftly { result shouldBe true }
      }

      should("dependent files are not syncing now") {
        every { progressIndicatorMock.isRunning } returns true
        every { VfsUtilCore.isAncestor(virtualFileMock, any(), true) } returns false

        val result = syncProcessService.areDependentFilesSyncingNow(virtualFileMock)

        assertSoftly { result shouldBe false }
      }

      should("dependent files are not syncing now because no sync is running") {
        every { progressIndicatorMock.isRunning } returns false

        val result = syncProcessService.areDependentFilesSyncingNow(virtualFileMock)

        assertSoftly { result shouldBe false }
      }
    }

    context("isAnyFileSyncingNow") {
      should("any file is syncing now") {
        every { progressIndicatorMock.isRunning } returns true

        val result = syncProcessService.isAnyFileSyncingNow()

        assertSoftly { result shouldBe true }
      }

      should("no file is syncing now") {
        every { progressIndicatorMock.isRunning } returns false

        val result = syncProcessService.isAnyFileSyncingNow()

        assertSoftly { result shouldBe false }
      }
    }
  }
})
