/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.actions.sort.jobs

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import eu.ibagroup.formainframe.dataops.fetch.JobFetchProvider
import eu.ibagroup.formainframe.dataops.sort.SortQueryKeys
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*

class JobsSortActionTestSpec : WithApplicationShouldSpec ({

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("Jobs sort action") {

    // action to spy
    val classUnderTest = spyk(JobsSortAction())

    val mockedActionEvent = mockk<AnActionEvent>()
    val dataOpsManagerService = ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
    val mockedFileFetchProvider = mockk<JobFetchProvider>()
    dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(ApplicationManager.getApplication()) {
      @Suppress("UNCHECKED_CAST")
      override fun <R : Any, Q : Query<R, Unit>, File : VirtualFile> getFileFetchProvider(
        requestClass: Class<out R>,
        queryClass: Class<out Query<*, *>>,
        vFileClass: Class<out File>
      ): FileFetchProvider<R, Q, File> {
        return mockedFileFetchProvider as FileFetchProvider<R, Q, File>
      }

    }

    context("common spec") {

      val mockedFileExplorerView = mockk<JesExplorerView>()

      // Target UssDirNode + Query for test
      val mockedMFVirtualFile = mockk<MFVirtualFile>()
      val mockedJesFilterNode = mockk<JesFilterNode>()
      val mockedJobRemoteAttributes = mockk<RemoteJobAttributes>()
      val mockedJobQuery = mockk<UnitRemoteQueryImpl<ConnectionConfig, JobsFilter>>()
      every { mockedJesFilterNode.virtualFile } returns mockedMFVirtualFile
      every { mockedJesFilterNode.query } returns mockedJobQuery
      every { mockedJobQuery.sortKeys } returns mutableListOf()

      // NodeData for test
      val mockedNodeDataForTest = NodeData(mockedJesFilterNode, mockedMFVirtualFile, mockedJobRemoteAttributes)
      mockkObject(mockedNodeDataForTest)

      // Common config for test
      every { mockedActionEvent.getExplorerView<JesExplorerView>() } returns mockedFileExplorerView
      every { mockedJesFilterNode.cleanCache(false) } just Runs
      every { mockedFileFetchProvider.reload(any()) } just Runs
      every { mockedFileFetchProvider.applyRefreshCacheDate(any(), any(), any()) } just Runs

      context("isSelected") {

        should("returnFalse_whenIsSelected_givenExplorerNull") {
          every { mockedActionEvent.getExplorerView<JesExplorerView>() } returns null
          val isSelected = classUnderTest.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("returnFalse_whenIsSelected_givenNullTemplateText") {
          every { mockedActionEvent.getExplorerView<JesExplorerView>() } returns mockedFileExplorerView
          every { classUnderTest.templateText } returns null
          val isSelected = classUnderTest.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("returnFalse_whenIsSelected_givenNotUssDirNode") {
          every { classUnderTest.templateText } returns "Job Name"
          val mockedNodeDataNotJesFilterForTest =
            NodeData(mockk<LibraryNode>(), mockk(), mockk<RemoteDatasetAttributes>())
          every { mockedFileExplorerView.mySelectedNodesData } answers {
            listOf(mockedNodeDataNotJesFilterForTest)
          }
          val isSelected = classUnderTest.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("returnFalse_whenIsSelected_givenNodeWithNoKeysSpecified") {
          every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedNodeDataForTest)
          every { mockedJesFilterNode.currentSortQueryKeysList } answers {
            mutableListOf()
          }
          val isSelected = classUnderTest.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("returnTrue_whenIsSelected_givenValidDataAndSortKey") {
          every { mockedJesFilterNode.currentSortQueryKeysList } answers {
            mutableListOf(SortQueryKeys.JOB_NAME, SortQueryKeys.ASCENDING)
          }
          val isSelected = classUnderTest.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe true
          }
        }
      }

      context("setSelected") {

        should("return_whenSetSelected_givenExplorerNull") {
          // given
          clearMocks(classUnderTest, mockedActionEvent, mockedFileFetchProvider,
            answers = false, recordedCalls = true, childMocks = false, verificationMarks = true, exclusionRules = false)
          every { mockedActionEvent.getExplorerView<JesExplorerView>() } returns null

          // when
          classUnderTest.setSelected(mockedActionEvent, true)

          // then
          verify { mockedFileFetchProvider wasNot Called }
        }

        should("throwException_whenSetSelected_givenNullTemplateText") {
          // given
          clearMocks(classUnderTest, mockedActionEvent, mockedFileFetchProvider,
            answers = false, recordedCalls = true, childMocks = false, verificationMarks = true, exclusionRules = false)
          every { mockedActionEvent.getExplorerView<JesExplorerView>() } returns mockedFileExplorerView
          every { classUnderTest.templateText } returns null

          // when
          val exception = shouldThrowExactly<Exception> { classUnderTest.setSelected(mockedActionEvent, true) }

          // then
          assertSoftly {
            exception shouldNotBe null
            exception.message shouldBe "Sort key for the selected action was not found."
          }
        }

        should("return_whenSetSelected_givenAlreadySelectedSortKey") {
          //
          clearMocks(classUnderTest, mockedActionEvent, mockedFileFetchProvider,
            answers = false, recordedCalls = true, childMocks = false, verificationMarks = true, exclusionRules = false)
          every { classUnderTest.templateText } returns "Job Name"
          every { classUnderTest.isSelected(any()) } returns true

          // when
          classUnderTest.setSelected(mockedActionEvent, true)

          // then
          verify(exactly = 1) { classUnderTest.isSelected(mockedActionEvent) }
          verify { mockedFileFetchProvider wasNot Called }
        }

        should("return_whenSetSelected_givenNotJesFilterNode") {
          // given
          clearMocks(classUnderTest, mockedActionEvent, mockedFileFetchProvider,
            answers = false, recordedCalls = true, childMocks = false, verificationMarks = true, exclusionRules = false)
          every { classUnderTest.isSelected(any()) } returns false
          val mockedNodeDataNotJesFilterForTest =
            NodeData(mockk<LibraryNode>(), mockk(), mockk<RemoteDatasetAttributes>())
          every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedNodeDataNotJesFilterForTest)

          // when
          classUnderTest.setSelected(mockedActionEvent, true)

          // then
          verify { mockedFileFetchProvider wasNot Called }
        }

        should("callFetchProvider_whenSetSelected_givenValidSortKey") {
          // given
          clearMocks(classUnderTest, mockedActionEvent, mockedFileFetchProvider,
            answers = false, recordedCalls = true, childMocks = false, verificationMarks = true, exclusionRules = false)
          every { classUnderTest.isSelected(any()) } returns false
          every { classUnderTest.templateText } returns "Job Name"
          every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedNodeDataForTest)
          every { mockedJesFilterNode.currentSortQueryKeysList } answers {
            mutableListOf(SortQueryKeys.JOB_ID, SortQueryKeys.ASCENDING)
          }

          // when
          classUnderTest.setSelected(mockedActionEvent, true)

          // then
          verify(exactly = 1) { mockedJesFilterNode.cleanCache(false) }
          verify(exactly = 1) { mockedFileFetchProvider.reload(mockedJobQuery) }
          verify(exactly = 1) { mockedFileFetchProvider.applyRefreshCacheDate(mockedJobQuery, mockedJesFilterNode, any()) }
        }
      }
    }
  }
})