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

package org.zowe.explorer.rateus

import com.intellij.openapi.project.Project
import org.zowe.explorer.config.ConfigService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.zowe.explorer.testutils.AppInitShouldSpec

@OptIn(ExperimentalCoroutinesApi::class)
class RateUsStartupActivityTestSpec : AppInitShouldSpec("rateus/RateUsStartupActivity", {
  context("execute") {
    lateinit var projectMock: Project

    lateinit var configService: ConfigService

    beforeEach {
      configService = ConfigService.getService()
      every { configService.rateUsNotificationDelay } returns 5 * 60 * 1000L
      mockkObject(RateUsNotification.Companion)
      projectMock = mockk<Project>()
    }

    afterEach {
      Dispatchers.resetMain()
    }

    should("show the 'Rate us' notification after 5 minutes") {
      every { projectMock.isDisposed } returns false

      val rateUsStartupActivity = RateUsStartupActivity()

      runTest {
        val job = launch {
          rateUsStartupActivity.execute(projectMock)
        }

        verify(exactly = 0) {
          RateUsNotification.showRateUsNotification()
        }

        advanceTimeBy(5 * 60 * 1000L + 1L)
        advanceUntilIdle()

        verify(exactly = 1) {
          RateUsNotification.showRateUsNotification()
        }

        job.cancel()
      }
    }
    should("does not show the 'Rate us' notification cause the project is already disposed") {
      every { projectMock.isDisposed } returns true

      val rateUsStartupActivity = RateUsStartupActivity()

      runTest {
        val job = launch {
          rateUsStartupActivity.execute(projectMock)
        }

        verify(exactly = 0) {
          RateUsNotification.showRateUsNotification()
        }

        advanceTimeBy(5 * 60 * 1000L + 1L)
        advanceUntilIdle()

        verify(exactly = 0) {
          RateUsNotification.showRateUsNotification()
        }

        job.cancel()
      }
    }
    should("does not show the 'Rate us' notification cause the delay is -1") {
      every { projectMock.isDisposed } returns false

      every { configService.rateUsNotificationDelay } returns -1L

      val rateUsStartupActivity = RateUsStartupActivity()

      runTest {
        val job = launch {
          rateUsStartupActivity.execute(projectMock)
        }

        verify(exactly = 0) {
          RateUsNotification.showRateUsNotification()
        }

        advanceTimeBy(5 * 60 * 1000L + 1L)
        advanceUntilIdle()

        verify(exactly = 0) {
          RateUsNotification.showRateUsNotification()
        }

        job.cancel()
      }
    }
    should("does not show the 'Rate us' notification cause the 'Rate us' button is already clicked during the delay") {
      every { projectMock.isDisposed } returns false

      val rateUsStartupActivity = RateUsStartupActivity()

      runTest {
        val job = launch {
          rateUsStartupActivity.execute(projectMock)
        }

        verify(exactly = 0) {
          RateUsNotification.showRateUsNotification()
        }

        advanceTimeBy(1 * 60 * 1000L + 1L)

        every { configService.rateUsNotificationDelay } returns -1L

        advanceTimeBy(4 * 60 * 1000L + 1L)
        advanceUntilIdle()

        verify(exactly = 0) {
          RateUsNotification.showRateUsNotification()
        }

        job.cancel()
      }
    }
  }
})
