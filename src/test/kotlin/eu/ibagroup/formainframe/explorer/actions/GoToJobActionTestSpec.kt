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
 */

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.impl.ContentImpl
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.config.ws.ui.AbstractWsDialog
import eu.ibagroup.formainframe.config.ws.ui.jes.JesWsDialog
import eu.ibagroup.formainframe.dataops.log.AbstractMFLoggerBase
import eu.ibagroup.formainframe.dataops.log.JobLogFetcher
import eu.ibagroup.formainframe.dataops.log.JobProcessInfo
import eu.ibagroup.formainframe.explorer.*
import eu.ibagroup.formainframe.explorer.actions.GoToJobAction.Companion.JOB_FILTER_CREATED_TITLE
import eu.ibagroup.formainframe.explorer.actions.GoToJobAction.Companion.JOB_FILTER_NOT_CREATED_TITLE
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestConfigServiceImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestUIComponentManager
import eu.ibagroup.formainframe.ui.build.jobs.JOBS_LOG_VIEW
import eu.ibagroup.formainframe.ui.build.jobs.JobBuildTreeView
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.zowe.kotlinsdk.Job
import java.util.*
import java.util.stream.Stream
import javax.swing.JComponent

class GoToJobActionTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("Go-To-Job Action test spec") {

    // defined common mocks
    val connectionConfigMock = mockk<ConnectionConfig>()
    val projectMock = ProjectManager.getInstance().defaultProject
    val actionEventMock = mockk<AnActionEvent>()
    val explorerContentProviderMock = mockk<JesExplorerContentProvider>()
    val consoleViewMock = mockk<JobBuildTreeView>()
    val jesExplorerMock = mockk<Explorer<ConnectionConfig, JesWorkingSetImpl>>()
    val jesExplorerViewMock = mockk<JesExplorerView>()
    val toolWindowManagerMock = mockk<ToolWindowManager>()
    val toolWindowMock = mockk<ToolWindow>()
    val contentManagerMock = mockk<ContentManager>()
    val contentMock = mockk<ContentImpl>()
    val myFsTreeStructureMock = mockk<CommonExplorerTreeStructure<Explorer<ConnectionConfig, JesWorkingSetImpl>>>()
    val myStructureMock =
      mockk<StructureTreeModel<CommonExplorerTreeStructure<Explorer<ConnectionConfig, JesWorkingSetImpl>>>>()
    val myTreeMock = mockk<Tree>()
    val uiComponentManagerService: TestUIComponentManager = UIComponentManager.getService() as TestUIComponentManager
    uiComponentManagerService.testInstance = object : TestUIComponentManager() {
      override fun <E : Explorer<*, *>> getExplorerContentProvider(
        clazz: Class<out E>
      ): ExplorerContentProvider<out ConnectionConfigBase, out Explorer<*, *>>? {
        return explorerContentProviderMock
      }
    }
    mockkObject(ToolWindowManager)

    // job process info for test
    val jobProcessInfo = JobProcessInfo("JOB_ID", "TEST_JOB", connectionConfigMock)

    // defined common mocks behavior
    every { explorerContentProviderMock.getExplorerView(any()) } returns jesExplorerViewMock
    every {
      jesExplorerMock.showNotification(
        any() as String,
        any() as String,
        any() as NotificationType,
        any() as Project
      )
    } just Runs
    every { contentManagerMock.getContent(any() as JComponent) } returns contentMock
    every { contentManagerMock.setSelectedContent(any(), any()) } just Runs
    every { toolWindowMock.contentManager } returns contentManagerMock
    every { ToolWindowManager.getInstance(any()) } returns toolWindowManagerMock
    every { toolWindowManagerMock.getToolWindow(any()) } returns toolWindowMock
    every { connectionConfigMock.uuid } returns "UUID_TEST"
    every { jesExplorerViewMock.myFsTreeStructure } returns myFsTreeStructureMock
    every { consoleViewMock.jobLogInfo } returns jobProcessInfo
    every { actionEventMock.getData(JOBS_LOG_VIEW) } returns consoleViewMock
    every { consoleViewMock.getConnectionConfig() } returns connectionConfigMock
    every { actionEventMock.project } returns projectMock
    every { jesExplorerViewMock.explorer } returns jesExplorerMock
    every { jesExplorerViewMock.myStructure } returns myStructureMock
    every { jesExplorerViewMock.myTree } returns myTreeMock

    // default mocks on tree
    every { myStructureMock.select(any(), any(), any()) } just Runs
    every { myStructureMock.expand(any(), any(), any()) } just Runs

    // class under test
    val classUnderTest = spyk(GoToJobAction(), "Go To Job", recordPrivateCalls = true)

    should("createJesWorkingSetWithDefinedFilter_whenActionPerformed_givenJobIdAndNoJesWSNodesFound") {
      var isNewConnectionAdded = false
      val configCrudable = (ConfigService.getService() as TestConfigServiceImpl).crudable
      val nextUniqueValueMock: (Class<*>) -> Any = configCrudable::nextUniqueValue
      val getAllMock: (Class<*>) -> Any = configCrudable::getAll
      every { nextUniqueValueMock(any()) } returns "test"
      every { getAllMock(any()) } returns Stream.empty<ConnectionConfig>()
      every { configCrudable.add(any()) } answers {
        isNewConnectionAdded = true
        Optional.empty()
      }
      val expectedNotificationMessage =
        "Job Filter(s): JobID=JOB_ID, successfully created on connection: $connectionConfigMock"
      val jesWsNodesTest = mutableListOf<JesWsNode>()
      mockkObject(AbstractWsDialog)
      every { AbstractWsDialog["initialize"](any<() -> Unit>()) } returns Unit
      mockkConstructor(JesWsDialog::class)
      every { anyConstructed<JesWsDialog>().showAndGet() } returns true
      every { myFsTreeStructureMock.findByPredicate(any()) } returns jesWsNodesTest

      runBlocking {
        withContext(Dispatchers.EDT) {
          classUnderTest.actionPerformed(actionEventMock)
        }
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
      clearMocks(jesExplorerMock, answers = false, recordedCalls = true, verificationMarks = true)
      val jobFilterToSaveExpected = JobsFilter("", "", "JOB_ID")
      val expectedNotificationMessage =
        "Job Filter(s): JobID=JOB_ID, successfully created in the working set JES_WS_TEST on connection: $connectionConfigMock"
      val jobFilter1 = JobsFilter("ARST", "ARST*", "")
      val jobFilter2 = JobsFilter("", "", "JOB_ID_TEST")
      val jesFilterNodeForTest1 = mockk<JesFilterNode>()
      val jesFilterNodeForTest2 = mockk<JesFilterNode>()
      val jesWsNodeForTest = mockk<JesWsNode>()
      val jesWorkingSet = mockk<JesWorkingSetImpl>()

      val jesFilterNodes = mutableListOf(jesFilterNodeForTest1, jesFilterNodeForTest2)

      every { jesFilterNodeForTest1.value } returns jobFilter1
      every { jesFilterNodeForTest2.value } returns jobFilter2
      every { jesWsNodeForTest.name } returns "JES_WS_TEST"
      every { jesWsNodeForTest.children } returns jesFilterNodes
      every { jesWsNodeForTest.unit } returns jesWorkingSet
      every { jesWorkingSet.connectionConfig } returns connectionConfigMock
      every { jesWorkingSet.addMask(any()) } just Runs

      val jesWsNodesTest = mutableListOf(jesWsNodeForTest)
      every { myFsTreeStructureMock.findByPredicate(any()) } returns jesWsNodesTest

      mockkObject(AddJobsFilterDialog)
      every { AddJobsFilterDialog["initialize"](any<() -> Unit>()) } returns Unit
      mockkConstructor(AddJobsFilterDialog::class)
      every { anyConstructed<AddJobsFilterDialog>().showAndGet() } returns true

      runBlocking {
        withContext(Dispatchers.EDT) {
          classUnderTest.actionPerformed(actionEventMock)
        }
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
      clearMocks(jesExplorerMock, answers = false, recordedCalls = true, verificationMarks = true)
      val jobFilter1 = JobsFilter("ARST", "ARST*", "")
      val jobFilter2 = JobsFilter("", "", "JOB_ID")
      val jesFilterNodeForTest1 = mockk<JesFilterNode>()
      val jesFilterNodeForTest2 = mockk<JesFilterNode>()
      val jesWsNodeForTest = mockk<JesWsNode>()
      val jesWorkingSet = mockk<JesWorkingSetImpl>()
      val jesWsNodesTest = mutableListOf(jesWsNodeForTest)
      val expectedNotificationMessage =
        "Cannot create job filter, because all working sets ([JES_WS_TEST]) on connection $connectionConfigMock already contain job filter with jobId = JOB_ID"

      val jesFilterNodes = mutableListOf(jesFilterNodeForTest1, jesFilterNodeForTest2)

      every { jesFilterNodeForTest1.value } returns jobFilter1
      every { jesFilterNodeForTest2.value } returns jobFilter2
      every { jesWsNodeForTest.name } returns "JES_WS_TEST"
      every { jesWsNodeForTest.children } returns jesFilterNodes
      every { jesWsNodeForTest.unit } returns jesWorkingSet
      every { jesWorkingSet.connectionConfig } returns connectionConfigMock

      every { myFsTreeStructureMock.findByPredicate(any()) } returns jesWsNodesTest

      runBlocking {
        withContext(Dispatchers.EDT) {
          classUnderTest.actionPerformed(actionEventMock)
        }
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
      assertSoftly {
        actual shouldBe expected
      }
    }

    should("returnTrue_whenIsDumbAware") {
      val actual = classUnderTest.isDumbAware
      assertSoftly {
        actual shouldBe true
      }
    }

    should("updateActionState_whenUpdate") {
      val jobMock = mockk<Job>()
      val jobLogFetcherMock = mockk<JobLogFetcher>()
      val jobLoggerMock = mockk<AbstractMFLoggerBase<JobProcessInfo, JobLogFetcher>>()
      every { jobMock.status } returns Job.Status.OUTPUT
      every { jobLoggerMock.logFetcher } returns jobLogFetcherMock
      every { jobLogFetcherMock.getCachedJobStatus() } returns jobMock
      every { actionEventMock.presentation } returns Presentation()
      every { consoleViewMock.getJobLogger() } returns jobLoggerMock
      classUnderTest.update(actionEventMock)
      assertSoftly {
        actionEventMock.presentation.isEnabled shouldBe true
        actionEventMock.presentation.isVisible shouldBe true
      }
    }
  }

})