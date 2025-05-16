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

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.operations.jobs.SubmitJobOperation
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.ui.build.jobs.JOB_ADDED_TOPIC
import org.zowe.explorer.ui.build.jobs.JobHandler
import org.zowe.explorer.utils.subscribe
import org.zowe.kotlinsdk.Job
import org.zowe.kotlinsdk.SubmitJobRequest
import java.util.UUID

class SubmitJobToolbarActionTestSpec : AppInitShouldSpec("explorer/actions/SubmitJobToolbarAction", {
  lateinit var currentTestUuid: UUID

  beforeSpec {
    currentTestUuid = AppInitShouldSpec.currentTestUuid ?: throw Exception("Test UUID must be defined before the spec run")
  }

  context("all functions") {
    var didPerformOperation = false
    var didJobSubmit = false
    var didShowErrorNotification = false
    var isEnabledAndVisibleNewValue: Boolean? = null

    val projectMock = ProjectManager.getInstance().defaultProject
    val editorMock = mockk<Editor>()
    val editorVFileMock = mockk<VirtualFile> {
      every { name } returns "TESTJOB"
    }
    val eventMock = mockk<AnActionEvent> {
      every { project } returns projectMock
      every { presentation } returns mockk {
        every {
          isEnabledAndVisible = any()
        } answers {
          isEnabledAndVisibleNewValue = firstArg<Boolean>()
        }
      }
    }

    val dataOpsManager = DataOpsManager.getService()

    val notificationsService = NotificationsService.getService()
    every {
      notificationsService
        .notifyError(any(), any(), any(), any(), any())
    } answers {
      didShowErrorNotification = true
    }

    subscribe(
      project = projectMock,
      topic = JOB_ADDED_TOPIC,
      handler = object: JobHandler {
        override fun submitted(project: Project, connectionConfig: ConnectionConfig, mfFilePath: String, jobRequest: SubmitJobRequest) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            didJobSubmit = true
          }
        }

        override fun viewed(project: Project, connectionConfig: ConnectionConfig, mfFileName: String, jobStatus: Job) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            fail("Unexpected event: viewed")
          }
        }
      }
    )

    val submitJobToolbarAction = SubmitJobToolbarAction()

    beforeEach {
      didPerformOperation = false
      didJobSubmit = false
      didShowErrorNotification = false
      isEnabledAndVisibleNewValue = null

      every { editorVFileMock.parent } returns mockk {
        every { path } returns "/test"
      }
      every { editorVFileMock.isWritable } returns true
      every { editorMock.document } returns mockk {
        every { text } returns "test content"
      }
      every { eventMock.getData(CommonDataKeys.EDITOR) } returns editorMock
      every { eventMock.getData(CommonDataKeys.VIRTUAL_FILE) } returns editorVFileMock

      every {
        dataOpsManager.performOperation(any<Operation<Any>>(), any())
      } answers {
        when (val operation = firstArg<Operation<*>>()) {
          is SubmitJobOperation -> {
            didPerformOperation = true
            mockk<SubmitJobRequest> {
              every { jobname } returns "TEST"
            }
          }

          else -> fail("Unexpected operation: $operation")
        }
      }
      every {
        dataOpsManager.tryToGetAttributes(any())
      } answers {
        if (firstArg<VirtualFile>() == editorVFileMock) {
          null
        } else {
          mockk<RemoteJobAttributes> {
            every { requesters } returns mutableListOf(
              mockk {
                every { connectionConfig } returns mockk()
              }
            )
          }
        }
      }
    }

    context("actionPerformed") {
      should("submit a JCL job, edited in the editor") {
        submitJobToolbarAction.actionPerformed(eventMock)

        assertSoftly {
          didPerformOperation shouldBe true
          didJobSubmit shouldBe true
          didShowErrorNotification shouldBe false
        }
      }

      should("produce an error during a JCL job submit") {
        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any())
        } answers {
          didPerformOperation = true
          throw Throwable("Test exception")
        }

        submitJobToolbarAction.actionPerformed(eventMock)

        assertSoftly {
          didPerformOperation shouldBe true
          didJobSubmit shouldBe false
          didShowErrorNotification shouldBe true
        }
      }

      should("not submit a JCL job as there is no content in the editor") {
        every { editorMock.document } returns mockk {
          every { text } returns ""
        }

        submitJobToolbarAction.actionPerformed(eventMock)

        assertSoftly {
          didPerformOperation shouldBe false
          didJobSubmit shouldBe false
          didShowErrorNotification shouldBe false
        }
      }

      should("not submit a JCL job as when it is impossible to find the editor's virtual file") {
        every { eventMock.getData(CommonDataKeys.VIRTUAL_FILE) } returns null

        submitJobToolbarAction.actionPerformed(eventMock)

        assertSoftly {
          didPerformOperation shouldBe false
          didJobSubmit shouldBe false
          didShowErrorNotification shouldBe false
        }
      }

      should("not submit a JCL job as when it is impossible to find the editor's instance") {
        every { eventMock.getData(CommonDataKeys.EDITOR) } returns null

        submitJobToolbarAction.actionPerformed(eventMock)

        assertSoftly {
          didPerformOperation shouldBe false
          didJobSubmit shouldBe false
          didShowErrorNotification shouldBe false
        }
      }
    }

    context("update") {
      should("show the Submit JCL editor action") {
        submitJobToolbarAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe true }
      }

      should("not show the Submit JCL editor action cause the editor's virtual file is not writable") {
        every { editorVFileMock.isWritable } returns false

        submitJobToolbarAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe false }
      }

      should("not show the Submit JCL editor action cause the file being edited is not an instance of a job") {
        every {
          dataOpsManager.tryToGetAttributes(any())
        } answers {
          if (firstArg<VirtualFile>() == editorVFileMock) null else mockk()
        }

        submitJobToolbarAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe false }
      }

      should("not show the Submit JCL editor action cause the file being edited is an existing file") {
        every { dataOpsManager.tryToGetAttributes(any()) } returns mockk()

        submitJobToolbarAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe false }
      }

      should("not show the Submit JCL editor action cause the file being edited does not have a job parent file") {
        every { editorVFileMock.parent } returns null

        submitJobToolbarAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe null }
      }

      should("not show the Submit JCL editor action cause there is no virtual file in the editor") {
        every { eventMock.getData(CommonDataKeys.VIRTUAL_FILE) } returns null

        submitJobToolbarAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe false }
      }
    }
  }
})
