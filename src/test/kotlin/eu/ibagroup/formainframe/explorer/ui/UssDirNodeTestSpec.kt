/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.SortQueryKeys
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributesService
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.explorer.FileExplorer
import eu.ibagroup.formainframe.explorer.FilesWorkingSetImpl
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

class UssDirNodeTestSpec : ShouldSpec({
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
  context("explorer module: ui/UssDirNode") {
    val mockedPath = mockk<UssPath>("USS//test_path/ZOSMFAD/test_dir")
    val mockedProject = mockk<Project>()
    val mockedExplorerTreeNodeParent = mockk<UssDirNode>()
    val mockedWorkingSet = mockk<FilesWorkingSetImpl>()
    val mockedExplorer = mockk<FileExplorer>()
    val mockedExplorerTreeStructure = mockk<ExplorerTreeStructureBase>()
    val isRootNode = false

    val mockedUssAttributesService = mockk<RemoteUssAttributesService>()

    every { mockedWorkingSet.explorer } returns mockedExplorer
    every { mockedExplorer.componentManager } returns mockk()
    every { mockedExplorer.componentManager.service<DataOpsManager>() } returns mockk()
    every { mockedExplorer.componentManager.service<DataOpsManager>().getAttributesService<RemoteUssAttributes, MFVirtualFile>() } returns mockedUssAttributesService
    every { mockedExplorerTreeStructure.registerNode(any()) } just Runs

    context("sort children nodes") {
      val mockedVFile = mockk<MFVirtualFile>()
      val childNode3 = mockk<UssFileNode>()
      val childNode4 = mockk<UssFileNode>()
      val unexpectedNode = mockk<LibraryNode>()
      val mockedVFileChild1 = mockk<MFVirtualFile>()
      val mockedVFileChild2 = mockk<MFVirtualFile>()
      val mockedAttributes1 = mockk<RemoteUssAttributes>()
      val mockedAttributes2 = mockk<RemoteUssAttributes>()
      val mockedAttributes3 = mockk<RemoteUssAttributes>()
      val mockedAttributes4 = mockk<RemoteUssAttributes>()

      beforeEach {
        every { mockedVFile.filenameInternal } returns "test_dir"
        every { mockedVFileChild1.filenameInternal } returns "a_dir"
        every { mockedVFileChild2.filenameInternal } returns "b_dir"
        every { childNode3.virtualFile } returns mockk()
        every { childNode3.virtualFile.filenameInternal } returns "a_file"
        every { childNode4.virtualFile } returns mockk()
        every { childNode4.virtualFile.filenameInternal } returns "b_file"

        every { mockedUssAttributesService.getAttributes(mockedVFileChild1) } returns mockedAttributes1
        every { mockedUssAttributesService.getAttributes(mockedVFileChild2) } returns mockedAttributes2
        every { mockedUssAttributesService.getAttributes(childNode3.virtualFile) } returns mockedAttributes3
        every { mockedUssAttributesService.getAttributes(childNode4.virtualFile) } returns mockedAttributes4

        every { mockedAttributes1.modificationTime } returns "2022/12/11"
        every { mockedAttributes2.modificationTime } returns "2022/12/10"
        every { mockedAttributes3.modificationTime } returns "2022/12/09"
        every { mockedAttributes4.modificationTime } returns "2022/12/08"
      }

      val mockedUssDirNodeChild1 = spyk(
        UssDirNode(mockedPath, mockedProject, mockedExplorerTreeNodeParent, mockedWorkingSet, mockedExplorerTreeStructure, mockedVFileChild1, isRootNode)
      )
      val mockedUssDirNodeChild2 = spyk(
        UssDirNode(mockedPath, mockedProject, mockedExplorerTreeNodeParent, mockedWorkingSet, mockedExplorerTreeStructure, mockedVFileChild2, isRootNode)
      )
      val mockedUssDirNode = spyk(
        UssDirNode(mockedPath, mockedProject, mockedExplorerTreeNodeParent, mockedWorkingSet, mockedExplorerTreeStructure, mockedVFile, isRootNode)
      )

      val mockedChildrenNodes = listOf<AbstractTreeNode<*>>(mockedUssDirNodeChild1, mockedUssDirNodeChild2, childNode3, childNode4, unexpectedNode)

      should("sort by name ascending") {

        val sortQueryKeys = listOf(SortQueryKeys.NAME, SortQueryKeys.ASCENDING)
        mockkObject(sortQueryKeys)

        val expected = listOf(unexpectedNode, mockedUssDirNodeChild1, childNode3, mockedUssDirNodeChild2, childNode4)
        val actual = sortChildrenForTest(mockedChildrenNodes, sortQueryKeys).invoke(mockedUssDirNode)

        assertSoftly {
          actual shouldBe expected
        }
      }

      should("sort by name descending") {

        val sortQueryKeys = listOf(SortQueryKeys.NAME, SortQueryKeys.DESCENDING)
        mockkObject(sortQueryKeys)

        val expected = listOf(childNode4, mockedUssDirNodeChild2, childNode3, mockedUssDirNodeChild1, unexpectedNode)
        val actual = sortChildrenForTest(mockedChildrenNodes, sortQueryKeys).invoke(mockedUssDirNode)

        assertSoftly {
          actual shouldBe expected
        }
      }

      should("sort by type ascending") {

        val sortQueryKeys = listOf(SortQueryKeys.TYPE, SortQueryKeys.ASCENDING)
        mockkObject(sortQueryKeys)

        val expected = listOf(mockedUssDirNodeChild1, mockedUssDirNodeChild2, childNode3, childNode4)
        val actual = sortChildrenForTest(mockedChildrenNodes, sortQueryKeys).invoke(mockedUssDirNode)

        assertSoftly {
          actual shouldBe expected
        }
      }

      should("sort by type descending") {

        val sortQueryKeys = listOf(SortQueryKeys.TYPE, SortQueryKeys.DESCENDING)
        mockkObject(sortQueryKeys)

        val expected = listOf(mockedUssDirNodeChild2, mockedUssDirNodeChild1, childNode4, childNode3)
        val actual = sortChildrenForTest(mockedChildrenNodes, sortQueryKeys).invoke(mockedUssDirNode)

        assertSoftly {
          actual shouldBe expected
        }
      }

      should("sort by date ascending") {

        val sortQueryKeys = listOf(SortQueryKeys.DATE, SortQueryKeys.ASCENDING)
        mockkObject(sortQueryKeys)

        val expected = listOf(unexpectedNode, childNode4, childNode3, mockedUssDirNodeChild2, mockedUssDirNodeChild1)
        val actual = sortChildrenForTest(mockedChildrenNodes, sortQueryKeys).invoke(mockedUssDirNode)

        assertSoftly {
          actual shouldBe expected
        }
      }

      should("sort by date descending") {

        val sortQueryKeys = listOf(SortQueryKeys.DATE, SortQueryKeys.DESCENDING)
        mockkObject(sortQueryKeys)

        val expected = listOf(mockedUssDirNodeChild1, mockedUssDirNodeChild2, childNode3, childNode4, unexpectedNode)
        val actual = sortChildrenForTest(mockedChildrenNodes, sortQueryKeys).invoke(mockedUssDirNode)

        assertSoftly {
          actual shouldBe expected
        }
      }

      should("sort by none key") {

        val sortQueryKeys = listOf(SortQueryKeys.NONE)
        mockkObject(sortQueryKeys)

        val expected = listOf(mockedUssDirNodeChild1, mockedUssDirNodeChild2, childNode3, childNode4, unexpectedNode)
        val actual = sortChildrenForTest(mockedChildrenNodes, sortQueryKeys).invoke(mockedUssDirNode)

        assertSoftly {
          actual shouldBe expected
        }
      }
    }
  }
})