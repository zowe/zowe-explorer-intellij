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

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.Presentation
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.fetch.UssQuery
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import javax.swing.tree.TreePath

class UssSortActionGroupTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("uss sort action group spec") {

    // group action to spy
    val classUnderTest = spyk(UssSortActionGroup())

    val mockedActionEvent = mockk<AnActionEvent>()
    val selectionPath = mockk<TreePath>()
    val mockedFileExplorerView = mockk<FileExplorerView>()
    every { mockedActionEvent.getExplorerView<FileExplorerView>() } returns mockedFileExplorerView
    every { mockedFileExplorerView.myTree } returns mockk()
    every { mockedFileExplorerView.myTree.selectionPath } returns selectionPath

    // Presentation
    val presentation = Presentation()
    every { mockedActionEvent.presentation } returns presentation

    // Target UssDirNode + Query for test
    val mockedMFVirtualFile = mockk<MFVirtualFile>()
    val mockedUssDirNode = mockk<UssDirNode>()
    val mockedUssRemoteAttributes = mockk<RemoteUssAttributes>()
    val mockedUssQuery = mockk<UnitRemoteQueryImpl<ConnectionConfig, UssQuery>>()
    every { mockedUssDirNode.virtualFile } returns mockedMFVirtualFile
    every { mockedUssDirNode.query } returns mockedUssQuery

    // NodeData for test
    val mockedNodeDataForTest = NodeData(mockedUssDirNode, mockedMFVirtualFile, mockedUssRemoteAttributes)
    mockkObject(mockedNodeDataForTest)

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

      assertSoftly {
        checkNode shouldBe true
      }
    }

    should("shouldReturnNull_whenGetExplorerView_givenActionEvent") {
      every { mockedActionEvent.getData(any() as DataKey<FileExplorerView>) } returns null
      val actualExplorer = classUnderTest.getSourceView(mockedActionEvent)

      assertSoftly {
        actualExplorer shouldBe null
      }
    }

    should("shouldReturnFalse_whenCheckNode_givenWrongNode") {
      val nodeMock = mockk<DSMaskNode>()
      val checkNode = classUnderTest.checkNode(nodeMock)

      assertSoftly {
        checkNode shouldBe false
      }
    }

    should("is visible from context menu if file explorer view is null") {
      var isVisible = true
      every { mockedActionEvent.getExplorerView<FileExplorerView>() } answers {
        isVisible = false
        null
      }

      classUnderTest.update(mockedActionEvent)
      assertSoftly {
        isVisible shouldBe false
      }
    }

    should("is visible from context menu if file explorer view is not null and selected node is not UssDirNode") {
      var isVisible = true
      val mockedNodeDataNotUssForTest =
        NodeData(mockk<LibraryNode>(), mockk(), mockk<RemoteDatasetAttributes>())
      every { mockedFileExplorerView.mySelectedNodesData } answers {
        isVisible = false
        listOf(mockedNodeDataNotUssForTest)
      }
      every { mockedFileExplorerView.myTree.isExpanded(selectionPath) } returns true
      every { mockedActionEvent.getExplorerView<FileExplorerView>() } returns mockedFileExplorerView

      classUnderTest.update(mockedActionEvent)
      assertSoftly {
        isVisible shouldBe false
      }
    }

    should("is visible from context menu if file explorer view is not null and selected node is UssDirNode and path is expanded") {
      var isVisible = false
      every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedNodeDataForTest)
      every { mockedFileExplorerView.myTree.isExpanded(selectionPath) } answers {
        isVisible = true
        true
      }

      classUnderTest.update(mockedActionEvent)
      assertSoftly {
        isVisible shouldBe true
      }
    }

    should("is visible from context menu if file explorer view is not null and selected node is UssDirNode and path is not expanded") {
      var isVisible = true
      every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedNodeDataForTest)
      every { mockedFileExplorerView.myTree.isExpanded(selectionPath) } answers {
        isVisible = false
        false
      }

      classUnderTest.update(mockedActionEvent)
      assertSoftly {
        isVisible shouldBe false
      }
    }

    should("is visible from context menu if file explorer view is not null and selectedNodes size > 1") {
      var isVisible = true
      every { mockedFileExplorerView.mySelectedNodesData } answers {
        isVisible = false
        listOf(mockedNodeDataForTest, mockedNodeDataForTest)
      }

      classUnderTest.update(mockedActionEvent)
      assertSoftly {
        isVisible shouldBe false
      }
    }

    should("return EDT thread_whenGetActionUpdateThread_givenNothing") {
      //given

      //when
      val thread = classUnderTest.actionUpdateThread
      //then
      assertSoftly {
        thread shouldBe ActionUpdateThread.EDT
      }
    }
  }
})
