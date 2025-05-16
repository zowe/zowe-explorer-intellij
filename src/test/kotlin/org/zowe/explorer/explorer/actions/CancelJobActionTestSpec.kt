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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.explorer.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.log.JobLogFetcher
import org.zowe.explorer.dataops.operations.jobs.CancelJobOperation
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.ui.build.jobs.JOBS_LOG_VIEW
import org.zowe.explorer.ui.build.jobs.JobBuildTreeView
import org.zowe.kotlinsdk.Job

class CancelJobActionTestSpec : AppInitShouldSpec("explorer/actions/CancelJobAction", {
  context("all functions") {
    var didChangeIsEnabledAndVisible = false
    var didChangeIsEnabled = false
    var didTriggerSuccessNotification = false
    var didTriggerErrorNotification = false
    var didPerformOperation = false
    var isEnabledAndVisibleNewValue: Boolean? = null
    var isEnabledNewValue: Boolean? = null

    val cancelJobAction = CancelJobAction()

    val logFetcherMock = mockk<JobLogFetcher>()

    val jobsLogViewMock = mockk<JobBuildTreeView> {
      every { getJobLogger() } returns mockk {
        every { logFetcher } returns logFetcherMock
      }
      every { getConnectionConfig() } returns mockk()
      every {
        showNotification(any(), any(), any(), any())
      } answers {
        val notificationType = arg<NotificationType>(3)
        when (notificationType) {
          NotificationType.INFORMATION -> {
            didTriggerSuccessNotification = true
          }
          NotificationType.ERROR -> {
            didTriggerErrorNotification = true
          }
          else -> {
            fail("Unknown notification type: $notificationType")
          }
        }
      }
    }
    val eventMock = mockk<AnActionEvent> {
      every { presentation } returns mockk {
        every {
          isEnabledAndVisible = any()
        } answers {
          didChangeIsEnabledAndVisible = true
          isEnabledAndVisibleNewValue = firstArg<Boolean>()
        }
        every {
          isEnabled = any()
        } answers {
          didChangeIsEnabled = true
          isEnabledNewValue = firstArg<Boolean>()
        }
      }
      every { project } returns mockk()
    }

    val dataOpsManager = DataOpsManager.getService()

    beforeEach {
      didChangeIsEnabledAndVisible = false
      didChangeIsEnabled = false
      didTriggerSuccessNotification = false
      didTriggerErrorNotification = false
      didPerformOperation = false
      isEnabledAndVisibleNewValue = null
      isEnabledNewValue = null

      every { eventMock.getData(JOBS_LOG_VIEW) } returns jobsLogViewMock
      every { logFetcherMock.getCachedJobStatus() } returns mockk {
        every { jobName } returns "JOBNAME"
        every { jobId } returns "JOB123"
        every { status } returns Job.Status.ACTIVE
      }

      every {
        dataOpsManager.performOperation(any<CancelJobOperation>(), any())
      } answers {
        didPerformOperation = true
        mockk()
      }
    }

    context("actionPerformed") {
      should("cancel job and show success notification") {
        cancelJobAction.actionPerformed(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          isEnabledAndVisibleNewValue shouldBe null
          didTriggerSuccessNotification shouldBe true
          didTriggerErrorNotification shouldBe false
          didPerformOperation shouldBe true
        }
      }

      should("fail to cancel job and show failure notification") {
        every {
          dataOpsManager.performOperation(any<CancelJobOperation>(), any())
        } answers {
          didPerformOperation = true
          throw Exception("Test exception")
        }

        cancelJobAction.actionPerformed(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          isEnabledAndVisibleNewValue shouldBe null
          didTriggerSuccessNotification shouldBe false
          didTriggerErrorNotification shouldBe true
          didPerformOperation shouldBe true
        }
      }

      should("not perform the operation as the job status is not available") {
        every { logFetcherMock.getCachedJobStatus() } returns null

        cancelJobAction.actionPerformed(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          isEnabledAndVisibleNewValue shouldBe null
          didTriggerSuccessNotification shouldBe false
          didTriggerErrorNotification shouldBe false
          didPerformOperation shouldBe false
        }
      }

      should("not allow to perform the action as the job log view is not visible") {
        every { eventMock.getData(JOBS_LOG_VIEW) } returns null

        cancelJobAction.actionPerformed(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
          didTriggerSuccessNotification shouldBe false
          didTriggerErrorNotification shouldBe false
          didPerformOperation shouldBe false
        }
      }
    }

    context("update") {
      should("show the cancel action in jobs log view when the job status is not null or OUTPUT") {
        cancelJobAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          didChangeIsEnabled shouldBe false
          isEnabledAndVisibleNewValue shouldBe null
          isEnabledNewValue shouldBe null
        }
      }

      should("not show the cancel action in jobs log view when the job status is OUTPUT") {
        every { logFetcherMock.getCachedJobStatus() } returns mockk {
          every { status } returns Job.Status.OUTPUT
        }

        cancelJobAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          didChangeIsEnabled shouldBe true
          isEnabledAndVisibleNewValue shouldBe null
          isEnabledNewValue shouldBe false
        }
      }

      should("not show the cancel action in jobs log view when the job status is not fetched") {
        every { logFetcherMock.getCachedJobStatus() } returns mockk {
          every { status } returns null
        }

        cancelJobAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          didChangeIsEnabled shouldBe true
          isEnabledAndVisibleNewValue shouldBe null
          isEnabledNewValue shouldBe false
        }
      }

      should("not show the cancel action in jobs log view when there is no jobs log view") {
        every { eventMock.getData(JOBS_LOG_VIEW) } returns null

        cancelJobAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          didChangeIsEnabled shouldBe false
          isEnabledAndVisibleNewValue shouldBe false
          isEnabledNewValue shouldBe null
        }
      }
    }
  }
})