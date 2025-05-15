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
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.JesExplorerView
import org.zowe.explorer.explorer.ui.JobNode
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.ui.build.jobs.JOB_ADDED_TOPIC
import org.zowe.explorer.ui.build.jobs.JobHandler
import org.zowe.explorer.utils.subscribe
import org.zowe.kotlinsdk.Job
import org.zowe.kotlinsdk.SubmitJobRequest
import java.util.UUID

class ViewJobActionTestCase : AppInitShouldSpec("explorer/actions/ViewJobAction", {
  lateinit var currentTestUuid: UUID

  beforeSpec {
    currentTestUuid = AppInitShouldSpec.currentTestUuid ?: throw Exception("Test UUID must be defined before the spec run")
  }

  context("all functions") {
    var didTryToGetAttributes = false
    var didViewJob = false
    var isEnabledAndVisibleNewValue: Boolean? = null
    var isVisibleNewValue: Boolean? = null

    val projectMock = ProjectManager.getInstance().defaultProject
    val jesExplorerViewMock = mockk<JesExplorerView>()
    val eventMock = mockk<AnActionEvent> {
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

    val remoteJobAttributes = mockk<RemoteJobAttributes> {
      every { requesters } returns mutableListOf(
        mockk {
          every { connectionConfig } returns mockk()
        }
      )
      every { jobInfo } returns mockk()
    }
    every { remoteJobAttributes.clone() } returns remoteJobAttributes

    val dataOpsManager = DataOpsManager.getService()
    every {
      dataOpsManager.tryToGetAttributes(any())
    } answers {
      didTryToGetAttributes = true
      remoteJobAttributes
    }

    subscribe(
      project = projectMock,
      topic = JOB_ADDED_TOPIC,
      handler = object: JobHandler {
        override fun submitted(
          project: Project,
          connectionConfig: ConnectionConfig,
          mfFilePath: String,
          jobRequest: SubmitJobRequest
        ) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            fail("Unexpected event: submitted")
          }
        }

        override fun viewed(
          project: Project,
          connectionConfig: ConnectionConfig,
          mfFileName: String,
          jobStatus: Job
        ) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            didViewJob = true
          }
        }
      }
    )

    val viewJobAction = ViewJobAction()

    beforeEach {
      didTryToGetAttributes = false
      didViewJob = false
      isEnabledAndVisibleNewValue = null
      isVisibleNewValue = null

      every { eventMock.project } returns projectMock
      every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
        mockk {
          every { node } returns mockk<JobNode> {
            every { virtualFile } returns mockk {
              every { filenameInternal } returns "TEST"
            }
          }
        }
      )
      every { eventMock.getData(EXPLORER_VIEW) } returns jesExplorerViewMock
    }

    context("actionPerformed") {
      should("open the selected job for view") {
        viewJobAction.actionPerformed(eventMock)

        assertSoftly {
          didTryToGetAttributes shouldBe true
          didViewJob shouldBe true
        }
      }

      should("not open the selected job for view as the project is not initialized") {
        every { eventMock.project } returns null

        viewJobAction.actionPerformed(eventMock)

        assertSoftly {
          didTryToGetAttributes shouldBe true
          didViewJob shouldBe false
        }
      }

      should("not open the selected job for view as the selected node's virtual file is null") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<JobNode> {
              every { virtualFile } returns null
            }
          }
        )

        viewJobAction.actionPerformed(eventMock)

        assertSoftly {
          didTryToGetAttributes shouldBe false
          didViewJob shouldBe false
        }
      }

      should("not open the selected job for view as there is no selected nodes") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf()

        viewJobAction.actionPerformed(eventMock)

        assertSoftly {
          didTryToGetAttributes shouldBe false
          didViewJob shouldBe false
        }
      }

      should("not open the selected job for view as the JES Explorer view is not initialized yet") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        viewJobAction.actionPerformed(eventMock)

        assertSoftly {
          didTryToGetAttributes shouldBe false
          didViewJob shouldBe false
        }
      }
    }

    context("update") {
      should("show the View Job action") {
        viewJobAction.update(eventMock)

        assertSoftly {
          isEnabledAndVisibleNewValue shouldBe null
          isVisibleNewValue shouldBe true
        }
      }

      should("not show the View Job action when not a job node is selected") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk() }
        )

        viewJobAction.update(eventMock)

        assertSoftly {
          isEnabledAndVisibleNewValue shouldBe null
          isVisibleNewValue shouldBe false
        }
      }

      should("not show the View Job action when there is more than one node selected") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk() },
          mockk { every { node } returns mockk() }
        )

        viewJobAction.update(eventMock)

        assertSoftly {
          isEnabledAndVisibleNewValue shouldBe null
          isVisibleNewValue shouldBe false
        }
      }

      should("not show the View Job action when the JES explorer is not initialized yet") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        viewJobAction.update(eventMock)

        assertSoftly {
          isEnabledAndVisibleNewValue shouldBe false
          isVisibleNewValue shouldBe null
        }
      }
    }
  }
})
