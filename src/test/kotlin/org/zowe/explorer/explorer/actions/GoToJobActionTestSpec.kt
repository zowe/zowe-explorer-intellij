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

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.impl.ContentImpl
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.JobsFilter
import org.zowe.explorer.config.ws.ui.jes.JesWsDialog
import org.zowe.explorer.dataops.log.JobProcessInfo
import org.zowe.explorer.explorer.*
import org.zowe.explorer.explorer.actions.GoToJobAction.Companion.JOB_FILTER_CREATED_TITLE
import org.zowe.explorer.explorer.actions.GoToJobAction.Companion.JOB_FILTER_NOT_CREATED_TITLE
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.ui.build.jobs.JOBS_LOG_VIEW
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.runInEdtAndWait
import org.zowe.kotlinsdk.Job
import java.util.*
import java.util.stream.Stream
import javax.swing.JComponent

class GoToJobActionTestSpec : AppInitShouldSpec("explorer/actions/GoToJobAction", {
  context("all functions") {
    // defined common mocks
    val connectionConfigMock = mockk<ConnectionConfig> {
      every { uuid } returns "UUID_TEST"
    }
    val projectMock = ProjectManager.getInstance().defaultProject
    val actionEventMock = mockk<AnActionEvent> {
      every { getData(JOBS_LOG_VIEW) } returns mockk {
        every { jobLogInfo } returns JobProcessInfo("JOB_ID", "TEST_JOB", connectionConfigMock)
        every { getConnectionConfig() } returns connectionConfigMock
        every { getJobLogger() } returns mockk {
          every { logFetcher } returns mockk {
            every { getCachedJobStatus() } returns mockk {
              every { status } returns Job.Status.OUTPUT
            }
          }
        }
      }
      every { project } returns projectMock
    }
    val myFsTreeStructureMock = mockk<CommonExplorerTreeStructure<Explorer<ConnectionConfig, JesWorkingSetImpl>>>()
    val jesExplorerMock = mockk<Explorer<ConnectionConfig, JesWorkingSetImpl>>(relaxUnitFun = true)
    val contentMock = mockk<ContentImpl>()
    val contentManagerMock = mockk<ContentManager> {
      every { getContent(any<JComponent>()) } returns contentMock
      every { setSelectedContent(any(), any()) } just Runs
    }

    val uiComponentManagerService = UIComponentManager.getService()
    every {
      uiComponentManagerService.getExplorerContentProvider(JesExplorer::class.java)
    } returns mockk<JesExplorerContentProvider> {
      every { getExplorerView(any()) } returns mockk(relaxUnitFun = true) {
        every { myFsTreeStructure } returns myFsTreeStructureMock
        every { explorer } returns jesExplorerMock
        every { myStructure } returns mockk(relaxUnitFun = true)
        every { myTree } returns mockk()
      }
    }

    mockkObject(ToolWindowManager)
    every { ToolWindowManager.getInstance(any()) } returns mockk {
      every { getToolWindow(any()) } returns mockk {
        every { contentManager } returns contentManagerMock
      }
    }

    // class under test
    val classUnderTest = spyk(GoToJobAction(), "Go To Job", recordPrivateCalls = true)

    beforeEach {
      clearMocks(jesExplorerMock, answers = false, recordedCalls = true, verificationMarks = true)
    }

    should("createJesWorkingSetWithDefinedFilter_whenActionPerformed_givenJobIdAndNoJesWSNodesFound") {
      var isNewConnectionAdded = false

      val configService = ConfigService.getService()
      every { configService.crudable } returns mockk {
        every { nextUniqueValue<Any, String>(any<Class<*>>()) } returns "test"
        every { getAll(any<Class<*>>()) } answers { Stream.of() }
        every {
          add(any())
        } answers {
          isNewConnectionAdded = true
          Optional.empty()
        }
      }
      val expectedNotificationMessage =
        "Job Filter(s): JobID=JOB_ID, successfully created on connection: $connectionConfigMock"

      mockkConstructor(JesWsDialog::class)
      every { anyConstructed<JesWsDialog>().showAndGet() } returns true
      every { myFsTreeStructureMock.findByPredicate(any()) } returns mutableListOf<JesWsNode>()

      runInEdtAndWait {
        classUnderTest.actionPerformed(actionEventMock)
      }

      verify {
        jesExplorerMock.showNotification(
          title = JOB_FILTER_CREATED_TITLE,
          content = expectedNotificationMessage,
          project = projectMock
        )
      }
      verify { contentManagerMock.setSelectedContent(contentMock, true) }
      assertSoftly {
        isNewConnectionAdded shouldBe true
      }
    }

    should("createJobFilterInExistingJesWorkingSet_whenActionPerformed_givenJobIdAndJesWsDoesNotContainFilter") {
      val jobFilterToSaveExpected = JobsFilter("", "", "JOB_ID")
      val expectedNotificationMessage =
        "Job Filter(s): JobID=JOB_ID, successfully created in the working set JES_WS_TEST on connection: $connectionConfigMock"
      val jesFilterNodeForTest1 = mockk<JesFilterNode> {
        every { value } returns JobsFilter("ARST", "ARST*", "")
      }
      val jesFilterNodeForTest2 = mockk<JesFilterNode> {
        every { value } returns JobsFilter("", "", "JOB_ID_TEST")
      }
      val jesWorkingSet = mockk<JesWorkingSetImpl>(relaxUnitFun = true) {
        every { name } returns "test"
        every { connectionConfig } returns connectionConfigMock
      }

      every {
        myFsTreeStructureMock.findByPredicate(any())
      } returns mutableListOf(
        mockk<JesWsNode> {
          every { name } returns "JES_WS_TEST"
          every { children } returns mutableListOf(jesFilterNodeForTest1, jesFilterNodeForTest2)
          every { unit } returns jesWorkingSet
        }
      )

      mockkConstructor(AddJobsFilterDialog::class)
      every { anyConstructed<AddJobsFilterDialog>().showAndGet() } returns true

      runInEdtAndWait {
        classUnderTest.actionPerformed(actionEventMock)
      }

      verify { jesWorkingSet.addMask(jobFilterToSaveExpected) }
      verify {
        jesExplorerMock.showNotification(
          title = JOB_FILTER_CREATED_TITLE,
          content = expectedNotificationMessage,
          project = projectMock
        )
      }
      verify { contentManagerMock.setSelectedContent(contentMock, true) }
    }

    should("NotCreateJobFilterInExistingJesWorkingSet_whenActionPerformed_givenJobIdAndJesWsAlreadyContainFilter") {
      val jesFilterNodeForTest1 = mockk<JesFilterNode> {
        every { value } returns JobsFilter("ARST", "ARST*", "")
      }
      val jesFilterNodeForTest2 = mockk<JesFilterNode> {
        every { value } returns JobsFilter("", "", "JOB_ID")
      }
      val jesWsNodeForTest = mockk<JesWsNode> {
        every { name } returns "JES_WS_TEST"
        every { children } returns mutableListOf(jesFilterNodeForTest1, jesFilterNodeForTest2)
        every { unit } returns mockk {
          every { connectionConfig } returns connectionConfigMock
        }
      }
      val expectedNotificationMessage =
        "Cannot create job filter, because all working sets ([JES_WS_TEST]) on connection $connectionConfigMock already contain job filter with jobId = JOB_ID"


      every { myFsTreeStructureMock.findByPredicate(any()) } returns mutableListOf(jesWsNodeForTest)

      runInEdtAndWait {
        classUnderTest.actionPerformed(actionEventMock)
      }

      verify {
        jesExplorerMock.showNotification(
          title = JOB_FILTER_NOT_CREATED_TITLE,
          content = expectedNotificationMessage,
          project = projectMock
        )
      }
      verify { contentManagerMock.setSelectedContent(contentMock, true) }
    }

    should("returnThreadEDT_whenGetActionUpdateThread") {
      val expected = ActionUpdateThread.EDT
      val actual = classUnderTest.actionUpdateThread

      assertSoftly { actual shouldBe expected }
    }

    should("returnTrue_whenIsDumbAware") {
      val actual = classUnderTest.isDumbAware

      assertSoftly { actual shouldBe true }
    }

    should("updateActionState_whenUpdate") {
      every { actionEventMock.presentation } returns Presentation()

      classUnderTest.update(actionEventMock)

      assertSoftly {
        actionEventMock.presentation.isEnabled shouldBe true
        actionEventMock.presentation.isVisible shouldBe true
      }
    }
  }

})
