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

import com.intellij.ide.util.treeView.TreeAnchorizer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DoNotAskOption
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.messages.MessagesService
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.intellij.util.Function
import com.intellij.util.PairFunction
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.ContentSynchronizer
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.dataops.content.synchronizer.SyncProvider
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.explorer.UIComponentManager
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import java.awt.Component
import javax.swing.Icon
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JTextField

class UssFileNodeTestSpec : ShouldSpec({
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
        mockkObject(MessagesService)
        every {
          MessagesService.getInstance()
        } returns
          object : TestMessagesService() {
            override fun showMessageDialog(
              project: Project?,
              parentComponent: Component?,
              message: String?,
              title: String?,
              options: Array<String>,
              defaultOptionIndex: Int,
              focusedOptionIndex: Int,
              icon: Icon?,
              doNotAskOption: DoNotAskOption?,
              alwaysUseIdeaUI: Boolean,
              helpId: String?
            ): Int {
              isErrorMessageInDialogCalled = true
              return 1
            }

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
    }
  }
})
