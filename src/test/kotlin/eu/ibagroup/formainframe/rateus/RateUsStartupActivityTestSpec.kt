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

package eu.ibagroup.formainframe.rateus

import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class RateUsStartupActivityTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }

  context("rateus module: RateUsStartupActivity") {
    context("execute") {
      lateinit var projectMock: Project

      beforeTest {
        mockkObject(RateUsNotification.Companion)
        projectMock = mockk<Project>()
      }

      afterTest {
        unmockkAll()
        Dispatchers.resetMain()
      }

      should("show the 'Rate us' notification after 5 minutes") {
        every { projectMock.isDisposed } returns false

        val configService = ConfigService.getService()
        configService.rateUsNotificationDelay = 5 * 60 * 1000L

        val rateUsStartupActivity = RateUsStartupActivity()

        runTest {
          val job = launch {
            rateUsStartupActivity.execute(projectMock)
          }

          verify(exactly = 0) {
            RateUsNotification.showRateUsNotification()
          }

          advanceTimeBy(5 * 60 * 1000L)
          advanceUntilIdle()

          verify(exactly = 1) {
            RateUsNotification.showRateUsNotification()
          }

          job.cancel()
        }
      }
      should("does not show the 'Rate us' notification cause the project is already disposed") {
        every { projectMock.isDisposed } returns true

        val configService = ConfigService.getService()
        configService.rateUsNotificationDelay = 5 * 60 * 1000L

        val rateUsStartupActivity = RateUsStartupActivity()

        runTest {
          val job = launch {
            rateUsStartupActivity.execute(projectMock)
          }

          verify(exactly = 0) {
            RateUsNotification.showRateUsNotification()
          }

          advanceTimeBy(5 * 60 * 1000L)
          advanceUntilIdle()

          verify(exactly = 0) {
            RateUsNotification.showRateUsNotification()
          }

          job.cancel()
        }
      }
      should("does not show the 'Rate us' notification cause the delay is -1") {
        every { projectMock.isDisposed } returns false

        val configService = ConfigService.getService()
        configService.rateUsNotificationDelay = -1L

        val rateUsStartupActivity = RateUsStartupActivity()

        runTest {
          val job = launch {
            rateUsStartupActivity.execute(projectMock)
          }

          verify(exactly = 0) {
            RateUsNotification.showRateUsNotification()
          }

          advanceTimeBy(5 * 60 * 1000L)
          advanceUntilIdle()

          verify(exactly = 0) {
            RateUsNotification.showRateUsNotification()
          }

          job.cancel()
        }
      }
      should("does not show the 'Rate us' notification cause the 'Rate us' button is already clicked during the delay") {
        every { projectMock.isDisposed } returns false

        val configService = ConfigService.getService()
        configService.rateUsNotificationDelay = 5 * 60 * 1000L

        val rateUsStartupActivity = RateUsStartupActivity()

        runTest {
          val job = launch {
            rateUsStartupActivity.execute(projectMock)
          }

          verify(exactly = 0) {
            RateUsNotification.showRateUsNotification()
          }

          advanceTimeBy(1 * 60 * 1000L)
          configService.rateUsNotificationDelay = -1L
          advanceTimeBy(4 * 60 * 1000L)
          advanceUntilIdle()

          verify(exactly = 0) {
            RateUsNotification.showRateUsNotification()
          }

          job.cancel()
        }
      }
    }
  }
})
