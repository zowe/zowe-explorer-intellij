package org.zowe.explorer.explorer.actions.sort.members

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import org.zowe.explorer.dataops.BatchedRemoteQuery
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.fetch.LibraryQuery
import org.zowe.explorer.dataops.sort.SortQueryKeys
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*

class MembersSortActionTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("Members sort action") {

    val actionEventMock = mockk<AnActionEvent>()
    val explorerViewMock = mockk<FileExplorerView>()
    // action to spy
    val classUnderTest = spyk(MembersSortAction())

    should("returnSourceView_whenGetSourceView_givenActionEvent") {
      every { actionEventMock.getData(any() as DataKey<FileExplorerView>) } returns explorerViewMock

      val actualExplorerView = classUnderTest.getSourceView(actionEventMock)

      assertSoftly {
        actualExplorerView shouldNotBe null
        actualExplorerView is FileExplorerView
      }
    }

    should("returnNull_whenGetSourceView_givenActionEvent") {
      every { actionEventMock.getData(any() as DataKey<FileExplorerView>) } returns null

      val actualExplorerView = classUnderTest.getSourceView(actionEventMock)

      assertSoftly {
        actualExplorerView shouldBe null
      }
    }

    should("returnSourceNode_whenGetSourceNode_givenView") {
      val nodeMock = mockk<LibraryNode>()
      val fileMock = mockk<MFVirtualFile>()
      val attributesMock = mockk<RemoteDatasetAttributes>()
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
      val nodeMock = mockk<LibraryNode>()
      val sortKey = SortQueryKeys.MEMBER_NAME
      every { nodeMock.currentSortQueryKeysList } returns listOf(SortQueryKeys.MEMBER_NAME, SortQueryKeys.ASCENDING)

      val shouldEnableSortKey = classUnderTest.shouldEnableSortKeyForNode(nodeMock, sortKey)

      assertSoftly {
        shouldEnableSortKey shouldBe true
      }
    }

    should("returnFalse_whenShouldEnableSortKeyForNode_givenSelectedNodeAndSortKey") {
      val nodeMock = mockk<LibraryNode>()
      val sortKey = SortQueryKeys.MEMBER_NAME
      every { nodeMock.currentSortQueryKeysList } returns listOf()

      val shouldEnableSortKey = classUnderTest.shouldEnableSortKeyForNode(nodeMock, sortKey)

      assertSoftly {
        shouldEnableSortKey shouldBe false
      }
    }

    should("updateQuery_whenPerformQueryUpdateForNode_givenSelectedNodeAndSortKey") {
      val batchedQueryMock = mockk<BatchedRemoteQuery<LibraryQuery>>()
      val nodeMock = mockk<LibraryNode>()
      val sortKey = SortQueryKeys.MEMBER_NAME
      val expectedSortKeys = listOf(SortQueryKeys.ASCENDING, SortQueryKeys.MEMBER_NAME)
      every { nodeMock.query } returns batchedQueryMock
      every { batchedQueryMock.sortKeys } returns mutableListOf(SortQueryKeys.ASCENDING, SortQueryKeys.MEMBER_MODIFICATION_DATE)
      every { nodeMock.currentSortQueryKeysList } returns mutableListOf(SortQueryKeys.ASCENDING, SortQueryKeys.MEMBER_MODIFICATION_DATE)

      classUnderTest.performQueryUpdateForNode(nodeMock, sortKey)

      assertSoftly {
        batchedQueryMock.sortKeys shouldContainExactly expectedSortKeys
      }
    }

  }

})
