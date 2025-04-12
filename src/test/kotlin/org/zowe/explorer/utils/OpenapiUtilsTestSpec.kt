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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.utils

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.dataops.content.service.SyncProcessService
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic

class OpenapiUtilsTestSpec : ShouldSpec({

  context("runBackgroundableSyncTask") {

    var isStartFileSyncPerformed = false
    var isStopFileSyncPerformed = false
    var didFileSyncTaskRun = false

    val applicationMock = mockk<Application>()
    mockkStatic(ApplicationManager::getApplication)
    every { ApplicationManager.getApplication() } returns applicationMock

    every { applicationMock.getService(SyncProcessService::class.java) } returns mockk {
      every {
        startFileSync(any<VirtualFile>(), any<ProgressIndicator>())
      } answers {
        isStartFileSyncPerformed = true
      }
      every {
        stopFileSync(any<VirtualFile>())
      } answers {
        isStopFileSyncPerformed = true
      }
    }
    every { applicationMock.extensionArea } returns mockk {
      every { hasExtensionPoint(any<ExtensionPointName<*>>()) } returns false
    }

    mockkStatic(ProgressManager::getInstance)
    every { ProgressManager.getInstance() } returns mockk {
      every {
        run(any<Task.Backgroundable>())
      } answers {
        val task = firstArg<Task.Backgroundable>()
        task.run(mockk())
      }
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
