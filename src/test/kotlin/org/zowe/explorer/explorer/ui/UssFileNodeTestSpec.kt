/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.TreeAnchorizer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileNavigator
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.content.synchronizer.ContentSynchronizer
import org.zowe.explorer.dataops.content.synchronizer.DocumentedSyncProvider
import org.zowe.explorer.dataops.content.synchronizer.SyncProvider
import org.zowe.explorer.dataops.content.synchronizer.checkFileForSync
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.ExplorerUnit
import org.zowe.explorer.explorer.FileExplorer
import org.zowe.explorer.explorer.FileExplorerContentProvider
import org.zowe.explorer.explorer.UIComponentManager
import org.zowe.explorer.explorer.WorkingSet
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.testutils.testServiceImpl.TestDataOpsManagerImpl
import org.zowe.explorer.utils.isBeingEditingNow
import org.zowe.explorer.utils.service
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.explorer.vfs.MFVirtualFileSystem
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import javax.swing.Icon
import javax.swing.tree.TreePath
import kotlin.reflect.KFunction

class UssFileNodeTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }
  context("explorer module: ui/UssFileNode") {

    context("ExplorerTreeNode.navigate") {
      val requestFocus = true
      lateinit var fileMock: MFVirtualFile
      lateinit var projectMock: Project
      lateinit var treeStructureMock: ExplorerTreeStructureBase
      lateinit var explorerTreeNodeMock: ExplorerTreeNode<ConnectionConfig, *>
      lateinit var explorer: Explorer<ConnectionConfig, WorkingSet<ConnectionConfig, *>>
      lateinit var explorerUnitMock: ExplorerUnit<ConnectionConfig>
      lateinit var dataOpsManagerService: TestDataOpsManagerImpl
      lateinit var ussFileNode: UssFileNode

      beforeEach {
        mockkObject(MFVirtualFileSystem)
        every { MFVirtualFileSystem.instance } returns mockk()

        fileMock = mockk()
        every { fileMock.isDirectory } returns false
        every { fileMock.isReadable } returns true
        every { fileMock.name } returns "navigate test"
        mockkStatic(VirtualFile::isBeingEditingNow)
        every { fileMock.isBeingEditingNow() } returns false

        projectMock = mockk()

        treeStructureMock = mockk()
        every { treeStructureMock.registerNode(any()) } returns mockk()

        mockkStatic(TreeAnchorizer::class)
        every { TreeAnchorizer.getService().createAnchor(any()) } returns mockk()

        mockkObject(UIComponentManager)
        every {
          UIComponentManager
            .INSTANCE
            .getExplorerContentProvider<Explorer<ConnectionConfig, WorkingSet<ConnectionConfig, UssFileNode>>>(any())
        } returns mockk()

        explorerTreeNodeMock = mockk()

        mockkObject(DataOpsManager)
        every { DataOpsManager.instance } returns mockk()

        explorer = mockk()
        every { explorer.componentManager } returns ApplicationManager.getApplication()

        explorerUnitMock = mockk()
        every { explorerUnitMock.explorer } returns explorer

        dataOpsManagerService = ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl

        ussFileNode = spyk(
          UssFileNode(
            fileMock,
            projectMock,
            explorerTreeNodeMock,
            explorerUnitMock,
            treeStructureMock
          )
        )
        every { ussFileNode.update() } returns false
        every { ussFileNode.virtualFile } returns fileMock

        mockkStatic(::checkFileForSync)
      }

      should("perform navigate on file") {
        var isSyncWithRemotePerformed = false
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(explorer.componentManager) {
          override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
            val contentSynchronizerMock = mockk<ContentSynchronizer>()
            every { contentSynchronizerMock.synchronizeWithRemote(any(), any()) } answers {
              isSyncWithRemotePerformed = true
              val syncProvider = firstArg() as SyncProvider
              syncProvider.onSyncSuccess()
            }
            return contentSynchronizerMock
          }

          override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
            return RemoteUssAttributes("test", false, null, "test", mutableListOf())
          }
        }

        var isOnSyncSuccessTriggered = false
        var isNavigatePerformed = false
        mockkStatic(FileNavigator::getInstance)
        every { FileNavigator.getInstance() } answers {
          isOnSyncSuccessTriggered = true
          object : FileNavigator {
            override fun navigate(descriptor: OpenFileDescriptor, requestFocus: Boolean) {
              isNavigatePerformed = true
              return
            }

            override fun navigateInEditor(descriptor: OpenFileDescriptor, requestFocus: Boolean): Boolean {
              return true
            }
          }
        }

        ussFileNode.navigate(requestFocus)
        assertSoftly { isSyncWithRemotePerformed shouldBe true }
        assertSoftly { isNavigatePerformed shouldBe true }
        assertSoftly { isOnSyncSuccessTriggered shouldBe true }
      }
      should("perform navigate on file with failure due to permission denied") {
        var isSyncWithRemotePerformed = false
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(explorer.componentManager) {
          override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
            val contentSynchronizerMock = mockk<ContentSynchronizer>()
            every { contentSynchronizerMock.synchronizeWithRemote(any(), any()) } answers {
              isSyncWithRemotePerformed = true
              val syncProvider = firstArg() as SyncProvider
              syncProvider.onThrowable(Throwable("test error with Permission denied"))
            }
            return contentSynchronizerMock
          }
        }

        var isDefaultOnThrowableHandlerTriggered = false
        mockkObject(DocumentedSyncProvider)
        every { DocumentedSyncProvider.defaultOnThrowableHandler(any(), any()) } answers {
          val throwed = secondArg() as Throwable
          assertSoftly { throwed.message shouldContain "Permission denied." }
          isDefaultOnThrowableHandlerTriggered = true
        }

        ussFileNode.navigate(requestFocus)
        assertSoftly { isSyncWithRemotePerformed shouldBe true }
        assertSoftly { isDefaultOnThrowableHandlerTriggered shouldBe true }
      }
      should("perform navigate on file with failure due to client is not authorized") {
        var isSyncWithRemotePerformed = false
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(explorer.componentManager) {
          override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
            val contentSynchronizerMock = mockk<ContentSynchronizer>()
            every { contentSynchronizerMock.synchronizeWithRemote(any(), any()) } answers {
              isSyncWithRemotePerformed = true
              val syncProvider = firstArg() as SyncProvider
              syncProvider.onThrowable(Throwable("test error with Client is not authorized for file access"))
            }
            return contentSynchronizerMock
          }
        }

        var isErrorMessageInDialogCalled = false
        val showDialogSpecificMock: (
          Project?, String, String, Array<String>, Int, Icon?
        ) -> Int = Messages::showDialog
        mockkStatic(showDialogSpecificMock as KFunction<*>)
        every {
          showDialogSpecificMock(any(), any<String>(), any<String>(), any<Array<String>>(), any<Int>(), any() as Icon?)
        } answers {
          isErrorMessageInDialogCalled = true
          1
        }

        ussFileNode.navigate(requestFocus)
        assertSoftly { isSyncWithRemotePerformed shouldBe true }
        assertSoftly { isErrorMessageInDialogCalled shouldBe true }
      }
      should("exit 'navigate' when 'getContentSynchronizer' returns null") {
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(explorer.componentManager) {
          override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer? {
            return null
          }
        }

        var isNavigateContinued = false
        mockkStatic(FileNavigator::getInstance)
        every { FileNavigator.getInstance() } answers {
          object : FileNavigator {
            override fun navigate(descriptor: OpenFileDescriptor, requestFocus: Boolean) {
              isNavigateContinued = true
              return
            }

            override fun navigateInEditor(descriptor: OpenFileDescriptor, requestFocus: Boolean): Boolean {
              return true
            }
          }
        }

        ussFileNode.navigate(requestFocus)
        assertSoftly { isNavigateContinued shouldBe false }
      }
      should("exit 'navigate' when the file is directory") {
        every { fileMock.isDirectory } returns true

        var isNavigateContinued = false
        mockkStatic(FileNavigator::getInstance)
        every { FileNavigator.getInstance() } answers {
          object : FileNavigator {
            override fun navigate(descriptor: OpenFileDescriptor, requestFocus: Boolean) {
              isNavigateContinued = true
              return
            }

            override fun navigateInEditor(descriptor: OpenFileDescriptor, requestFocus: Boolean): Boolean {
              return true
            }
          }
        }

        ussFileNode.navigate(requestFocus)
        assertSoftly { isNavigateContinued shouldBe false }
      }
      should("exit 'navigate' when the file is not readable") {
        var isDialogCalled = false
        mockkStatic(::showYesNoDialog)
        every {
          showYesNoDialog(any(), "Do you want to try open it anyway?", any(), any(), any(), any())
        } answers {
          isDialogCalled = true
          false
        }

        every { fileMock.isReadable } returns false

        var isNavigateContinued = false
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(explorer.componentManager) {
          override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
            val contentSynchronizerMock = mockk<ContentSynchronizer>()
            every { contentSynchronizerMock.synchronizeWithRemote(any(), any()) } answers {
              isNavigateContinued = true
              val syncProvider = firstArg() as SyncProvider
              syncProvider.onThrowable(Throwable("test error with Permission denied"))
            }
            return contentSynchronizerMock
          }
        }

        ussFileNode.navigate(requestFocus)
        assertSoftly { isDialogCalled shouldBe true }
        assertSoftly { isNavigateContinued shouldBe false }
      }
      should("perform navigate on a file that was already opened") {
        var isSyncWithRemotePerformed = false
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(explorer.componentManager) {
          override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
            val contentSynchronizerMock = mockk<ContentSynchronizer>()
            every { contentSynchronizerMock.synchronizeWithRemote(any(), any()) } answers {
              isSyncWithRemotePerformed = true
              val syncProvider = firstArg() as SyncProvider
              syncProvider.onSyncSuccess()
            }
            return contentSynchronizerMock
          }

          override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
            return RemoteUssAttributes("test", false, null, "test", mutableListOf())
          }
        }

        var isOnSyncSuccessTriggered = false
        var isNavigatePerformed = false
        mockkStatic(FileNavigator::getInstance)
        every { FileNavigator.getInstance() } answers {
          isOnSyncSuccessTriggered = true
          object : FileNavigator {
            override fun navigate(descriptor: OpenFileDescriptor, requestFocus: Boolean) {
              isNavigatePerformed = true
              return
            }

            override fun navigateInEditor(descriptor: OpenFileDescriptor, requestFocus: Boolean): Boolean {
              return true
            }
          }
        }

        every { fileMock.isBeingEditingNow() } returns true
        mockkStatic(FileEditorManager::getInstance)
        var isOpenFileCalled = false
        every {
          FileEditorManager.getInstance(any())
        } returns object : TestFileEditorManager() {
          override fun openFile(file: VirtualFile, focusEditor: Boolean): Array<FileEditor> {
            isOpenFileCalled = true
            return arrayOf()
          }
        }

        ussFileNode.navigate(requestFocus)
        assertSoftly { isSyncWithRemotePerformed shouldBe false }
        assertSoftly { isNavigatePerformed shouldBe false }
        assertSoftly { isOnSyncSuccessTriggered shouldBe false }
        assertSoftly { isOpenFileCalled shouldBe true }
      }
      should("exit 'navigate' when the file is currently being synchronized ") {
        every { checkFileForSync(any(), any(), any()) } returns true

        var isNavigateContinued = false
        mockkStatic(FileNavigator::getInstance)
        every { FileNavigator.getInstance() } answers {
          object : FileNavigator {
            override fun navigate(descriptor: OpenFileDescriptor, requestFocus: Boolean) {
              isNavigateContinued = true
              return
            }

            override fun navigateInEditor(descriptor: OpenFileDescriptor, requestFocus: Boolean): Boolean {
              return true
            }
          }
        }

        ussFileNode.navigate(requestFocus)

        assertSoftly {
          isNavigateContinued shouldBe false
        }
      }
    }

    context("UssFileNode update") {
      var textAdded = false
      var updatePerformed = false

      beforeEach {
        textAdded = false
        updatePerformed = false
      }

      val virtualFileMock = mockk<MFVirtualFile>()
      val mockedProject = mockk<Project>()
      val parentNode = mockk<UssDirNode>()
      val explorerUnit = mockk<ExplorerUnit<ConnectionConfig>>()
      val explorer = mockk<FileExplorer>()
      every { explorerUnit.explorer } returns explorer
      val treeStructure = mockk<ExplorerTreeStructureBase>()
      every { treeStructure.registerNode(any()) } just Runs

      mockkObject(UIComponentManager)
      every {
        UIComponentManager
          .INSTANCE
          .getExplorer<FileExplorer>(any())
      } returns explorer

      val explorerContentProviderMock = mockk<FileExplorerContentProvider>()

      every {
        UIComponentManager
          .INSTANCE
          .getExplorerContentProvider(explorer::class.java)
      } returns explorerContentProviderMock

      val mockedUssNode = UssFileNode(virtualFileMock, mockedProject, parentNode, explorerUnit, treeStructure)
      val ussFileMockToSpy = spyk(mockedUssNode, recordPrivateCalls = true)
      every { ussFileMockToSpy["shouldUpdateData"]() } returns true
      every { ussFileMockToSpy["shouldPostprocess"]() } returns false
      every { ussFileMockToSpy["shouldApply"]() } returns true
      every { ussFileMockToSpy["apply"](any() as PresentationData, any() as PresentationData) } answers {
        textAdded = true
        updatePerformed = true
        true
      }
      every { ussFileMockToSpy.virtualFile } returns virtualFileMock
      every { ussFileMockToSpy.value } returns virtualFileMock

      context("ExplorerTreeNode.updateNodeTitleUsingCutBuffer") {
        every { virtualFileMock.presentableName } returns "test"
        every { virtualFileMock.isValid } returns false

        should("perform an update of the node if virtual file in the cut buffer and navigate is true") {
          every { explorerContentProviderMock.isFileInCutBuffer(virtualFileMock) } returns true
          every { ussFileMockToSpy.navigating } returns true
          ussFileMockToSpy.update()

          assertSoftly {
            textAdded shouldBe true
            updatePerformed shouldBe true
          }
        }

        should("perform an update of the node if virtual file in the cut buffer and navigate is false") {
          every { ussFileMockToSpy.navigating } returns false
          ussFileMockToSpy.update()

          assertSoftly {
            textAdded shouldBe true
            updatePerformed shouldBe true
          }
        }

        should("perform an update of the node if virtual file is not in the cut buffer") {
          every { explorerContentProviderMock.isFileInCutBuffer(virtualFileMock) } returns false
          every { ussFileMockToSpy.navigating } returns true
          ussFileMockToSpy.update()

          assertSoftly {
            textAdded shouldBe true
            updatePerformed shouldBe true
          }
        }

        should("perform an update of the node if content provider is null") {
          val explorerUnitToTest = mockk<ExplorerUnit<ConnectionConfig>>()
          val explorerToTest = mockk<FileExplorer>()
          every { explorerUnitToTest.explorer } returns explorerToTest
          every {
            UIComponentManager
              .INSTANCE
              .getExplorerContentProvider(explorerToTest::class.java)
          } returns null
          val mockedUssNodeToTest =
            UssFileNode(virtualFileMock, mockedProject, parentNode, explorerUnitToTest, treeStructure)
          val ussFileMockToSpyTest = spyk(mockedUssNodeToTest, recordPrivateCalls = true)
          every { ussFileMockToSpyTest["shouldUpdateData"]() } returns true
          every { ussFileMockToSpyTest["shouldPostprocess"]() } returns false
          every { ussFileMockToSpyTest["shouldApply"]() } returns true
          every { ussFileMockToSpyTest["apply"](any() as PresentationData, any() as PresentationData) } answers {
            textAdded = true
            updatePerformed = true
            true
          }
          every { ussFileMockToSpyTest.virtualFile } returns virtualFileMock
          every { ussFileMockToSpyTest.value } returns virtualFileMock
          ussFileMockToSpyTest.update()

          assertSoftly {
            textAdded shouldBe true
            updatePerformed shouldBe true
          }
        }

        should("get virtual file of the USS file node") {
          val actual = ussFileMockToSpy.virtualFile

          assertSoftly {
            actual shouldBe virtualFileMock
          }
        }

        should("get children of the USS file node") {
          val expected = mutableListOf<AbstractTreeNode<*>>()
          val actual = ussFileMockToSpy.children

          assertSoftly {
            actual shouldBe expected
          }
        }
      }
    }

    context("create ExplorerTreeNode abstraction and cover remaining methods and calls") {
      val virtualFileMock = mockk<MFVirtualFile>()
      val mockedProject = mockk<Project>()
      val parentNode = null
      val explorer = mockk<FileExplorer>()
      val treeStructure = mockk<ExplorerTreeStructureBase>()
      every { treeStructure.registerNode(any()) } just Runs

      val objectMock = object : ExplorerTreeNode<ConnectionConfig, MFVirtualFile>(
        virtualFileMock,
        mockedProject,
        parentNode,
        explorer,
        treeStructure
      ) {
        override fun update(presentation: PresentationData) {
          return
        }

        override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
          return mutableListOf()
        }
      }

      val nodeToSpy = spyk(objectMock, recordPrivateCalls = true)

      context("miscellaneous tests") {
        should("get null virtual file of the node") {
          val actual = nodeToSpy.virtualFile
          assertSoftly {
            actual shouldBe null
          }
        }
        should("get view settings of the node") {
          val actual = nodeToSpy.settings
          assertSoftly {
            actual shouldBe treeStructure
          }
        }
        should("can navigate") {
          val actual = nodeToSpy.canNavigate()
          assertSoftly {
            actual shouldBe false
          }
        }
        should("can navigate to source") {
          val actual = nodeToSpy.canNavigateToSource()
          assertSoftly {
            actual shouldBe false
          }
        }
        should("get path of the node if parent is null") {
          val actual = nodeToSpy.path
          val expected = TreePath(listOf(nodeToSpy).toTypedArray())
          assertSoftly {
            actual shouldBe expected
          }
        }
        should("get path of the node if parent is not null") {
          val objectParentMock = object : ExplorerTreeNode<ConnectionConfig, MFVirtualFile>(
            virtualFileMock,
            mockedProject,
            parentNode,
            explorer,
            treeStructure
          ) {
            override fun update(presentation: PresentationData) {
              return
            }

            override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
              return mutableListOf()
            }
          }

          mockkObject(objectParentMock, recordPrivateCalls = true)

          val objectMockToTest = object : ExplorerTreeNode<ConnectionConfig, MFVirtualFile>(
            virtualFileMock,
            mockedProject,
            objectParentMock,
            explorer,
            treeStructure
          ) {
            override fun update(presentation: PresentationData) {
              return
            }

            override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
              return mutableListOf()
            }
          }

          val nodeToSpyTest = spyk(objectMockToTest, recordPrivateCalls = true)
          val actual = nodeToSpyTest.path
          val expected = TreePath(listOf(objectParentMock, nodeToSpyTest).toTypedArray())
          assertSoftly {
            actual shouldBe expected
          }
        }
      }
    }
  }
})
