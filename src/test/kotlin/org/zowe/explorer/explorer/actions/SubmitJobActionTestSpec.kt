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
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.content.synchronizer.checkFileForSync
import org.zowe.explorer.dataops.operations.jobs.SubmitJobOperation
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.JesWorkingSet
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.FileLikeDatasetNode
import org.zowe.explorer.explorer.ui.JesWsNode
import org.zowe.explorer.explorer.ui.UssFileNode
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.ui.build.jobs.JOB_ADDED_TOPIC
import org.zowe.explorer.ui.build.jobs.JobHandler
import org.zowe.explorer.utils.subscribe
import org.zowe.kotlinsdk.Job
import org.zowe.kotlinsdk.SubmitJobRequest
import java.util.UUID

class SubmitJobActionTestSpec : AppInitShouldSpec("explorer/actions/SubmitJobAction", {
  lateinit var currentTestUuid: UUID

  beforeSpec {
    currentTestUuid = AppInitShouldSpec.currentTestUuid ?: throw Exception("Test UUID must be defined before the spec run")
  }

  context("all functions") {
    var didSynchronizeWithRemote = false
    var didPerformOperation = false
    var didShowInfoNotification = false
    var didShowErrorNotification = false
    var didJobSubmit = false
    var isEnabledAndVisibleNewValue: Boolean? = null
    var isVisibleNewValue: Boolean? = null

    val projectMock = ProjectManager.getInstance().defaultProject
    val fileExplorerViewMock = mockk<FileExplorerView> {
      every { explorer } returns mockk {
        every {
          showNotification(any(), any(), any(), any())
        } answers {
          val notificationType = arg<NotificationType>(2)
          when (notificationType) {
            NotificationType.INFORMATION -> {
              didShowInfoNotification = true
            }
            else -> fail("Unexpected notification type: $notificationType")
          }
        }
      }
    }
    val eventMock = mockk<AnActionEvent> {
      every { project } returns projectMock
      every { presentation } returns mockk {
        every {
          isEnabledAndVisible = any()
        } answers {
          isEnabledAndVisibleNewValue = firstArg<Boolean>()
        }
        every {
          isVisible = any()
        } answers {
          isVisibleNewValue = firstArg<Boolean>()
        }
      }
    }

    mockkStatic(::checkFileForSync)

    val configService = ConfigService.getService()

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

    val submitJobAction = SubmitJobAction()

    beforeEach {
      didSynchronizeWithRemote = false
      didPerformOperation = false
      didShowInfoNotification = false
      didShowErrorNotification = false
      didJobSubmit = false
      isEnabledAndVisibleNewValue = null
      isVisibleNewValue = null

      every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
        mockk {
          every { node } returns mockk<FileLikeDatasetNode> {
            every { virtualFile } returns mockk {
              every { name } returns "TEST"
            }
            every { unit } returns mockk<FilesWorkingSet> {
              every { connectionConfig } returns mockk()
            }
          }
          every { attributes } returns mockk<RemoteDatasetAttributes> {
            every { hasDsOrg } returns true
          }
        }
      )
      every { eventMock.getData(EXPLORER_VIEW) } returns fileExplorerViewMock

      every { checkFileForSync(any(), any(), any()) } returns false

      every { configService.isAutoSyncEnabled } returns true

      every { dataOpsManager.isSyncSupported(any()) } returns true
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
      every { dataOpsManager.tryToGetAttributes(any()) } returns mockk<RemoteDatasetAttributes> {
        every { name } returns "TEST"
      }
      every { dataOpsManager.getContentSynchronizer(any()) } returns mockk {
        every {
          synchronizeWithRemote(any(), any())
        } answers {
          didSynchronizeWithRemote = true
        }
      }
    }

    context("actionPerformed") {
      should("submit a JCL job from a PS dataset") {
        submitJobAction.actionPerformed(eventMock)

        assertSoftly {
          didSynchronizeWithRemote shouldBe true
          didPerformOperation shouldBe true
          didShowInfoNotification shouldBe true
          didShowErrorNotification shouldBe false
          didJobSubmit shouldBe true
        }
      }

      should("submit a JCL job from a PS dataset and finish with failure due to the remote error") {
        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any())
        } answers {
          didPerformOperation = true
          throw Throwable("Test exception")
        }

        submitJobAction.actionPerformed(eventMock)

        assertSoftly {
          didSynchronizeWithRemote shouldBe true
          didPerformOperation shouldBe true
          didShowInfoNotification shouldBe false
          didShowErrorNotification shouldBe true
          didJobSubmit shouldBe false
        }
      }

      should("not submit a JCL job cause there is no attributes for the selected node") {
        every { dataOpsManager.tryToGetAttributes(any()) } returns null

        submitJobAction.actionPerformed(eventMock)

        assertSoftly {
          didSynchronizeWithRemote shouldBe true
          didPerformOperation shouldBe false
          didShowInfoNotification shouldBe false
          didShowErrorNotification shouldBe true
          didJobSubmit shouldBe false
        }
      }

      should("submit a JCL job from a non-syncable dataset and the autosync option is disabled") {
        every { configService.isAutoSyncEnabled } returns false

        every { dataOpsManager.isSyncSupported(any()) } returns false

        submitJobAction.actionPerformed(eventMock)

        assertSoftly {
          didSynchronizeWithRemote shouldBe false
          didPerformOperation shouldBe true
          didShowInfoNotification shouldBe true
          didShowErrorNotification shouldBe false
          didJobSubmit shouldBe true
        }
      }

      should("submit a JCL job from a non-syncable dataset and the autosync option is disabled") {
        every { dataOpsManager.isSyncSupported(any()) } returns false

        submitJobAction.actionPerformed(eventMock)

        assertSoftly {
          didSynchronizeWithRemote shouldBe false
          didPerformOperation shouldBe true
          didShowInfoNotification shouldBe true
          didShowErrorNotification shouldBe false
          didJobSubmit shouldBe true
        }
      }

      should("submit a JCL job from a dataset without the autosync option enabled") {
        every { configService.isAutoSyncEnabled } returns false

        submitJobAction.actionPerformed(eventMock)

        assertSoftly {
          didSynchronizeWithRemote shouldBe false
          didPerformOperation shouldBe true
          didShowInfoNotification shouldBe true
          didShowErrorNotification shouldBe false
          didJobSubmit shouldBe true
        }
      }

      should("submit a JCL job from a dataset, trying to synchronize, but the content synchronizer is not found") {
        every { dataOpsManager.getContentSynchronizer(any()) } returns null

        submitJobAction.actionPerformed(eventMock)

        assertSoftly {
          didSynchronizeWithRemote shouldBe false
          didPerformOperation shouldBe true
          didShowInfoNotification shouldBe true
          didShowErrorNotification shouldBe false
          didJobSubmit shouldBe true
        }
      }

      should("not submit a JCL job from a dataset when it is beign synced") {
        every { checkFileForSync(any(), any(), any()) } returns true

        submitJobAction.actionPerformed(eventMock)

        assertSoftly {
          didSynchronizeWithRemote shouldBe false
          didPerformOperation shouldBe false
          didShowInfoNotification shouldBe false
          didShowErrorNotification shouldBe false
          didJobSubmit shouldBe false
        }
      }

      should("not submit a JCL job from a non-supported entity") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<JesWsNode> {
              every { unit } returns mockk<JesWorkingSet>()
            }
          }
        )

        submitJobAction.actionPerformed(eventMock)

        assertSoftly {
          didSynchronizeWithRemote shouldBe false
          didPerformOperation shouldBe false
          didShowInfoNotification shouldBe false
          didShowErrorNotification shouldBe false
          didJobSubmit shouldBe false
        }
      }

      should("not submit a JCL job cause there is no selected node") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf()

        submitJobAction.actionPerformed(eventMock)

        assertSoftly {
          didSynchronizeWithRemote shouldBe false
          didPerformOperation shouldBe false
          didShowInfoNotification shouldBe false
          didShowErrorNotification shouldBe false
          didJobSubmit shouldBe false
        }
      }

      should("not submit a JCL job cause the File Explorer view is not initialized yet") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        submitJobAction.actionPerformed(eventMock)

        assertSoftly {
          didSynchronizeWithRemote shouldBe false
          didPerformOperation shouldBe false
          didShowInfoNotification shouldBe false
          didShowErrorNotification shouldBe false
          didJobSubmit shouldBe false
        }
      }
    }

    context("update") {
      should("show the Submit Job action") {
        submitJobAction.update(eventMock)

        assertSoftly {
          isVisibleNewValue shouldBe true
          isEnabledAndVisibleNewValue shouldBe null
        }
      }

      should("show the Submit Job action for a USS file") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<UssFileNode>()
            every { attributes } returns mockk()
          }
        )

        submitJobAction.update(eventMock)

        assertSoftly {
          isVisibleNewValue shouldBe true
          isEnabledAndVisibleNewValue shouldBe null
        }
      }

      should("show the Submit Job action for a dataset member") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<FileLikeDatasetNode>()
            every { attributes } returns mockk<RemoteMemberAttributes>()
          }
        )

        submitJobAction.update(eventMock)

        assertSoftly {
          isVisibleNewValue shouldBe true
          isEnabledAndVisibleNewValue shouldBe null
        }
      }

      should("not show the Submit Job action for a migrated dataset") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<FileLikeDatasetNode>()
            every { attributes } returns mockk<RemoteDatasetAttributes> {
              every { hasDsOrg } returns false
            }
          }
        )

        submitJobAction.update(eventMock)

        assertSoftly {
          isVisibleNewValue shouldBe false
          isEnabledAndVisibleNewValue shouldBe null
        }
      }

      should("not show the Submit Job action for a dataset with unsupported attributes") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<FileLikeDatasetNode>()
            every { attributes } returns mockk()
          }
        )

        submitJobAction.update(eventMock)

        assertSoftly {
          isVisibleNewValue shouldBe false
          isEnabledAndVisibleNewValue shouldBe null
        }
      }

      should("not show the Submit Job action when more than one node is selected") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(mockk(), mockk())

        submitJobAction.update(eventMock)

        assertSoftly {
          isVisibleNewValue shouldBe null
          isEnabledAndVisibleNewValue shouldBe false
        }
      }

      should("not show the Submit Job action cause the File Explorer view is not initialized yet") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        submitJobAction.update(eventMock)

        assertSoftly {
          isVisibleNewValue shouldBe null
          isEnabledAndVisibleNewValue shouldBe false
        }
      }
    }
  }
})
