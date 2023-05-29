/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.SortQueryKeys
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import eu.ibagroup.formainframe.dataops.fetch.UssFileFetchProvider
import eu.ibagroup.formainframe.dataops.fetch.UssQuery
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import javax.swing.tree.TreePath

class UssSortActionHolderTestSpec : ShouldSpec({
  beforeSpec {
    // FIXTURE SETUP TO HAVE ACCESS TO APPLICATION INSTANCE
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
    val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor, "for-mainframe")
    val fixture = fixtureBuilder.fixture
    val myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
      fixture,
      LightTempDirTestFixtureImpl(true)
    )
    myFixture.setUp()
  }

  afterSpec {
    clearAllMocks()
  }

  context("sort actions") {

    val dataOpsManagerService = ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
    val mockedFileFetchProvider = mockk<UssFileFetchProvider>()
    dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(ApplicationManager.getApplication()) {
      @Suppress("UNCHECKED_CAST")
      override fun <R : Any, Q : Query<R, Unit>, File : VirtualFile> getFileFetchProvider(
        requestClass: Class<out R>,
        queryClass: Class<out Query<*, *>>,
        vFileClass: Class<out File>
      ): FileFetchProvider<R, Q, File> {
        return mockedFileFetchProvider as FileFetchProvider<R, Q, File>
      }

    }
    val mockedActionEvent = mockk<AnActionEvent>()
    every { mockedActionEvent.presentation } returns mockk()
    every { mockedActionEvent.presentation.putClientProperty<Boolean>(any(), any()) } just Runs
    // isFromContextMenu is marked as deprecated
    every { mockedActionEvent.isFromContextMenu } returns false
    every { mockedActionEvent.presentation.isEnabledAndVisible = true } just Runs
    every { mockedActionEvent.presentation.isEnabledAndVisible = false } just Runs

    context("common spec") {

      val mockedFileExplorerView = mockk<FileExplorerView>()

      // Target UssDirNode + Query for test
      val mockedMFVirtualFile = mockk<MFVirtualFile>()
      val mockedUssDirNode = mockk<UssDirNode>()
      val mockedUssRemoteAttributes = mockk<RemoteUssAttributes>()
      val mockedUssQuery = mockk<UnitRemoteQueryImpl<ConnectionConfig, UssQuery>>()
      every { mockedUssDirNode.virtualFile } returns mockedMFVirtualFile
      every { mockedUssDirNode.query } returns mockedUssQuery
      every { mockedUssDirNode.cleanCache(false) } just Runs
      every { mockedUssQuery.setProperty("requester").value(mockedUssDirNode) } just Runs
      every { mockedUssQuery.sortKeys } returns mutableListOf()

      // NodeData for test
      val mockedNodeDataForTest = NodeData(mockedUssDirNode, mockedMFVirtualFile, mockedUssRemoteAttributes)
      mockkObject(mockedNodeDataForTest)

      // Common config for test
      every { mockedActionEvent.getExplorerView<FileExplorerView>() } returns mockedFileExplorerView
      every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedNodeDataForTest)

      context("sort action group spec") {

        // group action to spy
        val mockedActionGroup = spyk(SortActionGroup())

        val selectionPath = mockk<TreePath>()
        every { mockedActionEvent.getExplorerView<FileExplorerView>() } returns mockedFileExplorerView
        every { mockedFileExplorerView.myTree } returns mockk()
        every { mockedFileExplorerView.myTree.selectionPath } returns selectionPath

        should("is visible from context menu if file explorer view is null") {
          var isVisible = true
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } answers {
            isVisible = false
            null
          }

          mockedActionGroup.update(mockedActionEvent)
          assertSoftly {
            isVisible shouldBe false
          }
        }

        should("is visible from context menu if file explorer view is not null and selected node is not UssDirNode") {
          var isVisible = true
          val mockedNodeDataNotUssForTest = NodeData(mockk<LibraryNode>(), mockk<MFVirtualFile>(), mockk<RemoteDatasetAttributes>())
          every { mockedFileExplorerView.mySelectedNodesData } answers {
            isVisible = false
            listOf(mockedNodeDataNotUssForTest)
          }
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } returns mockedFileExplorerView

          mockedActionGroup.update(mockedActionEvent)
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

          mockedActionGroup.update(mockedActionEvent)
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

          mockedActionGroup.update(mockedActionEvent)
          assertSoftly {
            isVisible shouldBe false
          }
        }
      }

      context("sort by name action spec") {

        // action to spy
        val mockedSortActionByName = spyk(SortByNameAction())

        should("sort by name action performed if file explorer is null") {
          var actionPerformed = false
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } answers {
            actionPerformed = true
            null
          }
          mockedSortActionByName.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort by name action isSelected if file explorer is null") {
          var isSelected = true
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } answers {
            isSelected = false
            null
          }
          mockedSortActionByName.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("sort by name action performed if selected node is not UssDirNode") {
          var actionPerformed = false
          val mockedNodeDataNotUssForTest = NodeData(mockk<LibraryNode>(), mockk<MFVirtualFile>(), mockk<RemoteDatasetAttributes>())
          every { mockedFileExplorerView.mySelectedNodesData } answers {
            actionPerformed = true
            listOf(mockedNodeDataNotUssForTest)
          }
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } returns mockedFileExplorerView
          mockedSortActionByName.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort by name action isSelected if selected node is not UssDirNode") {
          var isSelected = true
          val mockedNodeDataNotUssForTest = NodeData(mockk<LibraryNode>(), mockk<MFVirtualFile>(), mockk<RemoteDatasetAttributes>())
          every { mockedFileExplorerView.mySelectedNodesData } answers {
            isSelected = false
            listOf(mockedNodeDataNotUssForTest)
          }
          mockedSortActionByName.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("sort by name action performed if sort query keys is empty") {
          var actionPerformed = false
          every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedNodeDataForTest)
          every { mockedUssDirNode.currentSortQueryKeysList } returns mutableListOf()
          every { mockedFileFetchProvider.reload(mockedUssQuery) } answers {
            actionPerformed = true
          }

          mockedSortActionByName.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort by name action isSelected if current sort query keys does not contain NAME key") {
          var isSelected = true
          every { mockedUssDirNode.currentSortQueryKeysList } answers {
            isSelected = false
            mutableListOf()
          }
          mockedSortActionByName.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("sort by name action performed if sort query keys is not empty") {
          var actionPerformed = false
          every { mockedUssDirNode.currentSortQueryKeysList } returns mutableListOf(SortQueryKeys.DATE, SortQueryKeys.ASCENDING)
          every { mockedFileFetchProvider.reload(mockedUssQuery) } answers {
            actionPerformed = true
          }

          mockedSortActionByName.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort by name action performed if sort query keys is not empty and contains desired key") {
          var actionPerformed = false
          every { mockedUssDirNode.currentSortQueryKeysList } returns mutableListOf(SortQueryKeys.DATE, SortQueryKeys.NAME, SortQueryKeys.ASCENDING)
          every { mockedFileFetchProvider.reload(mockedUssQuery) } answers {
            actionPerformed = true
          }

          mockedSortActionByName.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort by name action isSelected if current sort query keys contain NAME key") {
          var isSelected = false
          every { mockedUssDirNode.currentSortQueryKeysList } answers {
            isSelected = true
            mutableListOf(SortQueryKeys.NAME)
          }
          mockedSortActionByName.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe true
          }
        }

        should("sort by name action miscellaneous") {
          mockedSortActionByName.setSelected(mockedActionEvent, true)
          mockedSortActionByName.setSelected(mockedActionEvent, false)
          mockedSortActionByName.update(mockedActionEvent)
          val aware = mockedSortActionByName.isDumbAware
          assertSoftly {
            aware shouldBe true
          }
        }
      }

      context("sort by type action spec") {

        // action to spy
        val mockedSortActionByType = spyk(SortByTypeAction())

        should("sort by type action performed if file explorer is null") {
          var actionPerformed = false
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } answers {
            actionPerformed = true
            null
          }
          mockedSortActionByType.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort by type action isSelected if file explorer is null") {
          var isSelected = true
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } answers {
            isSelected = false
            null
          }
          mockedSortActionByType.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("sort by type action performed if selected node is not UssDirNode") {
          var actionPerformed = false
          val mockedNodeDataNotUssForTest = NodeData(mockk<LibraryNode>(), mockk<MFVirtualFile>(), mockk<RemoteDatasetAttributes>())
          every { mockedFileExplorerView.mySelectedNodesData } answers {
            actionPerformed = true
            listOf(mockedNodeDataNotUssForTest)
          }
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } returns mockedFileExplorerView
          mockedSortActionByType.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort by type action isSelected if selected node is not UssDirNode") {
          var isSelected = true
          val mockedNodeDataNotUssForTest = NodeData(mockk<LibraryNode>(), mockk<MFVirtualFile>(), mockk<RemoteDatasetAttributes>())
          every { mockedFileExplorerView.mySelectedNodesData } answers {
            isSelected = false
            listOf(mockedNodeDataNotUssForTest)
          }
          mockedSortActionByType.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("sort by type action performed if sort query keys is empty") {
          var actionPerformed = false
          every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedNodeDataForTest)
          every { mockedUssDirNode.currentSortQueryKeysList } returns mutableListOf()
          every { mockedFileFetchProvider.reload(mockedUssQuery) } answers {
            actionPerformed = true
          }

          mockedSortActionByType.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort by type action isSelected if current sort query keys does not contain TYPE key") {
          var isSelected = true
          every { mockedUssDirNode.currentSortQueryKeysList } answers {
            isSelected = false
            mutableListOf()
          }
          mockedSortActionByType.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("sort by type action performed if sort query keys is not empty") {
          var actionPerformed = false
          every { mockedUssDirNode.currentSortQueryKeysList } returns mutableListOf(SortQueryKeys.DATE, SortQueryKeys.ASCENDING)
          every { mockedFileFetchProvider.reload(mockedUssQuery) } answers {
            actionPerformed = true
          }

          mockedSortActionByType.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort by type action performed if sort query keys is not empty and contains desired key") {
          var actionPerformed = false
          every { mockedUssDirNode.currentSortQueryKeysList } returns mutableListOf(SortQueryKeys.DATE, SortQueryKeys.TYPE, SortQueryKeys.ASCENDING)
          every { mockedFileFetchProvider.reload(mockedUssQuery) } answers {
            actionPerformed = true
          }

          mockedSortActionByType.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort by type action isSelected if current sort query keys contain TYPE key") {
          var isSelected = false
          every { mockedUssDirNode.currentSortQueryKeysList } answers {
            isSelected = true
            mutableListOf(SortQueryKeys.TYPE)
          }
          mockedSortActionByType.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe true
          }
        }

        should("sort by type action miscellaneous") {
          mockedSortActionByType.setSelected(mockedActionEvent, true)
          mockedSortActionByType.setSelected(mockedActionEvent, false)
          mockedSortActionByType.update(mockedActionEvent)
          val aware = mockedSortActionByType.isDumbAware
          assertSoftly {
            aware shouldBe true
          }
        }
      }

      context("sort by date action spec") {

        // action to spy
        val mockedSortActionByDate = spyk(SortByModificationDateAction())

        should("sort by date action performed if file explorer is null") {
          var actionPerformed = false
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } answers {
            actionPerformed = true
            null
          }
          mockedSortActionByDate.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort by date action isSelected if file explorer is null") {
          var isSelected = true
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } answers {
            isSelected = false
            null
          }
          mockedSortActionByDate.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("sort by date action performed if selected node is not UssDirNode") {
          var actionPerformed = false
          val mockedNodeDataNotUssForTest = NodeData(mockk<LibraryNode>(), mockk<MFVirtualFile>(), mockk<RemoteDatasetAttributes>())
          every { mockedFileExplorerView.mySelectedNodesData } answers {
            actionPerformed = true
            listOf(mockedNodeDataNotUssForTest)
          }
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } returns mockedFileExplorerView
          mockedSortActionByDate.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort by date action isSelected if selected node is not UssDirNode") {
          var isSelected = true
          val mockedNodeDataNotUssForTest = NodeData(mockk<LibraryNode>(), mockk<MFVirtualFile>(), mockk<RemoteDatasetAttributes>())
          every { mockedFileExplorerView.mySelectedNodesData } answers {
            isSelected = false
            listOf(mockedNodeDataNotUssForTest)
          }
          mockedSortActionByDate.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("sort by date action performed if sort query keys is empty") {
          var actionPerformed = false
          every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedNodeDataForTest)
          every { mockedUssDirNode.currentSortQueryKeysList } returns mutableListOf()
          every { mockedFileFetchProvider.reload(mockedUssQuery) } answers {
            actionPerformed = true
          }

          mockedSortActionByDate.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort by date action isSelected if current sort query keys does not contain DATE key") {
          var isSelected = true
          every { mockedUssDirNode.currentSortQueryKeysList } answers {
            isSelected = false
            mutableListOf()
          }
          mockedSortActionByDate.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("sort by date action performed if sort query keys is not empty") {
          var actionPerformed = false
          every { mockedUssDirNode.currentSortQueryKeysList } returns mutableListOf(SortQueryKeys.NAME, SortQueryKeys.ASCENDING)
          every { mockedFileFetchProvider.reload(mockedUssQuery) } answers {
            actionPerformed = true
          }

          mockedSortActionByDate.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort by date action performed if sort query keys is not empty and contains desired key") {
          var actionPerformed = false
          every { mockedUssDirNode.currentSortQueryKeysList } returns mutableListOf(SortQueryKeys.DATE, SortQueryKeys.NAME, SortQueryKeys.ASCENDING)
          every { mockedFileFetchProvider.reload(mockedUssQuery) } answers {
            actionPerformed = true
          }

          mockedSortActionByDate.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort by date action isSelected if current sort query keys contain DATE key") {
          var isSelected = false
          every { mockedUssDirNode.currentSortQueryKeysList } answers {
            isSelected = true
            mutableListOf(SortQueryKeys.DATE)
          }
          mockedSortActionByDate.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe true
          }
        }

        should("sort by date action miscellaneous") {
          mockedSortActionByDate.setSelected(mockedActionEvent, true)
          mockedSortActionByDate.setSelected(mockedActionEvent, false)
          mockedSortActionByDate.update(mockedActionEvent)
          val aware = mockedSortActionByDate.isDumbAware
          assertSoftly {
            aware shouldBe true
          }
        }
      }

      context("sort ascending action spec") {

        // action to spy
        val mockedSortActionAscending = spyk(SortByAscendingOrderAction())

        should("sort Ascending action performed if file explorer is null") {
          var actionPerformed = false
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } answers {
            actionPerformed = true
            null
          }
          mockedSortActionAscending.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort Ascending action isSelected if file explorer is null") {
          var isSelected = true
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } answers {
            isSelected = false
            null
          }
          mockedSortActionAscending.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("sort Ascending action performed if selected node is not UssDirNode") {
          var actionPerformed = false
          val mockedNodeDataNotUssForTest = NodeData(mockk<LibraryNode>(), mockk<MFVirtualFile>(), mockk<RemoteDatasetAttributes>())
          every { mockedFileExplorerView.mySelectedNodesData } answers {
            actionPerformed = true
            listOf(mockedNodeDataNotUssForTest)
          }
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } returns mockedFileExplorerView
          mockedSortActionAscending.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort Ascending action isSelected if selected node is not UssDirNode") {
          var isSelected = true
          val mockedNodeDataNotUssForTest = NodeData(mockk<LibraryNode>(), mockk<MFVirtualFile>(), mockk<RemoteDatasetAttributes>())
          every { mockedFileExplorerView.mySelectedNodesData } answers {
            isSelected = false
            listOf(mockedNodeDataNotUssForTest)
          }
          mockedSortActionAscending.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("sort Ascending action performed if sort query keys is empty") {
          var actionPerformed = false
          every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedNodeDataForTest)
          every { mockedUssDirNode.currentSortQueryKeysList } returns mutableListOf()
          every { mockedFileFetchProvider.reload(mockedUssQuery) } answers {
            actionPerformed = true
          }

          mockedSortActionAscending.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort Ascending action isSelected if current sort query keys does not contain ASCENDING key") {
          var isSelected = true
          every { mockedUssDirNode.currentSortQueryKeysList } answers {
            isSelected = false
            mutableListOf()
          }
          mockedSortActionAscending.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("sort Ascending action performed if sort query keys is not empty") {
          var actionPerformed = false
          every { mockedUssDirNode.currentSortQueryKeysList } returns mutableListOf(SortQueryKeys.NAME, SortQueryKeys.DESCENDING)
          every { mockedFileFetchProvider.reload(mockedUssQuery) } answers {
            actionPerformed = true
          }

          mockedSortActionAscending.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort Ascending action performed if sort query keys is not empty and contains desired key") {
          var actionPerformed = false
          every { mockedUssDirNode.currentSortQueryKeysList } returns mutableListOf(SortQueryKeys.DESCENDING, SortQueryKeys.NAME, SortQueryKeys.ASCENDING)
          every { mockedFileFetchProvider.reload(mockedUssQuery) } answers {
            actionPerformed = true
          }

          mockedSortActionAscending.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort Ascending action isSelected if current sort query keys contain ASCENDING key") {
          var isSelected = false
          every { mockedUssDirNode.currentSortQueryKeysList } answers {
            isSelected = true
            mutableListOf(SortQueryKeys.ASCENDING)
          }
          mockedSortActionAscending.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe true
          }
        }

        should("sort Ascending action miscellaneous") {
          mockedSortActionAscending.setSelected(mockedActionEvent, true)
          mockedSortActionAscending.setSelected(mockedActionEvent, false)
          mockedSortActionAscending.update(mockedActionEvent)
          val aware = mockedSortActionAscending.isDumbAware
          assertSoftly {
            aware shouldBe true
          }
        }
      }

      context("sort descending action spec") {

        // action to spy
        val mockedSortActionDescending = spyk(SortByDescendingOrderAction())

        should("sort Descending action performed if file explorer is null") {
          var actionPerformed = false
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } answers {
            actionPerformed = true
            null
          }
          mockedSortActionDescending.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort Descending action isSelected if file explorer is null") {
          var isSelected = true
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } answers {
            isSelected = false
            null
          }
          mockedSortActionDescending.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("sort Descending action performed if selected node is not UssDirNode") {
          var actionPerformed = false
          val mockedNodeDataNotUssForTest = NodeData(mockk<LibraryNode>(), mockk<MFVirtualFile>(), mockk<RemoteDatasetAttributes>())
          every { mockedFileExplorerView.mySelectedNodesData } answers {
            actionPerformed = true
            listOf(mockedNodeDataNotUssForTest)
          }
          every { mockedActionEvent.getExplorerView<FileExplorerView>() } returns mockedFileExplorerView
          mockedSortActionDescending.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort Descending action isSelected if selected node is not UssDirNode") {
          var isSelected = true
          val mockedNodeDataNotUssForTest = NodeData(mockk<LibraryNode>(), mockk<MFVirtualFile>(), mockk<RemoteDatasetAttributes>())
          every { mockedFileExplorerView.mySelectedNodesData } answers {
            isSelected = false
            listOf(mockedNodeDataNotUssForTest)
          }
          mockedSortActionDescending.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("sort Descending action performed if sort query keys is empty") {
          var actionPerformed = false
          every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedNodeDataForTest)
          every { mockedUssDirNode.currentSortQueryKeysList } returns mutableListOf()
          every { mockedFileFetchProvider.reload(mockedUssQuery) } answers {
            actionPerformed = true
          }

          mockedSortActionDescending.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort Descending action isSelected if current sort query keys does not contain DESCENDING key") {
          var isSelected = true
          every { mockedUssDirNode.currentSortQueryKeysList } answers {
            isSelected = false
            mutableListOf()
          }
          mockedSortActionDescending.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe false
          }
        }

        should("sort Descending action performed if sort query keys is not empty") {
          var actionPerformed = false
          every { mockedUssDirNode.currentSortQueryKeysList } returns mutableListOf(SortQueryKeys.NAME, SortQueryKeys.ASCENDING)
          every { mockedFileFetchProvider.reload(mockedUssQuery) } answers {
            actionPerformed = true
          }

          mockedSortActionDescending.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort Descending action performed if sort query keys is not empty and contains desired key") {
          var actionPerformed = false
          every { mockedUssDirNode.currentSortQueryKeysList } returns mutableListOf(SortQueryKeys.DESCENDING, SortQueryKeys.NAME, SortQueryKeys.ASCENDING)
          every { mockedFileFetchProvider.reload(mockedUssQuery) } answers {
            actionPerformed = true
          }

          mockedSortActionDescending.actionPerformed(mockedActionEvent)
          assertSoftly {
            actionPerformed shouldBe true
          }
        }

        should("sort Descending action isSelected if current sort query keys contain DESCENDING key") {
          var isSelected = false
          every { mockedUssDirNode.currentSortQueryKeysList } answers {
            isSelected = true
            mutableListOf(SortQueryKeys.DESCENDING)
          }
          mockedSortActionDescending.isSelected(mockedActionEvent)
          assertSoftly {
            isSelected shouldBe true
          }
        }

        should("sort Descending action miscellaneous") {
          mockedSortActionDescending.setSelected(mockedActionEvent, true)
          mockedSortActionDescending.setSelected(mockedActionEvent, false)
          mockedSortActionDescending.update(mockedActionEvent)
          val aware = mockedSortActionDescending.isDumbAware
          assertSoftly {
            aware shouldBe true
          }
        }
      }
    }
  }

})