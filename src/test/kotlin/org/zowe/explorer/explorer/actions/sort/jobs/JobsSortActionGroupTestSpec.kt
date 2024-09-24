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

package org.zowe.explorer.explorer.actions.sort.jobs

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.Presentation
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.JobsFilter
import org.zowe.explorer.dataops.UnitRemoteQueryImpl
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import javax.swing.tree.TreePath

class JobsSortActionGroupTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("jobs sort action group spec") {

    // group action to spy
    val classUnderTest = spyk(JobsSortActionGroup())

    val mockedActionEvent = mockk<AnActionEvent>()
    val selectionPath = mockk<TreePath>()
    val mockedFileExplorerView = mockk<JesExplorerView>()
    every { mockedActionEvent.getExplorerView<JesExplorerView>() } returns mockedFileExplorerView
    every { mockedFileExplorerView.myTree } returns mockk()
    every { mockedFileExplorerView.myTree.selectionPath } returns selectionPath

    // Presentation
    val presentation = Presentation()
    every { mockedActionEvent.presentation } returns presentation

    // Target UssDirNode + Query for test
    val mockedMFVirtualFile = mockk<MFVirtualFile>()
    val mockedJesFilterNode = mockk<JesFilterNode>()
    val mockedJobRemoteAttributes = mockk<RemoteJobAttributes>()
    val mockedJobQuery = mockk<UnitRemoteQueryImpl<ConnectionConfig, JobsFilter>>()
    every { mockedJesFilterNode.virtualFile } returns mockedMFVirtualFile
    every { mockedJesFilterNode.query } returns mockedJobQuery

    // NodeData for test
    val mockedNodeDataForTest = NodeData(mockedJesFilterNode, mockedMFVirtualFile, mockedJobRemoteAttributes)
    mockkObject(mockedNodeDataForTest)

    should("shouldReturnExplorerView_whenGetExplorerView_givenActionEvent") {
      every { mockedActionEvent.getData(any() as DataKey<JesExplorerView>) } returns mockedFileExplorerView
      val actualExplorer = classUnderTest.getSourceView(mockedActionEvent)

      assertSoftly {
        actualExplorer shouldNotBe null
        actualExplorer is JesExplorerView
      }
    }

    should("shouldReturnTrue_whenCheckNode_givenJesFilterNode") {
      val nodeMock = mockk<JesFilterNode>()
      val checkNode = classUnderTest.checkNode(nodeMock)

      assertSoftly {
        checkNode shouldBe true
      }
    }

    should("shouldReturnNull_whenGetExplorerView_givenActionEvent") {
      every { mockedActionEvent.getData(any() as DataKey<JesExplorerView>) } returns null
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
      every { mockedActionEvent.getExplorerView<JesExplorerView>() } answers {
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
      val mockedNodeDataNotJesFilterForTest =
        NodeData(mockk<LibraryNode>(), mockk(), mockk<RemoteDatasetAttributes>())
      every { mockedFileExplorerView.mySelectedNodesData } answers {
        isVisible = false
        listOf(mockedNodeDataNotJesFilterForTest)
      }
      every { mockedFileExplorerView.myTree.isExpanded(selectionPath) } returns true
      every { mockedActionEvent.getExplorerView<JesExplorerView>() } returns mockedFileExplorerView

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
