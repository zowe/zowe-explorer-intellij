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

package org.zowe.explorer.dataops.content.synchronizer

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.zowe.explorer.dataops.content.service.SyncProcessService
import org.zowe.explorer.testutils.AppInitShouldSpec
import kotlin.reflect.KFunction

class SyncUtilsTestSpec : AppInitShouldSpec("dataops/content/synchronizer/syncUtils", {
  context("all functions") {
    var wasWarningShown = false

    val virtualFileMock = mockk<VirtualFile> {
      every { name } returns "fileName"
    }

    val syncProcessService = SyncProcessService.getService()

    beforeEach {
      wasWarningShown = false

      val showWarningDialogRef: (Project, String, String) -> Unit = Messages::showWarningDialog
      mockkStatic(showWarningDialogRef as KFunction<*>)
      every {
        Messages.showWarningDialog(any<Project>(), any<String>(), any<String>())
      } answers {
        wasWarningShown = true
      }

      every { syncProcessService.isFileSyncingNow(any<VirtualFile>()) } returns false
      every { syncProcessService.areDependentFilesSyncingNow(any<VirtualFile>()) } returns false
      every { syncProcessService.isAnyFileSyncingNow() } returns false
    }

    context("checkFileForSync") {
      should("check file when it is syncing") {
        every { syncProcessService.isFileSyncingNow(any<VirtualFile>()) } returns true

        val result = checkFileForSync(mockk(), virtualFileMock)

        assertSoftly {
          result shouldBe true
          wasWarningShown shouldBe true
        }
      }

      should("check file when it is not syncing") {
        val result = checkFileForSync(mockk(), virtualFileMock)

        assertSoftly {
          result shouldBe false
          wasWarningShown shouldBe false
        }
      }

      should("check file and dependent files when are they syncing") {
        every { syncProcessService.isFileSyncingNow(any<VirtualFile>()) } returns true
        every { syncProcessService.areDependentFilesSyncingNow(any<VirtualFile>()) } returns true

        val result = checkFileForSync(mockk(), virtualFileMock, checkDependentFiles = true)

        assertSoftly {
          result shouldBe true
          wasWarningShown shouldBe true
        }
      }

      should("check file and dependent files when are they not syncing") {
        val result = checkFileForSync(mockk(), virtualFileMock, checkDependentFiles = true)

        assertSoftly {
          result shouldBe false
          wasWarningShown shouldBe false
        }
      }
    }

    context("checkForSync") {
      should("check that any file is syncing") {
        every { syncProcessService.isAnyFileSyncingNow() } returns true

        val result = checkForSync(mockk())
        assertSoftly {
          result shouldBe true
          wasWarningShown shouldBe true
        }
      }

      should("check that no file is syncing") {
        val result = checkForSync()

        assertSoftly {
          result shouldBe false
          wasWarningShown shouldBe false
        }
      }
    }
  }
})
