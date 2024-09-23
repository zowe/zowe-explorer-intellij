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

import com.intellij.ide.PasteProvider
import com.intellij.ide.dnd.DnDAction
import com.intellij.ide.dnd.DnDDragStartBean
import com.intellij.ide.dnd.DnDEvent
import com.intellij.ide.dnd.DnDSource
import com.intellij.ide.dnd.TransferableWrapper
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.impl.ProjectViewImpl
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.ui.awt.RelativeRectangle
import com.intellij.ui.treeStructure.Tree
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.MaskedRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.attributes.UssRequester
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.FilesWorkingSetImpl
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import groovy.lang.Tuple4
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import java.awt.Point
import java.awt.Rectangle
import java.awt.datatransfer.DataFlavor
import java.io.File
import javax.swing.JRootPane
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

class FileExplorerViewDropTargetTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }

  context("explorer module: ui/FileExplorerViewDropTarget") {

    var isUpdatePerformed = false
    var isDropPossible = false
    var isDragHighlighted = false

    beforeEach {
      isUpdatePerformed = false
      isDropPossible = false
      isDragHighlighted = false
    }

    val mockedDnDEvent = mockk<DnDEvent>()
    every { mockedDnDEvent.isDropPossible = any() as Boolean } answers {
      isDropPossible = true
    }
    every { mockedDnDEvent.setHighlighting(any() as RelativeRectangle, any() as Int) } answers {
      isDragHighlighted = true
      isUpdatePerformed = true
    }

    context("perform various drops and perform update") {

      val mockedJTree = mockk<Tree>()
      val mockedFileExplorer = mockk<Explorer<ConnectionConfig, FilesWorkingSetImpl>>()
      val mockedCopyPasterProvider = mockk<FileExplorerView.ExplorerCopyPasteSupport>()

      val fileExplorerViewDropTarget = spyk(
        FileExplorerViewDropTarget(mockedJTree, mockedFileExplorer, mockedCopyPasterProvider), recordPrivateCalls = true
      )

      mockkStatic(FileExplorerViewDragSource::class)
      mockkStatic(FileExplorerViewDragSource.ExplorerTransferableWrapper::class)

      val defaultBounds = Rectangle(50, 200, 100, 50)
      var defaultTarget = TreePath(arrayOf("project", "project", "test1", "test2"))
      var defaultSources = arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))
      val defaultSourceTargetBounds = Tuple4(defaultSources, defaultTarget, defaultBounds, mockedJTree)
      mockkObject(defaultSourceTargetBounds)
      var defaultSourcesList = defaultSources.toList()
      mockkObject(defaultSourcesList)

      every { fileExplorerViewDropTarget["getSourcesTargetAndBounds"](mockedDnDEvent) } returns defaultSourceTargetBounds
      every { mockedCopyPasterProvider.pasteProvider } returns mockk()
      every { mockedCopyPasterProvider.copyProvider } returns mockk()
      every { mockedCopyPasterProvider.cutProvider } returns mockk()
      every { defaultSourceTargetBounds.v1 } returns defaultSources

      every { mockedCopyPasterProvider.project } returns mockk()
      every { mockedCopyPasterProvider.cutProvider.isCutEnabled(any() as DataContext) } returns true
      every { mockedCopyPasterProvider.copyProvider.isCopyEnabled(any() as DataContext) } returns true

      var isCutPerformed: Boolean
      var isCopyPerformed: Boolean
      var isPastePerformed: Boolean

      val userDefinedExplorerPasteProvider = object : PasteProvider {
        override fun performPaste(dataContext: DataContext) {
          dataContext.getData(IS_DRAG_AND_DROP_KEY)
          dataContext.getData(CommonDataKeys.PROJECT)
          dataContext.getData(ExplorerDataKeys.NODE_DATA_ARRAY)
          dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
          dataContext.getData(DRAGGED_FROM_PROJECT_FILES_ARRAY)
          dataContext.getData(CommonDataKeys.VIRTUAL_FILE)
          isPastePerformed = true
          return
        }

        override fun isPastePossible(dataContext: DataContext): Boolean {
          return true
        }

        override fun isPasteEnabled(dataContext: DataContext): Boolean {
          return true
        }
      }

      every { mockedCopyPasterProvider.cutProvider.performCut(any() as DataContext) } answers {
        isCutPerformed = true
      }
      every { mockedCopyPasterProvider.copyProvider.performCopy(any() as DataContext) } answers {
        isCopyPerformed = true
      }
      every { mockedCopyPasterProvider.pasteProvider.performPaste(any() as DataContext) } answers {
        isPastePerformed = true
      }

      // drop
      should("perform paste from project files to the mainframe files through the copy provider") {
        every { mockedDnDEvent.attachedObject } answers {
          object : DnDSource {
            override fun canStartDragging(action: DnDAction?, dragOrigin: Point): Boolean {
              TODO("Not yet implemented")
            }

            override fun startDragging(action: DnDAction?, dragOrigin: Point): DnDDragStartBean {
              TODO("Not yet implemented")
            }
          }
        }
        every { defaultSourceTargetBounds.v4 } returns mockedJTree
        isCopyPerformed = false
        isPastePerformed = false
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCopyPerformed shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("perform paste from mainframe files to the project files through the copy provider") {
        every { mockedDnDEvent.attachedObject } answers {
          FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        }
        every { defaultSourceTargetBounds.v4 } returns mockk()
        isCopyPerformed = false
        isPastePerformed = false
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCopyPerformed shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("perform paste from local/remote through the cut provider") {
        every { mockedDnDEvent.attachedObject } answers {
          FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        }
        every { defaultSourceTargetBounds.v4 } returns mockedJTree
        isCutPerformed = false
        isPastePerformed = false
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe true
          isPastePerformed shouldBe true

        }
      }

      should("perform paste from local/remote if no cut/copy provider enabled") {
        every { mockedDnDEvent.attachedObject } answers {
          FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        }
        every { defaultSourceTargetBounds.v4 } returns mockk()
        every { mockedCopyPasterProvider.cutProvider.isCutEnabled(any() as DataContext) } returns false
        every { mockedCopyPasterProvider.copyProvider.isCopyEnabled(any() as DataContext) } returns false
        isCutPerformed = false
        isCopyPerformed = false
        isPastePerformed = false
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe false
          isCopyPerformed shouldBe false
          isPastePerformed shouldBe true

        }
      }

      should("perform paste from mainframe z/OS datasets to the USS files within one remote system") {
        val dataOpsManagerService = DataOpsManager.getService() as TestDataOpsManagerImpl
        every { mockedFileExplorer.componentManager } returns ApplicationManager.getApplication()
        val mockedAttributeService = mockk<AttributesService<FileAttributes, VirtualFile>>()
        val mockedParentDatasetAttributes = mockk<RemoteDatasetAttributes>()
        val mockedVirtualFileTarget = mockk<MFVirtualFile>()
        val mockedVirtualFileSource = mockk<MFVirtualFile>()
        val targetAttributes = mockk<RemoteUssAttributes>()
        val sourceAttributes = mockk<RemoteMemberAttributes>()
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
          override fun <A : FileAttributes, F : VirtualFile> getAttributesService(
            attributesClass: Class<out A>,
            vFileClass: Class<out F>
          ): AttributesService<A, F> {
            return mockedAttributeService as AttributesService<A, F>
          }

          override fun tryToGetAttributes(file: VirtualFile): FileAttributes? {
            return when (file) {
              mockedVirtualFileTarget -> targetAttributes
              mockedVirtualFileSource -> sourceAttributes
              else -> super.tryToGetAttributes(file)
            }
          }
        }
        every { mockedAttributeService.getAttributes(any() as VirtualFile) } returns mockedParentDatasetAttributes

        defaultTarget = TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD"))
        defaultSources = arrayOf(TreePath(arrayOf("ARST.*", "ARST.TEST", "SAMPLE")))
        every { defaultSourceTargetBounds.v1 } returns defaultSources
        every { defaultSourceTargetBounds.v2 } returns defaultTarget
        defaultSourcesList = defaultSources.toList()
        mockkObject(defaultTarget)
        val sources = defaultSourcesList[0]
        mockkObject(sources)
        every { mockedDnDEvent.attachedObject } answers {
          FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        }
        every { defaultSourceTargetBounds.v4 } returns mockedJTree
        isCutPerformed = false
        isPastePerformed = false

        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedStructureTreeModelNodeSource = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<UssFileNode>()
        val mockedNodeSource = mockk<FileLikeDatasetNode>()
        every { defaultTarget.lastPathComponent } returns mockedStructureTreeModelNodeTarget
        every { sources.lastPathComponent } returns mockedStructureTreeModelNodeSource
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedStructureTreeModelNodeSource.userObject } returns mockedNodeSource
        every { mockedNodeTarget.virtualFile } returns mockedVirtualFileTarget
        every { mockedNodeSource.virtualFile } returns mockedVirtualFileSource

        every { mockedFileExplorer.componentManager } returns mockk()
        every { sourceAttributes.parentFile } returns mockk()
        val conn1 = mockk<ConnectionConfig>()
        val conn2 = mockk<ConnectionConfig>()
        every { conn1.url } returns "https://test1:10443"
        every { conn2.url } returns "https://test1:10443"
        every { conn1.isAllowSelfSigned } returns true
        every { conn2.isAllowSelfSigned } returns true
        val requester1 = mockk<UssRequester>()
        val requester2 = mockk<MaskedRequester>()
        every { requester1.connectionConfig } returns conn1
        every { requester2.connectionConfig } returns conn2
        every { targetAttributes.requesters } returns mutableListOf(requester1)
        every { mockedParentDatasetAttributes.requesters } returns mutableListOf(requester2)

        every { mockedCopyPasterProvider.cutProvider.isCutEnabled(any() as DataContext) } returns true
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("perform paste from mainframe USS files to the datasets within one remote system") {
        defaultTarget = TreePath(arrayOf("ARST.*", "ARST.TEST"))
        defaultSources = arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))
        every { defaultSourceTargetBounds.v1 } returns defaultSources
        every { defaultSourceTargetBounds.v2 } returns defaultTarget
        defaultSourcesList = defaultSources.toList()
        mockkObject(defaultTarget)
        val sources = defaultSourcesList[0]
        mockkObject(sources)
        every { mockedDnDEvent.attachedObject } answers {
          FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        }
        every { defaultSourceTargetBounds.v4 } returns mockedJTree
        isCutPerformed = false
        isPastePerformed = false

        val mockedVirtualFileTarget = mockk<MFVirtualFile>()
        val mockedVirtualFileSource = mockk<MFVirtualFile>()

        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedStructureTreeModelNodeSource = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<LibraryNode>()
        val mockedNodeSource = mockk<UssFileNode>()
        every { defaultTarget.lastPathComponent } returns mockedStructureTreeModelNodeTarget
        every { sources.lastPathComponent } returns mockedStructureTreeModelNodeSource
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedStructureTreeModelNodeSource.userObject } returns mockedNodeSource
        every { mockedNodeTarget.virtualFile } returns mockedVirtualFileTarget
        every { mockedNodeSource.virtualFile } returns mockedVirtualFileSource

        every { mockedFileExplorer.componentManager } returns mockk()

        val targetAttributes = mockk<RemoteDatasetAttributes>()
        val sourceAttributes = mockk<RemoteUssAttributes>()

        val dataOpsManagerService = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
          override fun tryToGetAttributes(file: VirtualFile): FileAttributes? {
            return when (file) {
              mockedVirtualFileTarget -> targetAttributes
              mockedVirtualFileSource -> sourceAttributes
              else -> super.tryToGetAttributes(file)
            }
          }
        }

        val conn1 = mockk<ConnectionConfig>()
        val conn2 = mockk<ConnectionConfig>()
        every { conn1.url } returns "https://test1:10443"
        every { conn2.url } returns "https://test1:10443"
        every { conn1.isAllowSelfSigned } returns true
        every { conn2.isAllowSelfSigned } returns true
        val requester1 = mockk<MaskedRequester>()
        val requester2 = mockk<UssRequester>()
        every { requester1.connectionConfig } returns conn1
        every { requester2.connectionConfig } returns conn2
        every { targetAttributes.requesters } returns mutableListOf(requester1)
        every { sourceAttributes.requesters } returns mutableListOf(requester2)

        every { mockedCopyPasterProvider.cutProvider.isCutEnabled(any() as DataContext) } returns true
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("perform paste from mainframe USS files to the datasets within one remote system if cutProvider is not enabled") {
        defaultTarget = TreePath(arrayOf("ARST.*", "ARST.TEST"))
        defaultSources = arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))
        every { defaultSourceTargetBounds.v1 } returns defaultSources
        every { defaultSourceTargetBounds.v2 } returns defaultTarget
        defaultSourcesList = defaultSources.toList()
        mockkObject(defaultTarget)
        val sources = defaultSourcesList[0]
        mockkObject(sources)
        every { mockedDnDEvent.attachedObject } answers {
          FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        }
        every { defaultSourceTargetBounds.v4 } returns mockedJTree
        isCutPerformed = false
        isPastePerformed = false

        val mockedVirtualFileTarget = mockk<MFVirtualFile>()
        val mockedVirtualFileSource = mockk<MFVirtualFile>()

        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedStructureTreeModelNodeSource = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<LibraryNode>()
        val mockedNodeSource = mockk<UssFileNode>()
        every { defaultTarget.lastPathComponent } returns mockedStructureTreeModelNodeTarget
        every { sources.lastPathComponent } returns mockedStructureTreeModelNodeSource
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedStructureTreeModelNodeSource.userObject } returns mockedNodeSource
        every { mockedNodeTarget.virtualFile } returns mockedVirtualFileTarget
        every { mockedNodeSource.virtualFile } returns mockedVirtualFileSource

        every { mockedFileExplorer.componentManager } returns mockk()
        val targetAttributes = mockk<RemoteDatasetAttributes>()
        val sourceAttributes = mockk<RemoteUssAttributes>()

        val dataOpsManagerService = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
          override fun tryToGetAttributes(file: VirtualFile): FileAttributes? {
            return when (file) {
              mockedVirtualFileTarget -> targetAttributes
              mockedVirtualFileSource -> sourceAttributes
              else -> super.tryToGetAttributes(file)
            }
          }
        }

        val conn1 = mockk<ConnectionConfig>()
        val conn2 = mockk<ConnectionConfig>()
        every { conn1.url } returns "https://test1:10443"
        every { conn2.url } returns "https://test1:10443"
        every { conn1.isAllowSelfSigned } returns true
        every { conn2.isAllowSelfSigned } returns true
        val requester1 = mockk<MaskedRequester>()
        val requester2 = mockk<UssRequester>()
        every { requester1.connectionConfig } returns conn1
        every { requester2.connectionConfig } returns conn2
        every { targetAttributes.requesters } returns mutableListOf(requester1)
        every { sourceAttributes.requesters } returns mutableListOf(requester2)

        every { mockedCopyPasterProvider.cutProvider.isCutEnabled(any() as DataContext) } returns false
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe false
          isPastePerformed shouldBe true
        }
      }

      should("perform paste from mainframe USS files of the first mainframe to the USS files of the other") {
        defaultTarget = TreePath(arrayOf("/u/root", "/test", "/test1"))
        defaultSources = arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))
        every { defaultSourceTargetBounds.v1 } returns defaultSources
        every { defaultSourceTargetBounds.v2 } returns defaultTarget
        defaultSourcesList = defaultSources.toList()
        mockkObject(defaultTarget)
        val sources = defaultSourcesList[0]
        mockkObject(sources)
        every { mockedDnDEvent.attachedObject } answers {
          FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        }
        every { defaultSourceTargetBounds.v4 } returns mockedJTree
        isCopyPerformed = false
        isPastePerformed = false

        val mockedVirtualFileTarget = mockk<MFVirtualFile>()
        val mockedVirtualFileSource = mockk<MFVirtualFile>()

        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedStructureTreeModelNodeSource = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<UssFileNode>()
        val mockedNodeSource = mockk<UssFileNode>()
        every { defaultTarget.lastPathComponent } returns mockedStructureTreeModelNodeTarget
        every { sources.lastPathComponent } returns mockedStructureTreeModelNodeSource
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedStructureTreeModelNodeSource.userObject } returns mockedNodeSource
        every { mockedNodeTarget.virtualFile } returns mockedVirtualFileTarget
        every { mockedNodeSource.virtualFile } returns mockedVirtualFileSource

        every { mockedFileExplorer.componentManager } returns mockk()
        val targetAttributes = mockk<RemoteUssAttributes>()
        val sourceAttributes = mockk<RemoteUssAttributes>()

        val dataOpsManagerService = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
          override fun tryToGetAttributes(file: VirtualFile): FileAttributes? {
            return when (file) {
              mockedVirtualFileTarget -> targetAttributes
              mockedVirtualFileSource -> sourceAttributes
              else -> super.tryToGetAttributes(file)
            }
          }
        }

        val conn1 = mockk<ConnectionConfig>()
        val conn2 = mockk<ConnectionConfig>()
        every { conn1.url } returns "https://test1:10443"
        every { conn2.url } returns "https://test2:10443"
        every { conn1.isAllowSelfSigned } returns true
        every { conn2.isAllowSelfSigned } returns true
        val requester1 = mockk<UssRequester>()
        val requester2 = mockk<UssRequester>()
        every { requester1.connectionConfig } returns conn1
        every { requester2.connectionConfig } returns conn2
        every { targetAttributes.requesters } returns mutableListOf(requester1)
        every { sourceAttributes.requesters } returns mutableListOf(requester2)

        every { mockedCopyPasterProvider.copyProvider.isCopyEnabled(any() as DataContext) } returns true
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCopyPerformed shouldBe true
          isPastePerformed shouldBe true
        }
      }

      // update
      should("highlight places where paste is possible") {
        defaultTarget = TreePath(arrayOf("project", "project", "test1", "test2"))
        defaultSources = arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))
        every { defaultSourceTargetBounds.v1 } returns defaultSources
        every { defaultSourceTargetBounds.v2 } returns defaultTarget
        every { mockedDnDEvent.isDataFlavorSupported(any() as DataFlavor) } returns true
        every {
          mockedCopyPasterProvider.isPastePossibleFromPath(
            any() as List<TreePath>,
            any() as List<TreePath>
          )
        } returns true
        every { mockedJTree.parent } returns mockk<JRootPane>()
        every { mockedJTree.isShowing } returns false
        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDropPossible shouldBe true
          isUpdatePerformed shouldBe true
          isDragHighlighted shouldBe true
        }
      }

      should("highlight is not possible if Tuple4 of sources and target is null") {
        every { fileExplorerViewDropTarget["getSourcesTargetAndBounds"](mockedDnDEvent) } returns null
        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDragHighlighted shouldBe false
          isUpdatePerformed shouldBe false
        }
      }

      should("highlight is not possible if sources from Tuple4 is null") {
        every { fileExplorerViewDropTarget["getSourcesTargetAndBounds"](mockedDnDEvent) } returns defaultSourceTargetBounds
        every { defaultSourceTargetBounds.v1 } returns null
        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDragHighlighted shouldBe false
          isUpdatePerformed shouldBe false
        }
      }

      should("highlight is not possible if data flavour is not supported") {
        every { fileExplorerViewDropTarget["getSourcesTargetAndBounds"](mockedDnDEvent) } returns defaultSourceTargetBounds
        defaultSources = arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))
        every { defaultSourceTargetBounds.v1 } returns defaultSources
        every { mockedDnDEvent.isDataFlavorSupported(any() as DataFlavor) } returns false
        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDragHighlighted shouldBe false
          isUpdatePerformed shouldBe false
        }
      }

      should("highlight is not possible if target virtual file is null") {
        every { fileExplorerViewDropTarget["getSourcesTargetAndBounds"](mockedDnDEvent) } returns defaultSourceTargetBounds
        defaultTarget = TreePath(arrayOf("project", "project", "test1", "test2"))
        defaultSources = arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))
        every { defaultSourceTargetBounds.v1 } returns defaultSources
        every { defaultSourceTargetBounds.v2 } returns defaultTarget
        mockkObject(defaultTarget)
        every { mockedDnDEvent.isDataFlavorSupported(any() as DataFlavor) } returns true
        every { defaultSourceTargetBounds.v4 } returns mockedJTree
        val mockedProject = mockk<Project>()
        every { mockedCopyPasterProvider.project } returns mockedProject
        every { ProjectViewImpl.getInstance(mockedProject) } returns mockk()
        every { ProjectViewImpl.getInstance(mockedProject).currentProjectViewPane } returns mockk()
        every { ProjectViewImpl.getInstance(mockedProject).currentProjectViewPane.tree } returns mockedJTree
        val mockedStructureTreeModelNode = mockk<DefaultMutableTreeNode>()
        val mockedNode = mockk<ProjectViewNode<*>>()
        every { defaultTarget.lastPathComponent } returns mockedStructureTreeModelNode
        every { mockedStructureTreeModelNode.userObject } returns mockedNode
        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDragHighlighted shouldBe false
          isUpdatePerformed shouldBe false
        }
      }

      should("highlight is possible if target virtual file is not null and paste is possible") {
        defaultSourcesList = defaultSources.toList()
        val sources = defaultSourcesList[0]
        mockkObject(sources)
        val mockedStructureTreeModelNode = mockk<DefaultMutableTreeNode>()
        val mockedStructureTreeModelNodeSource = mockk<DefaultMutableTreeNode>()
        val mockedPsiNode = mockk<PsiDirectoryNode>()
        val mockedNodeSource = mockk<UssFileNode>()
        val mockedVirtualFileSource = mockk<MFVirtualFile>()
        every { defaultTarget.lastPathComponent } returns mockedStructureTreeModelNode
        every { sources.lastPathComponent } returns mockedStructureTreeModelNodeSource
        every { mockedStructureTreeModelNode.userObject } returns mockedPsiNode
        every { mockedStructureTreeModelNodeSource.userObject } returns mockedNodeSource
        every { mockedPsiNode.virtualFile } returns mockk<MFVirtualFile>()
        every { mockedNodeSource.virtualFile } returns mockedVirtualFileSource

        every { mockedFileExplorer.componentManager } returns mockk()
        val sourceAttributes = mockk<RemoteUssAttributes>()
        val dataOpsManagerService = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
          override fun tryToGetAttributes(file: VirtualFile): FileAttributes? {
            return if (file == mockedVirtualFileSource) {
              sourceAttributes
            } else {
              super.tryToGetAttributes(file)
            }
          }
        }

        every {
          mockedCopyPasterProvider.isPastePossible(
            any() as List<VirtualFile>,
            any() as List<NodeData<*>>
          )
        } returns true
        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDropPossible shouldBe true
          isDragHighlighted shouldBe true
          isUpdatePerformed shouldBe true
        }
      }

      should("highlight is not possible if target virtual file is not null and paste is not possible") {
        every {
          mockedCopyPasterProvider.isPastePossible(
            any() as List<VirtualFile>,
            any() as List<NodeData<*>>
          )
        } returns false
        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDragHighlighted shouldBe false
          isUpdatePerformed shouldBe false
        }
      }

      should("highlight is possible if paste is possible and isCopiedFromRemote is false") {
        every { mockedDnDEvent.attachedObject } answers {
          object : DnDSource {
            override fun canStartDragging(action: DnDAction?, dragOrigin: Point): Boolean {
              TODO("Not yet implemented")
            }

            override fun startDragging(action: DnDAction?, dragOrigin: Point): DnDDragStartBean {
              TODO("Not yet implemented")
            }
          }
        }
        every { defaultSourceTargetBounds.v4 } returns mockedJTree
        defaultTarget = TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2"))
        val source = TreePath(arrayOf("project", "project", "test1", "test2"))
        mockkObject(source)
        defaultSources = arrayOf(source)
        every { defaultSourceTargetBounds.v1 } returns defaultSources
        every { defaultSourceTargetBounds.v2 } returns defaultTarget
        mockkObject(defaultTarget)

        val mockedStructureTreeModelNodeSource = mockk<DefaultMutableTreeNode>()
        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedNodeSource = mockk<PsiDirectoryNode>()
        val mockedNodeTarget = mockk<UssDirNode>()
        every { source.lastPathComponent } returns mockedStructureTreeModelNodeSource
        every { mockedStructureTreeModelNodeSource.userObject } returns mockedNodeSource
        every { mockedNodeSource.virtualFile } returns mockk<MFVirtualFile>()

        every { defaultTarget.lastPathComponent } returns mockedStructureTreeModelNodeTarget
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        val mockedVirtualFileTarget = mockk<MFVirtualFile>()
        every { mockedNodeTarget.virtualFile } returns mockedVirtualFileTarget
        val targetAttributes = mockk<RemoteUssAttributes>()
        val dataOpsManagerService = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
          override fun tryToGetAttributes(file: VirtualFile): FileAttributes? {
            return if (file == mockedVirtualFileTarget) {
              targetAttributes
            } else {
              super.tryToGetAttributes(file)
            }
          }
        }
        every {
          mockedCopyPasterProvider.isPastePossibleForFiles(
            any() as List<VirtualFile>,
            any() as List<VirtualFile>
          )
        } returns true

        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDropPossible shouldBe true
          isDragHighlighted shouldBe true
          isUpdatePerformed shouldBe true
        }
      }

      should("highlight is not possible if everything is not met conditions") {
        every { mockedDnDEvent.attachedObject } answers {
          FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        }
        every { defaultSourceTargetBounds.v4 } returns mockk()
        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDragHighlighted shouldBe false
          isUpdatePerformed shouldBe false
        }
      }

      should("perform paste with predefined dataContext") {
        isPastePerformed = false
        val defaultSourceTest = TreePath(arrayOf("u/root", "/test1", "/u/ARST", "test5"))
        val target = TreePath(arrayOf("/u/root", "test"))
        val mockedStructureTreeModelNodeSource = mockk<DefaultMutableTreeNode>()
        val mockedNodeSource = mockk<UssFileNode>()
        val mockedVirtualFileSource = mockk<MFVirtualFile>()
        val mockedSourceAttributes = mockk<RemoteUssAttributes>()
        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<UssFileNode>()
        val mockedVirtualFileTarget = mockk<MFVirtualFile>()
        val mockedTargetAttributes = mockk<RemoteUssAttributes>()
        mockkObject(defaultSourceTest)
        mockkObject(target)
        val sourceArray = arrayOf(defaultSourceTest)
        every { defaultSourceTargetBounds.v1 } returns sourceArray
        every { defaultSourceTargetBounds.v2 } returns target
        every { defaultSourceTargetBounds.v4 } returns mockedJTree

        every { defaultSourceTest.lastPathComponent } returns mockedStructureTreeModelNodeSource
        every { mockedStructureTreeModelNodeSource.userObject } returns mockedNodeSource
        every { mockedNodeSource.virtualFile } returns mockedVirtualFileSource

        every { target.lastPathComponent } returns mockedStructureTreeModelNodeTarget
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedNodeTarget.virtualFile } returns mockedVirtualFileTarget

        val dataOpsManagerService = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
          override fun tryToGetAttributes(file: VirtualFile): FileAttributes? {
            return when (file) {
              mockedVirtualFileTarget -> mockedTargetAttributes
              mockedVirtualFileSource -> mockedSourceAttributes
              else -> super.tryToGetAttributes(file)
            }
          }
        }

        every {
          fileExplorerViewDropTarget["isCrossSystemCopy"](
            any() as Collection<TreePath>,
            any() as TreePath
          )
        } returns false

        every { mockedCopyPasterProvider.pasteProvider } returns userDefinedExplorerPasteProvider

        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isPastePerformed shouldBe true
        }
      }

      should("perform paste with predefined dataContext when source is not ExplorerTreeNode instance") {
        isPastePerformed = false
        val defaultSourceTest = TreePath(arrayOf("u/root", "/test1", "/u/ARST", "test6"))
        mockkObject(defaultSourceTest)
        val mockedStructureTreeModelNodeSource = mockk<DefaultMutableTreeNode>()
        val mockedNodeSource = mockk<PsiFileNode>()
        val mockedNodeVirtualFile = mockk<VirtualFile>()
        every { defaultSourceTest.lastPathComponent } returns mockedStructureTreeModelNodeSource
        every { mockedStructureTreeModelNodeSource.userObject } returns mockedNodeSource
        every { mockedNodeSource.virtualFile } returns mockedNodeVirtualFile
        val sourceArray = arrayOf(defaultSourceTest)
        every { defaultSourceTargetBounds.v1 } returns sourceArray
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isPastePerformed shouldBe true
        }
      }

      should("perform paste with predefined dataContext when sources tree is not myTree") {
        isPastePerformed = false
        every { defaultSourceTargetBounds.v4 } returns mockk()
        val target = TreePath(arrayOf("project", "test_folder"))
        mockkObject(target)
        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<PsiDirectoryNode>()
        val mockedVirtualFileTarget = mockk<VirtualFile>()
        every { target.lastPathComponent } returns mockedStructureTreeModelNodeTarget
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedNodeTarget.virtualFile } returns mockedVirtualFileTarget
        every { defaultSourceTargetBounds.v2 } returns target
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isPastePerformed shouldBe true
        }
      }

      should("perform paste from local to remote with predefined dataContext") {
        isPastePerformed = false
        every { mockedDnDEvent.attachedObject } returns mockk()
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isPastePerformed shouldBe true
        }
      }

    }
  }

  context("explorer module: ui/ExplorerDropTarget additional tests") {
    context("defined getSourcesTargetAndBounds") {

      val mockedDnDEvent = mockk<DnDEvent>()
      var isDropPossibleForEvent = false
      every { mockedDnDEvent.setDropPossible(any() as Boolean, any() as String) } answers {
        isDropPossibleForEvent = false
      }
      val mockedJTree = mockk<Tree>()
      val mockedProjectTree = mockk<Tree>()
      val mockedFileExplorer = mockk<Explorer<ConnectionConfig, FilesWorkingSetImpl>>()
      val mockedCopyPasterProvider = mockk<FileExplorerView.ExplorerCopyPasteSupport>()
      val mockedProject = mockk<Project>()

      var isCutPerformed = false
      var isCopyPerformed = false
      var isPastePerformed = false

      beforeEach {
        isCutPerformed = false
        isCopyPerformed = false
        isPastePerformed = false
      }

      val fileExplorerViewDropTarget = spyk(
        FileExplorerViewDropTarget(mockedJTree, mockedFileExplorer, mockedCopyPasterProvider), recordPrivateCalls = true
      )

      should("perform drop with defined getSourcesTargetAndBounds when tree is myTree") {
        val mockedPoint = Point(500, 220)
        mockkObject(mockedPoint)

        every { mockedDnDEvent.point } returns mockedPoint
        every { mockedDnDEvent.currentOverComponent } returns mockedJTree
        every {
          mockedJTree.getClosestPathForLocation(any<Int>(), any<Int>())
        } returns TreePath(arrayOf("project", "project", "test1", "test2"))
        every { mockedJTree.getPathBounds(any() as TreePath) } returns Rectangle(50, 200, 100, 50)
        val mockedDnDWrapper = FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        val mockedDefaultWrapper = object : TransferableWrapper {
          override fun asFileList(): MutableList<File>? {
            TODO("Not yet implemented")
          }

          override fun getTreeNodes(): Array<TreeNode>? {
            TODO("Not yet implemented")
          }

          override fun getPsiElements(): Array<PsiElement>? {
            TODO("Not yet implemented")
          }
        }
        mockkObject(mockedDnDWrapper)
        mockkObject(mockedDefaultWrapper)
        every {
          mockedDnDWrapper.treePaths
        } returns arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))
        every {
          mockedDefaultWrapper.treePaths
        } returns arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))

        mockkStatic(FileExplorerViewDragSource::class)
        mockkStatic(FileExplorerViewDragSource.ExplorerTransferableWrapper::class)

        every { mockedCopyPasterProvider.pasteProvider } returns mockk()
        every { mockedCopyPasterProvider.copyProvider } returns mockk()
        every { mockedCopyPasterProvider.cutProvider } returns mockk()

        every { mockedCopyPasterProvider.project } returns mockk()
        every { mockedCopyPasterProvider.cutProvider.isCutEnabled(any() as DataContext) } returns true
        every { mockedCopyPasterProvider.copyProvider.isCopyEnabled(any() as DataContext) } returns true

        every { mockedCopyPasterProvider.cutProvider.performCut(any() as DataContext) } answers {
          isCutPerformed = true
        }
        every { mockedCopyPasterProvider.copyProvider.performCopy(any() as DataContext) } answers {
          isCopyPerformed = true
        }
        every { mockedCopyPasterProvider.pasteProvider.performPaste(any() as DataContext) } answers {
          isPastePerformed = true
        }

        every { mockedDnDEvent.attachedObject } returns mockedDefaultWrapper

        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCopyPerformed shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("perform drop with defined getSourcesTargetAndBounds when tree is mot myTree, but project tree is null") {
        every { mockedDnDEvent.currentOverComponent } returns mockedProjectTree
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isDropPossibleForEvent shouldBe false
          isCutPerformed shouldBe false
          isCopyPerformed shouldBe false
          isPastePerformed shouldBe false
        }
      }

      should("perform drop with defined getSourcesTargetAndBounds when tree is project tree") {
        every { mockedCopyPasterProvider.project } returns mockedProject
        every { ProjectViewImpl.getInstance(mockedProject) } returns mockk()
        every { ProjectViewImpl.getInstance(mockedProject).currentProjectViewPane } returns mockk()
        every { ProjectViewImpl.getInstance(mockedProject).currentProjectViewPane.tree } returns mockedProjectTree
        every { mockedDnDEvent.currentOverComponent } returns mockedProjectTree
        every {
          mockedProjectTree.getClosestPathForLocation(any<Int>(), any<Int>())
        } returns TreePath(arrayOf("project", "project", "test1", "test2"))
        every { mockedProjectTree.getPathBounds(any() as TreePath) } returns Rectangle(50, 200, 100, 50)
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCopyPerformed shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("perform drop with defined getSourcesTargetAndBounds when event tree is new tree, but current tree is the project tree") {
        every { mockedDnDEvent.currentOverComponent } returns mockk()
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe false
          isCopyPerformed shouldBe false
          isPastePerformed shouldBe false
        }
      }

      should("perform drop with defined getSourcesTargetAndBounds when bounds are not suitable") {
        every { mockedDnDEvent.currentOverComponent } returns mockedProjectTree
        every { mockedProjectTree.getPathBounds(any() as TreePath) } returns Rectangle(50, 400, 100, 50)
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe false
          isCopyPerformed shouldBe false
          isPastePerformed shouldBe false
        }
      }

      should("perform drop with defined getSourcesTargetAndBounds when project is null to get project tree") {
        every { mockedCopyPasterProvider.project } returns null
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe false
          isCopyPerformed shouldBe false
          isPastePerformed shouldBe false
        }
      }

      should("perform drop with defined getSourcesTargetAndBounds when project tree getClosestPathForLocation returns null") {
        every { mockedCopyPasterProvider.project } returns mockedProject
        every { mockedProjectTree.getClosestPathForLocation(any() as Int, any() as Int) } returns null
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe false
          isCopyPerformed shouldBe false
          isPastePerformed shouldBe false
        }
      }

      should("perform drop with defined getSourcesTargetAndBounds when transfer data (sources) is not an instance of TransferableWrapper") {
        every {
          mockedProjectTree.getClosestPathForLocation(any<Int>(), any<Int>())
        } returns TreePath(arrayOf("project", "project", "test1", "test2"))
        every { mockedProjectTree.getPathBounds(any() as TreePath) } returns Rectangle(50, 200, 100, 50)
        every { mockedDnDEvent.attachedObject } answers {
          object : DnDSource {
            override fun canStartDragging(action: DnDAction?, dragOrigin: Point): Boolean {
              TODO("Not yet implemented")
            }

            override fun startDragging(action: DnDAction?, dragOrigin: Point): DnDDragStartBean {
              TODO("Not yet implemented")
            }
          }
        }
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCopyPerformed shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("perform drop with defined getSourcesTargetAndBounds when current event point is null") {
        every { mockedDnDEvent.point } returns null
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe false
          isCopyPerformed shouldBe false
          isPastePerformed shouldBe false
        }
      }
    }
  }

})
