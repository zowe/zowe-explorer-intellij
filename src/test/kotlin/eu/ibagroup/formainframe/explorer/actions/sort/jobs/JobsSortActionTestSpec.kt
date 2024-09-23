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

package eu.ibagroup.formainframe.explorer.actions.sort.jobs

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.dataops.sort.SortQueryKeys
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*

class JobsSortActionTestSpec : WithApplicationShouldSpec ({

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("Jobs sort action") {

    val actionEventMock = mockk<AnActionEvent>()
    val explorerViewMock = mockk<JesExplorerView>()
    // action to spy
    val classUnderTest = spyk(JobsSortAction())

    should("returnSourceView_whenGetSourceView_givenActionEvent") {
      every { actionEventMock.getData(any() as DataKey<JesExplorerView>) } returns explorerViewMock

      val actualExplorerView = classUnderTest.getSourceView(actionEventMock)

      assertSoftly {
        actualExplorerView shouldNotBe null
        actualExplorerView is JesExplorerView
      }
    }

    should("returnNull_whenGetSourceView_givenActionEvent") {
      every { actionEventMock.getData(any() as DataKey<JesExplorerView>) } returns null

      val actualExplorerView = classUnderTest.getSourceView(actionEventMock)

      assertSoftly {
        actualExplorerView shouldBe null
      }
    }

    should("returnSourceNode_whenGetSourceNode_givenView") {
      val nodeMock = mockk<JesFilterNode>()
      val fileMock = mockk<MFVirtualFile>()
      val attributesMock = mockk<RemoteJobAttributes>()
      val myNodesData = mutableListOf(NodeData(nodeMock, fileMock, attributesMock))
      mockkObject(myNodesData)
      every { explorerViewMock.mySelectedNodesData } returns myNodesData

      val actualNode = classUnderTest.getSourceNode(explorerViewMock)

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
      every { explorerViewMock.mySelectedNodesData } returns myNodesData

      val actualNode = classUnderTest.getSourceNode(explorerViewMock)

      assertSoftly {
        actualNode shouldBe null
      }
    }

    should("returnTrue_whenShouldEnableSortKeyForNode_givenSelectedNodeAndSortKey") {
      val nodeMock = mockk<JesFilterNode>()
      val sortKey = SortQueryKeys.JOB_NAME
      every { nodeMock.currentSortQueryKeysList } returns listOf(SortQueryKeys.JOB_NAME, SortQueryKeys.ASCENDING)

      val shouldEnableSortKey = classUnderTest.shouldEnableSortKeyForNode(nodeMock, sortKey)

      assertSoftly {
        shouldEnableSortKey shouldBe true
      }
    }

    should("returnFalse_whenShouldEnableSortKeyForNode_givenSelectedNodeAndSortKey") {
      val nodeMock = mockk<JesFilterNode>()
      val sortKey = SortQueryKeys.JOB_NAME
      every { nodeMock.currentSortQueryKeysList } returns listOf()

      val shouldEnableSortKey = classUnderTest.shouldEnableSortKeyForNode(nodeMock, sortKey)

      assertSoftly {
        shouldEnableSortKey shouldBe false
      }
    }

    should("updateQuery_whenPerformQueryUpdateForNode_givenSelectedNodeAndSortKey") {
      val jobQueryMock = mockk<UnitRemoteQueryImpl<ConnectionConfig, JobsFilter>>()
      val nodeMock = mockk<JesFilterNode>()
      val sortKey = SortQueryKeys.JOB_NAME
      val expectedSortKeys = listOf(SortQueryKeys.ASCENDING, SortQueryKeys.JOB_NAME)
      every { nodeMock.query } returns jobQueryMock
      every { jobQueryMock.sortKeys } returns mutableListOf(SortQueryKeys.ASCENDING, SortQueryKeys.JOB_COMPLETION_DATE)
      every { nodeMock.currentSortQueryKeysList } returns mutableListOf(SortQueryKeys.ASCENDING, SortQueryKeys.JOB_COMPLETION_DATE)

      classUnderTest.performQueryUpdateForNode(nodeMock, sortKey)

      assertSoftly {
        jobQueryMock.sortKeys shouldContainExactly expectedSortKeys
      }
    }
  }
})
