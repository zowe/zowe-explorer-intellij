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
 *   Katsiaryna Tsytsenia
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.TreeAnchorizer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerBase.HARD_REF_TO_DOCUMENT_KEY
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.openapi.vfs.VirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributesService
import org.zowe.explorer.dataops.content.service.SyncProcessService
import org.zowe.explorer.dataops.content.synchronizer.SyncProvider
import org.zowe.explorer.dataops.content.synchronizer.checkFileForSync
import org.zowe.explorer.explorer.*
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.isBeingEditingNow
import org.zowe.explorer.utils.runInEdtAndWait
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.explorer.vfs.MFVirtualFileSystem
import org.zowe.kotlinsdk.FileMode
import org.zowe.kotlinsdk.UssFile
import java.time.LocalDateTime
import javax.swing.Icon
import javax.swing.tree.TreePath
import kotlin.reflect.KFunction

class UssFileNodeTestSpec : AppInitShouldSpec("explorer/ui/UssFileNode", {
  context("all functions") {
    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "test"
    every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()

    val syncProcessService = SyncProcessService.getService()
    every { syncProcessService.isFileSyncingNow(any<VirtualFile>()) } returns false
    every { syncProcessService.areDependentFilesSyncingNow(any<VirtualFile>()) } returns false

    context("ExplorerTreeNode.navigate") {
      val requestFocus = true
      lateinit var fileMock: MFVirtualFile
      lateinit var projectMock: Project
      lateinit var treeStructureMock: ExplorerTreeStructureBase
      lateinit var explorerTreeNodeMock: ExplorerTreeNode<ConnectionConfig, *>
      lateinit var explorerMock: Explorer<ConnectionConfig, WorkingSet<ConnectionConfig, *>>
      lateinit var explorerUnitMock: ExplorerUnit<ConnectionConfig>
      lateinit var dataOpsManagerService: DataOpsManager
      lateinit var uiComponentManagerService: UIComponentManager
      lateinit var ussFileNode: UssFileNode
      lateinit var rootNode: ExplorerTreeNode<ConnectionConfig, Any>
      var firstNode: ExplorerTreeNode<ConnectionConfig, Any>

      beforeEach {
        mockkObject(MFVirtualFileSystem)
        every { MFVirtualFileSystem.instance } returns mockk()

        projectMock = mockk<Project>()

        var isReadableFlag = true
        var isWritableFlag = true
        fileMock = mockk {
          every { isDirectory } returns false
          every { isReadable } returns isReadableFlag
          every {
            isReadable = any<Boolean>()
          } answers {
            isReadableFlag = firstArg<Boolean>()
          }
          every { isWritable } returns isWritableFlag
          every {
            isWritable = any<Boolean>()
          } answers {
            isWritableFlag = firstArg<Boolean>()
          }
          every { isValid } returns true
          every { fileType } returns FileTypes.UNKNOWN
          every { detectedLineSeparator } returns "\n"
          every { name } returns "navigate test"
          every { getUserData(HARD_REF_TO_DOCUMENT_KEY) } returns null
        }

        mockkStatic(VirtualFile::isBeingEditingNow)
        every { fileMock.isBeingEditingNow() } returns false

        treeStructureMock = mockk {
          every { registerNode(any()) } returns mockk()
        }

        mockkStatic(TreeAnchorizer::class)
        every { TreeAnchorizer.getService().createAnchor(any()) } returns mockk()

        uiComponentManagerService = UIComponentManager.getService()
        every { uiComponentManagerService.getExplorerContentProvider(any<Class<FileExplorer>>()) } returns mockk()

        rootNode =
          object : ExplorerTreeNode<ConnectionConfig, Any>("rootNode", projectMock, null, mockk(), treeStructureMock) {
            override fun update(presentation: PresentationData) {
              throw NotImplementedError()
            }

            override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
              throw NotImplementedError()
            }
          }

        explorerMock = mockk {
          every { componentManager } returns ApplicationManager.getApplication()
        }

        explorerUnitMock = mockk {
          every { explorer } returns explorerMock
          every { connectionConfig } returns ConnectionConfig()
        }

        firstNode = object : ExplorerUnitTreeNodeBase<ConnectionConfig, Any, ExplorerUnit<ConnectionConfig>>(
          "firstNode",
          projectMock,
          rootNode,
          explorerUnitMock,
          treeStructureMock
        ) {
          override fun update(presentation: PresentationData) {
            throw NotImplementedError()
          }

          override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
            throw NotImplementedError()
          }
        }

        explorerTreeNodeMock = spyk(
          UssFileNode(
            fileMock, projectMock,
            firstNode,
            explorerUnitMock, treeStructureMock
          ),
          recordPrivateCalls = true
        ) {
          every { parent } answers { firstNode }
        }

        dataOpsManagerService = DataOpsManager.getService()

        ussFileNode = spyk(
          UssFileNode(
            fileMock,
            projectMock,
            explorerTreeNodeMock,
            explorerUnitMock,
            treeStructureMock
          )
        ) {
          every { update() } returns false
          every { virtualFile } returns fileMock
        }

        mockkStatic(::checkFileForSync)
      }

      should("perform navigate on file") {
        var isSyncWithRemotePerformed = false
        every {
          dataOpsManagerService.getContentSynchronizer(any<VirtualFile>())
        } returns mockk {
          every { isFileUploadNeeded(any()) } returns false
          every { successfulContentStorage(any()) } returns byteArrayOf(Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE)
          every {
            synchronizeWithRemote(any(), any())
          } answers {
            isSyncWithRemotePerformed = true
            val syncProvider = firstArg() as SyncProvider
            syncProvider.onSyncSuccess()
          }
        }
        every {
          dataOpsManagerService.tryToGetAttributes(any<VirtualFile>())
        } returns RemoteUssAttributes("test", false, null, "test", mutableListOf())

        var isOnSyncSuccessTriggered = false
        mockkStatic(::runInEdtAndWait)
        every { runInEdtAndWait(any()) } answers {
          isOnSyncSuccessTriggered = true
        }

        ussFileNode.navigate(requestFocus)

        assertSoftly { isSyncWithRemotePerformed shouldBe true }
        assertSoftly { isOnSyncSuccessTriggered shouldBe true }
      }
      should("perform navigate on file with failure due to permission denied") {
        var isSyncWithRemotePerformed = false
        val ussFileMock = mockk<UssFile> {
          every { name } returns "USSFileName"
          every { isDirectory } returns false
          every { size } returns 100
          every { uid } returns 500
          every { user } returns "user"
          every { gid } returns 110
          every { groupId } returns "guid"
          every { modificationTime } returns "modTime"
          every { target } returns "target"
          every { fileMode } returns FileMode(7, 5, 5, "")
        }

        every {
          dataOpsManagerService.tryToGetAttributes(any<VirtualFile>())
        } returns RemoteUssAttributes("rootPath", ussFileMock, "URL", ConnectionConfig())
        every {
          dataOpsManagerService.getContentSynchronizer(any<VirtualFile>())
        } returns mockk {
          every { isFileUploadNeeded(any()) } returns false
          every { successfulContentStorage(any()) } returns byteArrayOf(Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE)
          every { synchronizeWithRemote(any(), any()) } answers {
            isSyncWithRemotePerformed = true
            val syncProvider = firstArg() as SyncProvider
            syncProvider.onThrowable(Throwable("test error with Permission denied"))
          }
        }

        var isDefaultOnThrowableHandlerTriggered = false
        val notificationsService = NotificationsService.getService()
        every {
          notificationsService.notifyError(any<Throwable>(), any<Project>(), any<String>(), any<String>(), any<String>())
        } answers {
          assertSoftly { firstArg<Throwable>().message shouldContain "Permission denied." }
          isDefaultOnThrowableHandlerTriggered = true
        }

        ussFileNode.navigate(requestFocus)

        assertSoftly { isSyncWithRemotePerformed shouldBe true }
        assertSoftly { isDefaultOnThrowableHandlerTriggered shouldBe true }
      }
      should("perform navigate on file with failure due to client is not authorized") {
        var isSyncWithRemotePerformed = false

        every {
          dataOpsManagerService.tryToGetAttributes(any<VirtualFile>())
        } returns RemoteUssAttributes("test", false, null, "test", mutableListOf())
        every {
          dataOpsManagerService.getContentSynchronizer(any<VirtualFile>())
        } returns mockk {
          every { isFileUploadNeeded(any()) } returns false
          every { successfulContentStorage(any()) } returns byteArrayOf(Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE)
          every { synchronizeWithRemote(any(), any()) } answers {
            isSyncWithRemotePerformed = true
            val syncProvider = firstArg() as SyncProvider
            syncProvider.onThrowable(Throwable("test error with Client is not authorized for file access"))
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
        every {
          dataOpsManagerService.tryToGetAttributes(any<VirtualFile>())
        } returns RemoteUssAttributes("test", false, null, "test", mutableListOf())
        every { dataOpsManagerService.getContentSynchronizer(any<VirtualFile>()) } returns null

        var isNavigateContinued = false
        mockkStatic(::runInEdtAndWait)
        every { runInEdtAndWait(any()) } answers {
          isNavigateContinued = true
        }

        ussFileNode.navigate(requestFocus)

        assertSoftly { isNavigateContinued shouldBe false }
      }
      should("exit 'navigate' when the file is directory") {
        every { fileMock.isDirectory } returns true

        var isNavigateContinued = false
        mockkStatic(::runInEdtAndWait)
        every { runInEdtAndWait(any()) } answers {
          isNavigateContinued = true
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
        every {
          dataOpsManagerService.tryToGetAttributes(any<VirtualFile>())
        } returns RemoteUssAttributes("test", false, null, "test", mutableListOf())
        every {
          dataOpsManagerService.getContentSynchronizer(any<VirtualFile>())
        } returns mockk {
          every { isFileUploadNeeded(any()) } returns false
          every { successfulContentStorage(any()) } returns byteArrayOf(Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE)
          every { synchronizeWithRemote(any(), any()) } answers {
            isNavigateContinued = true
            val syncProvider = firstArg() as SyncProvider
            syncProvider.onThrowable(Throwable("test error with Permission denied"))
          }
        }

        ussFileNode.navigate(requestFocus)

        assertSoftly {
          isDialogCalled shouldBe true
          isNavigateContinued shouldBe false
        }
      }
      should("perform navigate on a file that was already opened") {
        every { fileMock.isWritable } returns false
        firstNode = object : ExplorerTreeNode<ConnectionConfig, Any>(
          "nullConnectionNode",
          projectMock,
          rootNode,
          mockk<FileExplorer>(),
          treeStructureMock
        ) {
          override fun update(presentation: PresentationData) {
            throw NotImplementedError()
          }
          override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
            throw NotImplementedError()
          }
        }

        explorerTreeNodeMock = spyk(
          UssFileNode(
            fileMock, projectMock,
            firstNode,
            explorerUnitMock, treeStructureMock
          ),
          recordPrivateCalls = true
        ) {
          every { parent } answers { firstNode }
        }

        ussFileNode = spyk(
          UssFileNode(
            fileMock,
            projectMock,
            explorerTreeNodeMock,
            explorerUnitMock,
            treeStructureMock
          )
        ) {
          every { update() } returns false
          every { virtualFile } returns fileMock
        }

        var isSyncWithRemotePerformed = false
        every {
          dataOpsManagerService.tryToGetAttributes(any<VirtualFile>())
        } returns RemoteUssAttributes("test", false, null, "test", mutableListOf())
        every {
          dataOpsManagerService.getContentSynchronizer(any<VirtualFile>())
        } returns mockk {
          every { isFileUploadNeeded(any()) } answers { true }
          every { successfulContentStorage(any()) } answers {
            byteArrayOf(Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE)
          }
          every { synchronizeWithRemote(any(), any()) } answers {
            isSyncWithRemotePerformed = true
            val syncProvider = firstArg() as SyncProvider
            syncProvider.onSyncSuccess()
          }
        }

        var isOnSyncSuccessTriggered = false
        mockkStatic(::runInEdtAndWait)
        every { runInEdtAndWait(any()) } answers {
          isOnSyncSuccessTriggered = true
        }

        var isOpenFileCalled = false
        val fileEditorManager = mockk<FileEditorManager>(relaxUnitFun = true) {
          every {
            openFile(any<VirtualFile>(), any<Boolean>())
          } answers {
            isOpenFileCalled = true
            arrayOf()
          }
        }
        every { fileMock.isBeingEditingNow() } returns true
        mockkStatic(FileEditorManager::getInstance)
        every { FileEditorManager.getInstance(any()) } returns fileEditorManager

        ussFileNode.navigate(requestFocus)

        assertSoftly {
          isSyncWithRemotePerformed shouldBe false
          isOnSyncSuccessTriggered shouldBe false
          isOpenFileCalled shouldBe true
        }
      }
      should("exit 'navigate' when the file is currently being synchronized ") {
        every { checkFileForSync(any(), any(), any()) } returns true

        var isNavigateContinued = false
        mockkStatic(::runInEdtAndWait)
        every { runInEdtAndWait(any()) } answers {
          isNavigateContinued = true
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
      val mockedUssAttributesService = mockk<RemoteUssAttributesService>()

      val dataOpsManagerService = DataOpsManager.getService()
      every {
        dataOpsManagerService.getAttributesService(RemoteUssAttributes::class.java, MFVirtualFile::class.java)
      } returns mockedUssAttributesService

      beforeEach {
        textAdded = false
        updatePerformed = false
      }

      val virtualFileMock = mockk<MFVirtualFile>()
      val mockedAttributes = mockk<RemoteUssAttributes> {
        every {
          modificationTime
        } returns LocalDateTime.of(2002, 2, 17, 0, 0).toString()
        every { fileMode } returns FileMode(6, 6, 6)
        every { owner } returns "Test"
      }

      val mockedProject = mockk<Project>()
      val parentNode = mockk<UssDirNode>()
      val explorerMock = mockk<FileExplorer> {
        every { nullableProject } returns null
      }
      val explorerUnit = mockk<ExplorerUnit<ConnectionConfig>> {
        every { explorer } returns explorerMock
      }
      val treeStructure = mockk<ExplorerTreeStructureBase> {
        every { registerNode(any()) } just Runs
      }
      val explorerContentProviderMock = mockk<FileExplorerContentProvider>()

      val uiComponentManagerService = UIComponentManager.getService()
      every { uiComponentManagerService.getExplorer(FileExplorer::class.java) } returns explorerMock
      every {
        uiComponentManagerService.getExplorerContentProvider(FileExplorer::class.java)
      } returns explorerContentProviderMock

      val mockedUssNode = UssFileNode(virtualFileMock, mockedProject, parentNode, explorerUnit, treeStructure)
      val ussFileMockToSpy = spyk(mockedUssNode, recordPrivateCalls = true) {
        every { virtualFile } returns virtualFileMock
        every { value } returns virtualFileMock
      }
      every { ussFileMockToSpy["shouldUpdateData"]() } returns true
      every { ussFileMockToSpy["shouldPostprocess"]() } returns false
      every { ussFileMockToSpy["shouldApply"]() } returns true
      every { ussFileMockToSpy["apply"](any<PresentationData>(), any<PresentationData>()) } answers {
        textAdded = true
        updatePerformed = true
        true
      }

      context("ExplorerTreeNode.updateNodeTitleUsingCutBuffer") {
        every { virtualFileMock.presentableName } returns "test"
        every { virtualFileMock.isValid } returns false
        every { mockedUssAttributesService.getAttributes(virtualFileMock) } returns mockedAttributes

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
          every { explorerUnitToTest.explorer } returns mockk<FileExplorer> {
            every { nullableProject } returns null
          }
          every { uiComponentManagerService.getExplorerContentProvider(FileExplorer::class.java) } returns null

          val mockedUssNodeToTest =
            UssFileNode(virtualFileMock, mockedProject, parentNode, explorerUnitToTest, treeStructure)
          val ussFileMockToSpyTest = spyk(mockedUssNodeToTest, recordPrivateCalls = true) {
            every { virtualFile } returns virtualFileMock
            every { value } returns virtualFileMock
          }
          every { ussFileMockToSpyTest["shouldUpdateData"]() } returns true
          every { ussFileMockToSpyTest["shouldPostprocess"]() } returns false
          every { ussFileMockToSpyTest["shouldApply"]() } returns true
          every { ussFileMockToSpyTest["apply"](any() as PresentationData, any() as PresentationData) } answers {
            textAdded = true
            updatePerformed = true
            true
          }

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
