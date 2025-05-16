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
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.log.JobLogFetcher
import org.zowe.explorer.dataops.operations.jobs.ReleaseJobOperation
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.ui.build.jobs.JOBS_LOG_VIEW
import org.zowe.explorer.ui.build.jobs.JobBuildTreeView
import org.zowe.explorer.utils.runInEdtAndWait
import org.zowe.kotlinsdk.Job
import org.zowe.kotlinsdk.ReleaseJobRequest

class ReleaseJobActionTestSpec : AppInitShouldSpec("explorer/actions/ReleaseJobAction", {
  context("all functions") {
    var didPerformReleaseJobOperation = false
    var didShowInfoNotification = false
    var didShowErrorNotification = false
    var isEnabledNewValue: Boolean? = null
    var isVisibleNewValue: Boolean? = null

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
            didShowInfoNotification = true
          }
          NotificationType.ERROR -> {
            didShowErrorNotification = true
          }
          else -> fail("Unexpected notification type: $notificationType")
        }
      }
    }
    val eventMock = mockk<AnActionEvent> {
      every { project } returns mockk()
      every { presentation } returns mockk {
        every {
          isEnabled = any()
        } answers {
          isEnabledNewValue = firstArg<Boolean>()
        }
        every {
          isVisible = any()
        } answers {
          isVisibleNewValue = firstArg<Boolean>()
        }
        every {
          isEnabledAndVisible = any()
        } answers {
          isEnabled = firstArg<Boolean>()
          isVisible = firstArg<Boolean>()
        }
      }
    }

    val dataOpsManager = DataOpsManager.getService()

    val releaseJobAction = ReleaseJobAction()

    beforeEach {
      didPerformReleaseJobOperation = false
      didShowInfoNotification = false
      didShowErrorNotification = false
      isEnabledNewValue = null
      isVisibleNewValue = null

      every { logFetcherMock.getCachedJobStatus() } returns mockk {
        every { status } returns Job.Status.INPUT
        every { jobName } returns "TESTJOB"
        every { jobId } returns "TESTJID"
      }
      every { eventMock.getData(JOBS_LOG_VIEW) } returns jobsLogViewMock

      every {
        dataOpsManager.performOperation(any<Operation<Any>>(), any())
      } answers {
        when (val operation = firstArg<Operation<*>>()) {
          is ReleaseJobOperation -> {
            didPerformReleaseJobOperation = true
            mockk<ReleaseJobRequest>()
          }
          else -> fail("Unexpected operation: $operation")
        }
      }
    }

    context("actionPerformed") {
      should("perform job release operation") {
        runInEdtAndWait {
          releaseJobAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didPerformReleaseJobOperation shouldBe true
          didShowInfoNotification shouldBe true
          didShowErrorNotification shouldBe false
        }
      }

      should("fail to perform job release operation due to the remote error") {
        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any())
        } answers {
          didPerformReleaseJobOperation = true
          throw Throwable("Test exception")
        }

        runInEdtAndWait {
          releaseJobAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didPerformReleaseJobOperation shouldBe true
          didShowInfoNotification shouldBe false
          didShowErrorNotification shouldBe true
        }
      }

      should("not perform job release operation cause the job status is null") {
        every { logFetcherMock.getCachedJobStatus() } returns null

        runInEdtAndWait {
          releaseJobAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didPerformReleaseJobOperation shouldBe false
          didShowInfoNotification shouldBe false
          didShowErrorNotification shouldBe false
        }
      }

      should("not perform job release operation cause the jobs console view is not initialized yet") {
        every { eventMock.getData(JOBS_LOG_VIEW) } returns null

        runInEdtAndWait {
          releaseJobAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didPerformReleaseJobOperation shouldBe false
          didShowInfoNotification shouldBe false
          didShowErrorNotification shouldBe false
        }
      }
    }

    context("update") {
      should("show the Release Job action") {
        releaseJobAction.update(eventMock)

        assertSoftly {
          isEnabledNewValue shouldBe null
          isVisibleNewValue shouldBe null
        }
      }

      should("show the Release Job action as disabled cause the job status is null") {
        every { logFetcherMock.getCachedJobStatus() } returns mockk {
          every { status } returns null
        }

        releaseJobAction.update(eventMock)

        assertSoftly {
          isEnabledNewValue shouldBe false
          isVisibleNewValue shouldBe null
        }
      }

      should("show the Release Job action as disabled cause the job status is ACTIVE") {
        every { logFetcherMock.getCachedJobStatus() } returns mockk {
          every { status } returns Job.Status.ACTIVE
        }

        releaseJobAction.update(eventMock)

        assertSoftly {
          isEnabledNewValue shouldBe false
          isVisibleNewValue shouldBe null
        }
      }

      should("show the Release Job action as disabled cause the job status is OUTPUT") {
        every { logFetcherMock.getCachedJobStatus() } returns mockk {
          every { status } returns Job.Status.OUTPUT
        }

        releaseJobAction.update(eventMock)

        assertSoftly {
          isEnabledNewValue shouldBe false
          isVisibleNewValue shouldBe null
        }
      }

      should("show the Release Job action as disabled cause the cached job status holder is null") {
        every { logFetcherMock.getCachedJobStatus() } returns null

        releaseJobAction.update(eventMock)

        assertSoftly {
          isEnabledNewValue shouldBe false
          isVisibleNewValue shouldBe null
        }
      }

      should("not show the Release Job action cause jobs console view is not initialized yet") {
        every { eventMock.getData(JOBS_LOG_VIEW) } returns null

        releaseJobAction.update(eventMock)

        assertSoftly {
          isEnabledNewValue shouldBe false
          isVisibleNewValue shouldBe false
        }
      }
    }
  }
})
