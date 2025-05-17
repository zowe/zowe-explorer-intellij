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

package org.zowe.explorer.explorer.actions.sort.uss

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.ui.treeStructure.Tree
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.zowe.explorer.testutils.MockkAwareShouldSpec
import javax.swing.tree.TreePath

class UssSortActionGroupTestSpec : MockkAwareShouldSpec({
  context("uss sort action group spec") {
    // group action to spy
    val classUnderTest = spyk(UssSortActionGroup())

    val mockedActionEvent = mockk<AnActionEvent> {
      every { presentation } returns Presentation()
    }
    val selectionPathMock = mockk<TreePath>()
    val filesTreeMock = mockk<Tree> {
      every { selectionPath } returns selectionPathMock
    }
    val mockedFileExplorerView = mockk<FileExplorerView> {
      every { myTree } returns filesTreeMock
    }

    // Presentation

    // Target UssDirNode + Query for test
    val mockedMFVirtualFile = mockk<MFVirtualFile>()
    val mockedUssDirNode = mockk<UssDirNode> {
      every { virtualFile } returns mockedMFVirtualFile
      every { query } returns mockk()
    }
    val mockedUssRemoteAttributes = mockk<RemoteUssAttributes>()

    // NodeData for test
    val mockedNodeDataForTest = NodeData(mockedUssDirNode, mockedMFVirtualFile, mockedUssRemoteAttributes)
    mockkObject(mockedNodeDataForTest)

    beforeEach {
      every { mockedActionEvent.getData(EXPLORER_VIEW) } returns mockedFileExplorerView
      every { filesTreeMock.isExpanded(any<Int>()) } returns false
    }

    should("shouldReturnExplorerView_whenGetExplorerView_givenActionEvent") {
      every { mockedActionEvent.getData(any() as DataKey<FileExplorerView>) } returns mockedFileExplorerView
      val actualExplorer = classUnderTest.getSourceView(mockedActionEvent)

      assertSoftly {
        actualExplorer shouldNotBe null
        actualExplorer is FileExplorerView
      }
    }

    should("shouldReturnTrue_whenCheckNode_givenUssDirNode") {
      val nodeMock = mockk<UssDirNode>()
      val checkNode = classUnderTest.checkNode(nodeMock)

      assertSoftly { checkNode shouldBe true }
    }

    should("shouldReturnNull_whenGetExplorerView_givenActionEvent") {
      every { mockedActionEvent.getData(EXPLORER_VIEW) } returns null

      val actualExplorer = classUnderTest.getSourceView(mockedActionEvent)

      assertSoftly { actualExplorer shouldBe null }
    }

    should("shouldReturnFalse_whenCheckNode_givenWrongNode") {
      val nodeMock = mockk<DSMaskNode>()
      val checkNode = classUnderTest.checkNode(nodeMock)

      assertSoftly { checkNode shouldBe false }
    }

    should("is visible from context menu if file explorer view is null") {
      var isVisible = true
      every {
        mockedActionEvent.getData(EXPLORER_VIEW)
      } answers {
        isVisible = false
        null
      }

      classUnderTest.update(mockedActionEvent)

      assertSoftly { isVisible shouldBe false }
    }

    should("is visible from context menu if file explorer view is not null and selected node is not UssDirNode") {
      var isVisible = true
      val mockedNodeDataNotUssForTest =
        NodeData(mockk<LibraryNode>(), mockk(), mockk<RemoteDatasetAttributes>())
      every {
        mockedFileExplorerView.mySelectedNodesData
      } answers {
        isVisible = false
        listOf(mockedNodeDataNotUssForTest)
      }
      every { filesTreeMock.isExpanded(selectionPathMock) } returns true

      classUnderTest.update(mockedActionEvent)

      assertSoftly { isVisible shouldBe false }
    }

    should("is visible from context menu if file explorer view is not null and selected node is UssDirNode and path is expanded") {
      var isVisible = false
      every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedNodeDataForTest)
      every {
        filesTreeMock.isExpanded(selectionPathMock)
      } answers {
        isVisible = true
        true
      }

      classUnderTest.update(mockedActionEvent)
      assertSoftly { isVisible shouldBe true }
    }

    should("is visible from context menu if file explorer view is not null and selected node is UssDirNode and path is not expanded") {
      var isVisible = true
      every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedNodeDataForTest)
      every {
        filesTreeMock.isExpanded(selectionPathMock)
      } answers {
        isVisible = false
        false
      }

      classUnderTest.update(mockedActionEvent)

      assertSoftly { isVisible shouldBe false }
    }

    should("is visible from context menu if file explorer view is not null and selectedNodes size > 1") {
      var isVisible = true
      every {
        mockedFileExplorerView.mySelectedNodesData
      } answers {
        isVisible = false
        listOf(mockedNodeDataForTest, mockedNodeDataForTest)
      }

      classUnderTest.update(mockedActionEvent)

      assertSoftly { isVisible shouldBe false }
    }

    should("return EDT thread_whenGetActionUpdateThread_givenNothing") {
      //when
      val thread = classUnderTest.actionUpdateThread
      //then
      assertSoftly { thread shouldBe ActionUpdateThread.EDT }
    }
  }
})
