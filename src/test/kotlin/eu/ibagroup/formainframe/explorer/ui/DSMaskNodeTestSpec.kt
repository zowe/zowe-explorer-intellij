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

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.sort.SortQueryKeys
import eu.ibagroup.formainframe.explorer.FileExplorer
import eu.ibagroup.formainframe.explorer.FilesWorkingSetImpl
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import org.zowe.kotlinsdk.Dataset

class DSMaskNodeTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("explorer module: ui/DSMaskNode") {
    val mockedDSMask = mockk<DSMask>()
    val mockedProject = mockk<Project>()
    val mockedExplorerTreeNodeParent = mockk<DSMaskNode>()
    val mockedWorkingSet = mockk<FilesWorkingSetImpl>()
    val mockedExplorer = mockk<FileExplorer>()
    val mockedExplorerTreeStructure = mockk<ExplorerTreeStructureBase>()

    every { mockedWorkingSet.explorer } returns mockedExplorer
    every { mockedExplorerTreeStructure.registerNode(any()) } just Runs

    val classUnderTest = spyk(
      DSMaskNode(
        mockedDSMask,
        mockedProject,
        mockedExplorerTreeNodeParent,
        mockedWorkingSet,
        mockedExplorerTreeStructure
      )
    )

    context("sort children nodes") {
      val unexpectedNode = mockk<UssDirNode>()
      val mockedVFileChild1 = mockk<MFVirtualFile>()
      val mockedVFileChild2 = mockk<MFVirtualFile>()
      val mockedVFileChild3 = mockk<MFVirtualFile>()
      val mockedAttributes1 = mockk<RemoteDatasetAttributes>()
      val mockedAttributes2 = mockk<RemoteDatasetAttributes>()
      val mockedAttributes3 = mockk<RemoteDatasetAttributes>()
      val datasetInfo1 = mockk<Dataset>()
      val datasetInfo2 = mockk<Dataset>()
      val datasetInfo3 = mockk<Dataset>()

      val nodeToAttributesMap = mutableMapOf(
        Pair(mockedVFileChild1, mockedAttributes1),
        Pair(mockedVFileChild2, mockedAttributes2),
        Pair(mockedVFileChild3, mockedAttributes3)
      )

      val dataOpsManagerService = DataOpsManager.getService() as TestDataOpsManagerImpl
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
        override fun tryToGetAttributes(file: VirtualFile): FileAttributes? {
          return nodeToAttributesMap[file]
        }
      }

      beforeEach {
        every { mockedAttributes1.datasetInfo } returns datasetInfo1
        every { mockedAttributes2.datasetInfo } returns datasetInfo2
        every { mockedAttributes3.datasetInfo } returns datasetInfo3
        every { mockedAttributes1.datasetInfo.name } returns "AAAA"
        every { mockedAttributes2.datasetInfo.name } returns "BBBB"
        every { mockedAttributes3.datasetInfo.name } returns "CCCC"
        every { mockedAttributes1.datasetInfo.lastReferenceDate } returns "2024/01/10"
        every { mockedAttributes2.datasetInfo.lastReferenceDate } returns "2024/01/09"
        every { mockedAttributes3.datasetInfo.lastReferenceDate } returns "2024/02/02"
      }

      val mockedDataset1 = spyk(
        FileLikeDatasetNode(
          mockedVFileChild1,
          mockedProject,
          mockedExplorerTreeNodeParent,
          mockedWorkingSet,
          mockedExplorerTreeStructure
        )
      )
      val mockedDataset2 = spyk(
        FileLikeDatasetNode(
          mockedVFileChild2,
          mockedProject,
          mockedExplorerTreeNodeParent,
          mockedWorkingSet,
          mockedExplorerTreeStructure
        )
      )
      val mockedDataset3 = spyk(
        LibraryNode(
          mockedVFileChild3,
          mockedProject,
          mockedExplorerTreeNodeParent,
          mockedWorkingSet,
          mockedExplorerTreeStructure
        )
      )

      every { mockedDataset1.virtualFile } returns mockedVFileChild1
      every { mockedDataset2.virtualFile } returns mockedVFileChild2
      every { mockedDataset3.virtualFile } returns mockedVFileChild3

      val mockedChildrenNodes = listOf<AbstractTreeNode<*>>(mockedDataset1, mockedDataset2, mockedDataset3)

      should("sort by name ascending") {

        val sortQueryKeys = listOf(SortQueryKeys.DATASET_NAME)
        every { classUnderTest.currentSortQueryKeysList } returns listOf(
          SortQueryKeys.DATASET_NAME,
          SortQueryKeys.ASCENDING
        )

        val expected = listOf(mockedDataset1, mockedDataset2, mockedDataset3)
        val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

        assertSoftly {
          actual shouldBe expected
        }
      }

      should("sort by name descending") {

        val sortQueryKeys = listOf(SortQueryKeys.DATASET_NAME)
        every { classUnderTest.currentSortQueryKeysList } returns listOf(
          SortQueryKeys.DATASET_NAME,
          SortQueryKeys.DESCENDING
        )

        val expected = listOf(mockedDataset3, mockedDataset2, mockedDataset1)
        val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

        assertSoftly {
          actual shouldBe expected
        }
      }

      should("sort by date ascending") {

        val sortQueryKeys = listOf(SortQueryKeys.DATASET_MODIFICATION_DATE)
        every { classUnderTest.currentSortQueryKeysList } returns listOf(
          SortQueryKeys.DATASET_MODIFICATION_DATE,
          SortQueryKeys.ASCENDING
        )

        val expected = listOf(mockedDataset2, mockedDataset1, mockedDataset3)
        val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

        assertSoftly {
          actual shouldBe expected
        }
      }

      should("sort by date descending") {

        val sortQueryKeys = listOf(SortQueryKeys.DATASET_MODIFICATION_DATE)
        every { classUnderTest.currentSortQueryKeysList } returns listOf(
          SortQueryKeys.DATASET_MODIFICATION_DATE,
          SortQueryKeys.DESCENDING
        )

        val expected = listOf(mockedDataset3, mockedDataset1, mockedDataset2)
        val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

        assertSoftly {
          actual shouldBe expected
        }
      }

      should("sort by type ascending") {

        val sortQueryKeys = listOf(SortQueryKeys.DATASET_TYPE)
        every { classUnderTest.currentSortQueryKeysList } returns listOf(
          SortQueryKeys.DATASET_TYPE,
          SortQueryKeys.ASCENDING
        )

        val expected = listOf(mockedDataset3, mockedDataset1, mockedDataset2)
        val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

        assertSoftly {
          actual shouldBe expected
        }
      }

      should("sort by type descending") {

        val sortQueryKeys = listOf(SortQueryKeys.DATASET_TYPE)
        every { classUnderTest.currentSortQueryKeysList } returns listOf(
          SortQueryKeys.DATASET_TYPE,
          SortQueryKeys.DESCENDING
        )

        val expected = listOf(mockedDataset3, mockedDataset2, mockedDataset1)
        val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

        assertSoftly {
          actual shouldBe expected
        }
      }

      should("return unsorted nodes when passing null sort key") {

        val sortQueryKeys = listOf<SortQueryKeys>()

        val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

        assertSoftly {
          actual shouldBe mockedChildrenNodes
        }
      }

      should("return unsorted nodes when passing invalid sort key") {

        val mockedChildrenNodesForThisTest =
          listOf<AbstractTreeNode<*>>(mockedDataset1, mockedDataset2, mockedDataset3, unexpectedNode)
        val sortQueryKeys = listOf(SortQueryKeys.JOB_NAME)

        val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodesForThisTest, sortQueryKeys)

        assertSoftly {
          actual shouldBe mockedChildrenNodesForThisTest
        }
      }
    }
  }

})