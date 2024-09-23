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

package eu.ibagroup.formainframe.explorer.actions.sort.uss

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.fetch.UssQuery
import eu.ibagroup.formainframe.dataops.sort.SortQueryKeys
import eu.ibagroup.formainframe.explorer.actions.sort.SortAction
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*

class UssSortActionTestSpec : WithApplicationShouldSpec ({

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("USS sort action") {

    mockkObject(SortAction.Companion)
    every { SortAction.runRefreshAction(any()) } just Runs

    // action to spy
    val classUnderTest = spyk(UssSortAction())

    val mockedActionEvent = mockk<AnActionEvent>()

    context("common spec") {

      val mockedFileExplorerView = mockk<FileExplorerView>()

      // Target UssDirNode + Query for test
      val mockedMFVirtualFile = mockk<MFVirtualFile>()
      val mockedUssDirNode = mockk<UssDirNode>()
      val mockedUssRemoteAttributes = mockk<RemoteUssAttributes>()
      val mockedUssQuery = mockk<UnitRemoteQueryImpl<ConnectionConfig, UssQuery>>()
      every { mockedUssDirNode.virtualFile } returns mockedMFVirtualFile
      every { mockedUssDirNode.query } returns mockedUssQuery
      every { mockedUssQuery.sortKeys } returns mutableListOf()

      // NodeData for test
      val mockedNodeDataForTest = NodeData(mockedUssDirNode, mockedMFVirtualFile, mockedUssRemoteAttributes)
      mockkObject(mockedNodeDataForTest)

      // Common config for test
      every { mockedActionEvent.getExplorerView<FileExplorerView>() } returns mockedFileExplorerView

      context("misc") {

        should("returnSourceView_whenGetSourceView_givenActionEvent") {
          every { mockedActionEvent.getData(any() as DataKey<FileExplorerView>) } returns mockedFileExplorerView

          val actualExplorerView = classUnderTest.getSourceView(mockedActionEvent)

          assertSoftly {
            actualExplorerView shouldNotBe null
            actualExplorerView is FileExplorerView
          }
        }

        should("returnNull_whenGetSourceView_givenActionEvent") {
          every { mockedActionEvent.getData(any() as DataKey<FileExplorerView>) } returns null

          val actualExplorerView = classUnderTest.getSourceView(mockedActionEvent)

          assertSoftly {
            actualExplorerView shouldBe null
          }
        }

        should("returnSourceNode_whenGetSourceNode_givenView") {
          val nodeMock = mockk<UssDirNode>()
          val fileMock = mockk<MFVirtualFile>()
          val attributesMock = mockk<RemoteUssAttributes>()
          val myNodesData = mutableListOf(NodeData(nodeMock, fileMock, attributesMock))
          mockkObject(myNodesData)
          every { mockedFileExplorerView.mySelectedNodesData } returns myNodesData

          val actualNode = classUnderTest.getSourceNode(mockedFileExplorerView)

          assertSoftly {
            actualNode shouldBe nodeMock
          }
        }

        should("returnNull_whenGetSourceNode_givenView") {
          val nodeMock = mockk<DSMaskNode>()
          val fileMock = mockk<MFVirtualFile>()
          val attributesMock = mockk<RemoteDatasetAttributes>()
          val myNodesData = mutableListOf(NodeData(nodeMock, fileMock, attributesMock))
          mockkObject(myNodesData)
          every { mockedFileExplorerView.mySelectedNodesData } returns myNodesData

          val actualNode = classUnderTest.getSourceNode(mockedFileExplorerView)

          assertSoftly {
            actualNode shouldBe null
          }
        }

        should("returnTrue_whenShouldEnableSortKeyForNode_givenSelectedNodeAndSortKey") {
          val nodeMock = mockk<UssDirNode>()
          val sortKey = SortQueryKeys.FILE_NAME
          every { nodeMock.currentSortQueryKeysList } returns listOf(SortQueryKeys.FILE_NAME, SortQueryKeys.ASCENDING)

          val shouldEnableSortKey = classUnderTest.shouldEnableSortKeyForNode(nodeMock, sortKey)

          assertSoftly {
            shouldEnableSortKey shouldBe true
          }
        }

        should("returnFalse_whenShouldEnableSortKeyForNode_givenSelectedNodeAndSortKey") {
          val nodeMock = mockk<UssDirNode>()
          val sortKey = SortQueryKeys.FILE_NAME
          every { nodeMock.currentSortQueryKeysList } returns listOf()

          val shouldEnableSortKey = classUnderTest.shouldEnableSortKeyForNode(nodeMock, sortKey)

          assertSoftly {
            shouldEnableSortKey shouldBe false
          }
        }

        should("updateQuery_whenPerformQueryUpdateForNode_givenSelectedNodeAndSortKey") {
          val ussQueryMock = mockk<UnitRemoteQueryImpl<ConnectionConfig, UssQuery>>()
          val nodeMock = mockk<UssDirNode>()
          val sortKey = SortQueryKeys.FILE_NAME
          val expectedSortKeys = listOf(SortQueryKeys.ASCENDING, SortQueryKeys.FILE_NAME)
          every { nodeMock.query } returns ussQueryMock
          every { ussQueryMock.sortKeys } returns mutableListOf(SortQueryKeys.ASCENDING, SortQueryKeys.FILE_MODIFICATION_DATE)
          every { nodeMock.currentSortQueryKeysList } returns mutableListOf(SortQueryKeys.ASCENDING, SortQueryKeys.FILE_MODIFICATION_DATE)

          classUnderTest.performQueryUpdateForNode(nodeMock, sortKey)

          assertSoftly {
            ussQueryMock.sortKeys shouldContainExactly expectedSortKeys
          }
        }
      }

      context("isSelected") {

        should("returnFalse_whenIsSelected_givenExplorerNull") {
          every { mockedActionEvent.getData(any() as DataKey<FileExplorerView>) } returns null
          val isSelected = classUnderTest.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("returnFalse_whenIsSelected_givenNullTemplateText") {
          every { mockedActionEvent.getData(any() as DataKey<FileExplorerView>) } returns mockedFileExplorerView
          every { classUnderTest.templateText } returns null
          val isSelected = classUnderTest.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("returnFalse_whenIsSelected_givenNotUssDirNode") {
          every { classUnderTest.templateText } returns "File Name"
          val mockedNodeDataNotUssForTest =
            NodeData(mockk<LibraryNode>(), mockk(), mockk<RemoteDatasetAttributes>())
          every { mockedFileExplorerView.mySelectedNodesData } answers {
            listOf(mockedNodeDataNotUssForTest)
          }
          val isSelected = classUnderTest.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("returnFalse_whenIsSelected_givenNodeWithNoKeysSpecified") {
          every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedNodeDataForTest)
          every { mockedUssDirNode.currentSortQueryKeysList } answers {
            mutableListOf()
          }
          val isSelected = classUnderTest.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("returnTrue_whenIsSelected_givenValidDataAndSortKey") {
          every { mockedUssDirNode.currentSortQueryKeysList } answers {
            mutableListOf(SortQueryKeys.FILE_NAME, SortQueryKeys.ASCENDING)
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
          var setSelected = true
          every { mockedActionEvent.getData(any() as DataKey<FileExplorerView>) } answers {
            setSelected = false
            null
          }

          // when
          classUnderTest.setSelected(mockedActionEvent, true)

          // then
          assertSoftly { setSelected shouldBe false }
        }

        should("throwException_whenSetSelected_givenNullTemplateText") {
          // given
          every { mockedActionEvent.getData(any() as DataKey<FileExplorerView>) } returns mockedFileExplorerView
          every { classUnderTest.templateText } returns null

          // when
          val exception = shouldThrowExactly<Exception> { classUnderTest.setSelected(mockedActionEvent, true) }

          // then
          assertSoftly {
            exception shouldNotBe null
            exception.message shouldBe "Sort key for the selected action was not found."
          }
        }

        should("return_whenSetSelected_givenNotUssDirNode") {
          // given
          var setSelected = true
          every { classUnderTest.isSelected(any()) } returns false
          val mockedNodeDataNotUssForTest =
            NodeData(mockk<LibraryNode>(), mockk(), mockk<RemoteDatasetAttributes>())
          every { mockedFileExplorerView.mySelectedNodesData } answers {
            setSelected = false
            listOf(mockedNodeDataNotUssForTest)
          }

          // when
          classUnderTest.setSelected(mockedActionEvent, true)

          // then
          assertSoftly {
            setSelected shouldBe false
          }
        }

        should("shouldSetSelected_whenSetSelected_givenValidSortKey") {
          // given
          val dataContext = mockk<SimpleDataContext>()
          val expectedSortKeys = listOf(SortQueryKeys.ASCENDING, SortQueryKeys.FILE_TYPE)
          every { classUnderTest.templateText } returns "File Type"
          every { classUnderTest.isSelected(any()) } returns false
          every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedNodeDataForTest)
          every { mockedUssDirNode.currentSortQueryKeysList } returns mutableListOf(SortQueryKeys.FILE_NAME, SortQueryKeys.ASCENDING)
          every { mockedActionEvent.place } returns "Place"
          every { mockedActionEvent.dataContext } returns dataContext

          // when
          classUnderTest.setSelected(mockedActionEvent, true)

          // then
          assertSoftly {
            mockedUssQuery.sortKeys shouldContainExactly expectedSortKeys
          }
        }

        should("return_whenSetSelected_givenAlreadySelectedSortKey") {
          //
          clearMocks(classUnderTest, mockedActionEvent,
            answers = false, recordedCalls = true, childMocks = false, verificationMarks = true, exclusionRules = false)
          var setSelected = true
          every { classUnderTest.templateText } returns "File Name"
          every { classUnderTest.isSelected(any()) } answers {
            setSelected = false
            true
          }

          // when
          classUnderTest.setSelected(mockedActionEvent, true)

          // then
          verify(exactly = 1) { classUnderTest.isSelected(mockedActionEvent) }
          assertSoftly { setSelected shouldBe false }
        }
      }
    }
  }
})
