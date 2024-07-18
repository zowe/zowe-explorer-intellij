package org.zowe.explorer.explorer.actions.sort.datasets

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.dataops.BatchedRemoteQuery
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.sort.SortQueryKeys
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*

class DatasetsSortActionTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("Datasets sort action") {

    val actionEventMock = mockk<AnActionEvent>()
    val explorerViewMock = mockk<FileExplorerView>()
    // action to spy
    val classUnderTest = spyk(DatasetsSortAction())

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
      val nodeMock = mockk<DSMaskNode>()
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
      val nodeMock = mockk<JesFilterNode>()
      val fileMock = mockk<MFVirtualFile>()
      val attributesMock = mockk<RemoteJobAttributes>()
      val myNodesData = mutableListOf(NodeData(nodeMock, fileMock, attributesMock))
      mockkObject(myNodesData)
      every { explorerViewMock.mySelectedNodesData } returns myNodesData

      val actualNode = classUnderTest.getSourceNode(explorerViewMock)

      assertSoftly {
        actualNode shouldBe null
      }
    }

    should("returnTrue_whenShouldEnableSortKeyForNode_givenSelectedNodeAndSortKey") {
      val nodeMock = mockk<DSMaskNode>()
      val sortKey = SortQueryKeys.DATASET_NAME
      every { nodeMock.currentSortQueryKeysList } returns listOf(SortQueryKeys.DATASET_NAME, SortQueryKeys.ASCENDING)

      val shouldEnableSortKey = classUnderTest.shouldEnableSortKeyForNode(nodeMock, sortKey)

      assertSoftly {
        shouldEnableSortKey shouldBe true
      }
    }

    should("returnFalse_whenShouldEnableSortKeyForNode_givenSelectedNodeAndSortKey") {
      val nodeMock = mockk<DSMaskNode>()
      val sortKey = SortQueryKeys.DATASET_NAME
      every { nodeMock.currentSortQueryKeysList } returns listOf()

      val shouldEnableSortKey = classUnderTest.shouldEnableSortKeyForNode(nodeMock, sortKey)

      assertSoftly {
        shouldEnableSortKey shouldBe false
      }
    }

    should("updateQuery_whenPerformQueryUpdateForNode_givenSelectedNodeAndSortKey") {
      val batchedQueryMock = mockk<BatchedRemoteQuery<DSMask>>()
      val nodeMock = mockk<DSMaskNode>()
      val sortKey = SortQueryKeys.DATASET_NAME
      val expectedSortKeys = listOf(SortQueryKeys.ASCENDING, SortQueryKeys.DATASET_NAME)
      every { nodeMock.query } returns batchedQueryMock
      every { batchedQueryMock.sortKeys } returns mutableListOf(SortQueryKeys.ASCENDING, SortQueryKeys.DATASET_MODIFICATION_DATE)
      every { nodeMock.currentSortQueryKeysList } returns mutableListOf(SortQueryKeys.ASCENDING, SortQueryKeys.DATASET_MODIFICATION_DATE)

      classUnderTest.performQueryUpdateForNode(nodeMock, sortKey)

      assertSoftly {
        batchedQueryMock.sortKeys shouldContainExactly expectedSortKeys
      }
    }

  }

})
