/*
 * Copyright (c) 2020-2024 IBA Group.
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

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.attributes.RemoteSpoolFileAttributes
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.runInEdtAndWait
import org.zowe.kotlinsdk.Job
import org.zowe.kotlinsdk.SpoolFile

class GetJobPropertiesActionTestSpec : AppInitShouldSpec("explorer/actions/GetJobPropertiesAction", {
  context("all functions") {
    var didCallTryToGetAttributes = false
    var didTriggerJobPropertiesDialog = false
    var didTriggerSpoolFilePropertiesDialog = false
    var didChangeIsEnabledAndVisible = false
    var didChangeIsVisible = false
    var isEnabledAndVisibleNewValue: Boolean? = null
    var isVisibleNewValue: Boolean? = null

    val jesExplorerViewMock = mockk<JesExplorerView>()
    val eventMock = mockk<AnActionEvent> {
      every { project } returns mockk()
      every { presentation } returns mockk {
        every {
          isEnabledAndVisible = any()
        } answers {
          didChangeIsEnabledAndVisible = true
          isEnabledAndVisibleNewValue = firstArg<Boolean>()
        }
        every {
          isVisible = any<Boolean>()
        } answers {
          didChangeIsVisible = true
          isVisibleNewValue = firstArg<Boolean>()
        }
      }
    }

    val getJobPropertiesAction = GetJobPropertiesAction()

    val dataOpsManager = DataOpsManager.getService()

    mockkConstructor(JobPropertiesDialog::class)
    every {
      anyConstructed<JobPropertiesDialog>().showAndGet()
    } answers {
      didTriggerJobPropertiesDialog = true
      true
    }

    mockkConstructor(SpoolFilePropertiesDialog::class)
    every {
      anyConstructed<SpoolFilePropertiesDialog>().showAndGet()
    } answers {
      didTriggerSpoolFilePropertiesDialog = true
      true
    }

    beforeEach {
      didCallTryToGetAttributes = false
      didTriggerJobPropertiesDialog = false
      didTriggerSpoolFilePropertiesDialog = false
      didChangeIsEnabledAndVisible = false
      didChangeIsVisible = false
      isEnabledAndVisibleNewValue = null
      isVisibleNewValue = null

      every { jesExplorerViewMock.mySelectedNodesData } returns listOf()
      every { eventMock.getData(EXPLORER_VIEW) } returns jesExplorerViewMock

      every {
        dataOpsManager.tryToGetAttributes(any())
      } answers {
        didCallTryToGetAttributes = true
        mockk {
          every { clone() } returns mockk()
        }
      }
    }

    context("actionPerformed") {
      should("get job properties") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<JobNode> {
              every { virtualFile } returns mockk()
            }
          }
        )

        every {
          dataOpsManager.tryToGetAttributes(any())
        } answers {
          didCallTryToGetAttributes = true
          mockk {
            every { clone() } returns mockk<RemoteJobAttributes>(relaxed = true) {
              every { jobInfo } returns Job(
                jobId = "TESTJID",
                jobName = "TESTJOB",
                owner = "TESTOWNR",
                type = Job.JobType.JOB,
                url = "test",
                filesUrl = "test",
                phase = 0,
                phaseName = "TEST"
              )
            }
          }
        }

        runInEdtAndWait {
          getJobPropertiesAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didCallTryToGetAttributes shouldBe true
          didTriggerJobPropertiesDialog shouldBe true
          didTriggerSpoolFilePropertiesDialog shouldBe false
        }
      }

      should("get spool file properties") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<SpoolFileNode> {
              every { virtualFile } returns mockk()
            }
          }
        )

        every {
          dataOpsManager.tryToGetAttributes(any())
        } answers {
          didCallTryToGetAttributes = true
          mockk {
            every { clone() } returns mockk<RemoteSpoolFileAttributes> {
              every { info } returns SpoolFile(
                jobname = "TESTJOB",
                recfm = "F",
                byteCount = 0,
                recordCount = 1,
                fileClass = "TEST",
                jobId = "TESTJID",
                id = 0,
                ddName = "TEST.DD",
                recordsUrl = "test",
                recordLength = 80
              )
            }
          }
        }

        runInEdtAndWait {
          getJobPropertiesAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didCallTryToGetAttributes shouldBe true
          didTriggerJobPropertiesDialog shouldBe false
          didTriggerSpoolFilePropertiesDialog shouldBe true
        }
      }

      should("not show a job properties dialog cause the selected node is not a job or a spool file node") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk {
              every { virtualFile } returns mockk()
            }
          }
        )

        every {
          dataOpsManager.tryToGetAttributes(any())
        } answers {
          didCallTryToGetAttributes = true
          mockk {
            every { clone() } returns mockk()
          }
        }

        runInEdtAndWait {
          getJobPropertiesAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didCallTryToGetAttributes shouldBe true
          didTriggerJobPropertiesDialog shouldBe false
          didTriggerSpoolFilePropertiesDialog shouldBe false
        }
      }

      should("not show a job properties dialog cause the selected node has no attributes") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk {
              every { virtualFile } returns mockk()
            }
          }
        )

        every {
          dataOpsManager.tryToGetAttributes(any())
        } answers {
          didCallTryToGetAttributes = true
          null
        }

        runInEdtAndWait {
          getJobPropertiesAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didCallTryToGetAttributes shouldBe true
          didTriggerJobPropertiesDialog shouldBe false
          didTriggerSpoolFilePropertiesDialog shouldBe false
        }
      }

      should("not show a job properties dialog cause the selected node has no virtual file") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk {
              every { virtualFile } returns null
            }
          }
        )

        runInEdtAndWait {
          getJobPropertiesAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didCallTryToGetAttributes shouldBe false
          didTriggerJobPropertiesDialog shouldBe false
          didTriggerSpoolFilePropertiesDialog shouldBe false
        }
      }

      should("not show a job properties dialog cause there is no selected node") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf()

        runInEdtAndWait {
          getJobPropertiesAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didCallTryToGetAttributes shouldBe false
          didTriggerJobPropertiesDialog shouldBe false
          didTriggerSpoolFilePropertiesDialog shouldBe false
        }
      }

      should("not show a job properties dialog cause the JES Explorer view is not initialized yet") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        runInEdtAndWait {
          getJobPropertiesAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didCallTryToGetAttributes shouldBe false
          didTriggerJobPropertiesDialog shouldBe false
          didTriggerSpoolFilePropertiesDialog shouldBe false
        }
      }
    }

    context("update") {
      should("show the Properties action for a job node") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk<JobNode>() }
        )

        getJobPropertiesAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          didChangeIsVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe null
          isVisibleNewValue shouldBe true
        }
      }

      should("show the Properties action for a spool file node") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk<SpoolFileNode>() }
        )

        getJobPropertiesAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          didChangeIsVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe null
          isVisibleNewValue shouldBe true
        }
      }

      should("not show the Properties action for a non-job and non-spool file node") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk() }
        )

        getJobPropertiesAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          didChangeIsVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe null
          isVisibleNewValue shouldBe false
        }
      }

      should("not show the Properties action when there is more than one node selected") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk() },
          mockk { every { node } returns mockk() }
        )

        getJobPropertiesAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          didChangeIsVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe null
          isVisibleNewValue shouldBe false
        }
      }

      should("not show the Properties action when there is no nodes selected") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf()

        getJobPropertiesAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          didChangeIsVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe null
          isVisibleNewValue shouldBe false
        }
      }

      should("not show the Properties action when the JES Explorer view is not initialized yet") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        getJobPropertiesAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          didChangeIsVisible shouldBe false
          isEnabledAndVisibleNewValue shouldBe false
          isVisibleNewValue shouldBe null
        }
      }
    }
  }
})
