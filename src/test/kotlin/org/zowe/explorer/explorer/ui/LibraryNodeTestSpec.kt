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
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.sort.SortQueryKeys
import org.zowe.explorer.explorer.FileExplorer
import org.zowe.explorer.explorer.FilesWorkingSetImpl
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import org.zowe.explorer.explorer.UIComponentManager
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.kotlinsdk.Member

class LibraryNodeTestSpec : AppInitShouldSpec("explorer/ui/LibraryNode", {
  val mockedLibrary = mockk<MFVirtualFile>()
  val mockedProject = mockk<Project>()
  val mockedExplorerTreeNodeParent = mockk<DSMaskNode>()
  val mockedExplorer = mockk<FileExplorer>()
  val mockedWorkingSet = mockk<FilesWorkingSetImpl> {
    every { explorer } returns mockedExplorer
  }
  val mockedExplorerTreeStructure = mockk<ExplorerTreeStructureBase> {
    every { registerNode(any()) } just Runs
  }

  context("sort children nodes") {
    val uiComponentManager = UIComponentManager.getService()
    every { uiComponentManager.getExplorerContentProvider(any<Class<FileExplorer>>()) } returns mockk()

    val classUnderTest = spyk(
      LibraryNode(
        mockedLibrary,
        mockedProject,
        mockedExplorerTreeNodeParent,
        mockedWorkingSet,
        mockedExplorerTreeStructure
      )
    )

    val mockedVFileChild1 = mockk<MFVirtualFile>()
    val mockedVFileChild2 = mockk<MFVirtualFile>()
    val mockedVFileChild3 = mockk<MFVirtualFile>()
    val mockedAttributes1 = mockk<RemoteMemberAttributes>()
    val mockedAttributes2 = mockk<RemoteMemberAttributes>()
    val mockedAttributes3 = mockk<RemoteMemberAttributes>()
    val memberInfo1 = mockk<Member>()
    val memberInfo2 = mockk<Member>()
    val memberInfo3 = mockk<Member>()

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

    beforeEach {
      every { mockedAttributes1.info } returns memberInfo1
      every { mockedAttributes2.info } returns memberInfo2
      every { mockedAttributes3.info } returns memberInfo3
      every { mockedAttributes1.info.name } returns "AAAA"
      every { mockedAttributes2.info.name } returns "BBBB"
      every { mockedAttributes3.info.name } returns "CCCC"
      every { mockedAttributes1.info.modificationDate } returns "2024/01/10"
      every { mockedAttributes2.info.modificationDate } returns "2024/01/09"
      every { mockedAttributes3.info.modificationDate } returns "2024/02/02"
    }

    val mockedMember1 = spyk(
      FileLikeDatasetNode(
        mockedVFileChild1,
        mockedProject,
        mockedExplorerTreeNodeParent,
        mockedWorkingSet,
        mockedExplorerTreeStructure
      )
    )
    val mockedMember2 = spyk(
      FileLikeDatasetNode(
        mockedVFileChild2,
        mockedProject,
        mockedExplorerTreeNodeParent,
        mockedWorkingSet,
        mockedExplorerTreeStructure
      )
    )
    val mockedMember3 = spyk(
      FileLikeDatasetNode(
        mockedVFileChild3,
        mockedProject,
        mockedExplorerTreeNodeParent,
        mockedWorkingSet,
        mockedExplorerTreeStructure
      )
    )

    every { mockedMember1.virtualFile } returns mockedVFileChild1
    every { mockedMember2.virtualFile } returns mockedVFileChild2
    every { mockedMember3.virtualFile } returns mockedVFileChild3

    val mockedChildrenNodes = listOf<AbstractTreeNode<*>>(mockedMember1, mockedMember2, mockedMember3)

    should("sort by name ascending") {

      val sortQueryKeys = listOf(SortQueryKeys.MEMBER_NAME)
      every { classUnderTest.currentSortQueryKeysList } returns listOf(
        SortQueryKeys.MEMBER_NAME,
        SortQueryKeys.ASCENDING
      )

      val expected = listOf(mockedMember1, mockedMember2, mockedMember3)
      val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

      assertSoftly {
        actual shouldBe expected
      }
    }

    should("sort by name descending") {

      val sortQueryKeys = listOf(SortQueryKeys.MEMBER_NAME)
      every { classUnderTest.currentSortQueryKeysList } returns listOf(
        SortQueryKeys.MEMBER_NAME,
        SortQueryKeys.DESCENDING
      )

      val expected = listOf(mockedMember3, mockedMember2, mockedMember1)
      val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

      assertSoftly {
        actual shouldBe expected
      }
    }

    should("sort by date ascending") {

      val sortQueryKeys = listOf(SortQueryKeys.MEMBER_MODIFICATION_DATE)
      every { classUnderTest.currentSortQueryKeysList } returns listOf(
        SortQueryKeys.MEMBER_MODIFICATION_DATE,
        SortQueryKeys.ASCENDING
      )

      val expected = listOf(mockedMember2, mockedMember1, mockedMember3)
      val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

      assertSoftly {
        actual shouldBe expected
      }
    }

    should("sort by date descending") {

      val sortQueryKeys = listOf(SortQueryKeys.MEMBER_MODIFICATION_DATE)
      every { classUnderTest.currentSortQueryKeysList } returns listOf(
        SortQueryKeys.MEMBER_MODIFICATION_DATE,
        SortQueryKeys.DESCENDING
      )

      val expected = listOf(mockedMember3, mockedMember1, mockedMember2)
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

      val sortQueryKeys = listOf(SortQueryKeys.JOB_NAME)

      val actual = classUnderTest.sortChildrenNodes(mockedChildrenNodes, sortQueryKeys)

      assertSoftly {
        actual shouldBe mockedChildrenNodes
      }
    }
  }
})
