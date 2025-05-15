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

package org.zowe.explorer.explorer.ui

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.sort.SortQueryKeys
import org.zowe.explorer.explorer.FilesWorkingSetImpl
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.zowe.explorer.explorer.FileExplorer
import org.zowe.explorer.explorer.UIComponentManager
import org.zowe.explorer.testutils.AppInitShouldSpec

class DSMaskNodeTestSpec : AppInitShouldSpec("explorer/ui/DSMaskNode", {
  context("all functions") {
    val uiComponentManagerService = UIComponentManager.getService()
    every { uiComponentManagerService.getExplorerContentProvider(any<Class<FileExplorer>>()) } returns mockk()

    val mockedProject = mockk<Project>()
    val mockedExplorerTreeNodeParent = mockk<DSMaskNode>()
    val mockedWorkingSet = mockk<FilesWorkingSetImpl> {
      every { explorer } returns mockk()
    }
    val mockedExplorerTreeStructure = mockk<ExplorerTreeStructureBase>(relaxUnitFun = true)

    val classUnderTest = spyk(
      DSMaskNode(
        mockk<DSMask>(),
        mockedProject,
        mockedExplorerTreeNodeParent,
        mockedWorkingSet,
        mockedExplorerTreeStructure
      )
    )

    context("sort children nodes") {
      val mockedVFileChild1 = mockk<MFVirtualFile>()
      val mockedVFileChild2 = mockk<MFVirtualFile>()
      val mockedVFileChild3 = mockk<MFVirtualFile>()
      val mockedAttributes1 = mockk<RemoteDatasetAttributes> {
        every { datasetInfo } returns mockk {
          every { name } returns "AAAA"
          every { lastReferenceDate } returns "2024/01/10"
        }
      }
      val mockedAttributes2 = mockk<RemoteDatasetAttributes> {
        every { datasetInfo } returns mockk {
          every { name } returns "BBBB"
          every { lastReferenceDate } returns "2024/01/09"
        }
      }
      val mockedAttributes3 = mockk<RemoteDatasetAttributes> {
        every { datasetInfo } returns mockk {
          every { name } returns "CCCC"
          every { lastReferenceDate } returns "2024/02/02"
        }
      }

      val nodeToAttributesMap = mutableMapOf(
        Pair(mockedVFileChild1, mockedAttributes1),
        Pair(mockedVFileChild2, mockedAttributes2),
        Pair(mockedVFileChild3, mockedAttributes3)
      )

      val dataOpsManagerService = DataOpsManager.getService()
      every {
        dataOpsManagerService.tryToGetAttributes(any<VirtualFile>())
      } answers {
        val file = firstArg<VirtualFile>()
        nodeToAttributesMap[file]
      }

      val mockedDataset1 = spyk(
        FileLikeDatasetNode(
          mockedVFileChild1,
          mockedProject,
          mockedExplorerTreeNodeParent,
          mockedWorkingSet,
          mockedExplorerTreeStructure
        )
      ) {
        every { virtualFile } returns mockedVFileChild1
      }
      val mockedDataset2 = spyk(
        FileLikeDatasetNode(
          mockedVFileChild2,
          mockedProject,
          mockedExplorerTreeNodeParent,
          mockedWorkingSet,
          mockedExplorerTreeStructure
        )
      ) {
        every { virtualFile } returns mockedVFileChild2
      }
      val mockedDataset3 = spyk(
        LibraryNode(
          mockedVFileChild3,
          mockedProject,
          mockedExplorerTreeNodeParent,
          mockedWorkingSet,
          mockedExplorerTreeStructure
        )
      ) {
        every { virtualFile } returns mockedVFileChild3
      }

      val mockedChildrenNodes = listOf<AbstractTreeNode<*>>(mockedDataset1, mockedDataset2, mockedDataset3)

      should("sort by name ascending") {
        val sortQueryKeys = listOf(SortQueryKeys.DATASET_NAME)
        every {
          classUnderTest.currentSortQueryKeysList
        } returns listOf(SortQueryKeys.DATASET_NAME, SortQueryKeys.ASCENDING)

        val expected = listOf(mockedDataset1, mockedDataset2, mockedDataset3)
        val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

        assertSoftly { actual shouldBe expected }
      }

      should("sort by name descending") {
        val sortQueryKeys = listOf(SortQueryKeys.DATASET_NAME)
        every {
          classUnderTest.currentSortQueryKeysList
        } returns listOf(SortQueryKeys.DATASET_NAME, SortQueryKeys.DESCENDING)

        val expected = listOf(mockedDataset3, mockedDataset2, mockedDataset1)
        val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

        assertSoftly { actual shouldBe expected }
      }

      should("sort by date ascending") {
        val sortQueryKeys = listOf(SortQueryKeys.DATASET_MODIFICATION_DATE)
        every {
          classUnderTest.currentSortQueryKeysList
        } returns listOf(SortQueryKeys.DATASET_MODIFICATION_DATE, SortQueryKeys.ASCENDING)

        val expected = listOf(mockedDataset2, mockedDataset1, mockedDataset3)
        val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

        assertSoftly { actual shouldBe expected }
      }

      should("sort by date descending") {
        val sortQueryKeys = listOf(SortQueryKeys.DATASET_MODIFICATION_DATE)
        every {
          classUnderTest.currentSortQueryKeysList
        } returns listOf(SortQueryKeys.DATASET_MODIFICATION_DATE, SortQueryKeys.DESCENDING)

        val expected = listOf(mockedDataset3, mockedDataset1, mockedDataset2)
        val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

        assertSoftly { actual shouldBe expected }
      }

      should("sort by type ascending") {

        val sortQueryKeys = listOf(SortQueryKeys.DATASET_TYPE)
        every {
          classUnderTest.currentSortQueryKeysList
        } returns listOf(SortQueryKeys.DATASET_TYPE, SortQueryKeys.ASCENDING)

        val expected = listOf(mockedDataset3, mockedDataset1, mockedDataset2)
        val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

        assertSoftly { actual shouldBe expected }
      }

      should("sort by type descending") {
        val sortQueryKeys = listOf(SortQueryKeys.DATASET_TYPE)
        every {
          classUnderTest.currentSortQueryKeysList
        } returns listOf(SortQueryKeys.DATASET_TYPE, SortQueryKeys.DESCENDING)

        val expected = listOf(mockedDataset3, mockedDataset2, mockedDataset1)
        val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

        assertSoftly { actual shouldBe expected }
      }

      should("return unsorted nodes when passing null sort key") {
        val sortQueryKeys = listOf<SortQueryKeys>()

        val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

        assertSoftly { actual shouldBe mockedChildrenNodes }
      }

      should("return unsorted nodes when passing invalid sort key") {
        val unexpectedNode = mockk<UssDirNode>()

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
