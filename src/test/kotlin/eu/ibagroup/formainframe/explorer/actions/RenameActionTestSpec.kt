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

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Operation
import eu.ibagroup.formainframe.dataops.attributes.*
import eu.ibagroup.formainframe.dataops.content.synchronizer.checkFileForSync
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.telemetry.NotificationsService
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestNotificationsServiceImpl
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*

class RenameActionTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }
  context("explorer module: actions/RenameAction") {
    val renameAction = RenameAction()

    val fileExplorerViewMock = mockk<FileExplorerView>()
    val selectedNodeDataMock = mockk<NodeData<ConnectionConfig>>()

    var isEnabledAndVisible = false

    val anActionEventMock = mockk<AnActionEvent>()
    every { anActionEventMock.presentation.isEnabledAndVisible = any<Boolean>() } answers {
      isEnabledAndVisible = firstArg<Boolean>()
      every { anActionEventMock.presentation.isEnabledAndVisible } returns isEnabledAndVisible
    }
    every { anActionEventMock.project } returns mockk()

    val virtualFileMock = mockk<MFVirtualFile>()
    every { virtualFileMock.name } returns "fileName"

    val explorerMock = mockk<Explorer<ConnectionConfig, *>>()
    every { explorerMock.componentManager } returns ApplicationManager.getApplication()

    val notificationsService = NotificationsService.getService() as TestNotificationsServiceImpl

    notificationsService.testInstance = object : TestNotificationsServiceImpl() {
      override fun notifyError(
        t: Throwable,
        project: Project?,
        custTitle: String?,
        custDetailsShort: String?,
        custDetailsLong: String?
      ) {
        return
      }
    }

    val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl

    var updated = false
    var renamed = false

    beforeEach {
      isEnabledAndVisible = false

      every { fileExplorerViewMock.mySelectedNodesData } returns listOf(selectedNodeDataMock)

      mockkStatic("eu.ibagroup.formainframe.explorer.ui.ExplorerTreeViewKt")
      every { anActionEventMock.getExplorerView<FileExplorerView>() } returns fileExplorerViewMock

      every { selectedNodeDataMock.node } returns mockk()
      every { selectedNodeDataMock.file } returns virtualFileMock
      every { selectedNodeDataMock.attributes } returns mockk()

      renamed = false
      dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
        override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
          return mockk()
        }

        override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
          renamed = true
          @Suppress("UNCHECKED_CAST")
          return Unit as R
        }
      }

      mockkObject(RenameDialog)
      every { RenameDialog["initialize"](any<() -> Unit>()) } returns Unit

      mockkConstructor(RenameDialog::class)
      every { anyConstructed<RenameDialog>().showAndGet() } returns true

      updated = false
      every {
        ConfigService.getService().crudable.update(any<FilesWorkingSetConfig>())
      } answers {
        updated = true
        mockk()
      }

      mockkStatic(ExplorerTreeNode<ConnectionConfig, *>::cleanCacheIfPossible)
      mockkStatic(::checkFileForSync)
    }
    afterEach {
      unmockkAll()
    }

    context("actionPerformed") {
      context("rename dataset") {
        val libraryNodeMock = mockk<LibraryNode>()
        every { libraryNodeMock.explorer } returns explorerMock

        val attributes = mockk<RemoteDatasetAttributes>()
        every { attributes.datasetInfo.name } returns "dataset"

        beforeEach {
          every { selectedNodeDataMock.node } returns libraryNodeMock
          every { selectedNodeDataMock.attributes } returns attributes

          every { libraryNodeMock.virtualFile } returns virtualFileMock
          every { libraryNodeMock.parent?.cleanCacheIfPossible(any()) } returns Unit
        }

        should("perform rename on dataset") {
          every { anActionEventMock.getExplorerView<FileExplorerView>() } returns fileExplorerViewMock

          runBlocking {
            withContext(Dispatchers.EDT) {
              renameAction.actionPerformed(anActionEventMock)
            }
          }

          assertSoftly { renamed shouldBe true }
        }
        should("not perform rename on dataset if dialog is closed") {
          every { anyConstructed<RenameDialog>().showAndGet() } returns false

          runBlocking {
            withContext(Dispatchers.EDT) {
              renameAction.actionPerformed(anActionEventMock)
            }
          }

          assertSoftly { renamed shouldBe false }
        }
        should("not perform rename on dataset if virtual file is null") {
          every { (libraryNodeMock as ExplorerTreeNode<ConnectionConfig, *>).virtualFile } returns null

          runBlocking {
            withContext(Dispatchers.EDT) {
              renameAction.actionPerformed(anActionEventMock)
            }
          }

          assertSoftly { renamed shouldBe false }
        }
        should("not perform rename on dataset if virtual file is syncing now") {
          every { checkFileForSync(any(), any(), any()) } returns true

          runBlocking {
            withContext(Dispatchers.EDT) {
              renameAction.actionPerformed(anActionEventMock)
            }
          }

          assertSoftly { renamed shouldBe false }
        }
      }
      context("rename dataset member") {
        val fileLikeDSNodeMock = mockk<FileLikeDatasetNode>()
        every { fileLikeDSNodeMock.explorer } returns explorerMock
        every { fileLikeDSNodeMock.virtualFile } returns virtualFileMock

        val attributes = mockk<RemoteMemberAttributes>()
        every { attributes.info.name } returns "member"

        beforeEach {
          every { selectedNodeDataMock.node } returns fileLikeDSNodeMock
          every { selectedNodeDataMock.attributes } returns attributes

          every { fileLikeDSNodeMock.parent?.cleanCacheIfPossible(any()) } returns Unit
        }

        should("perform rename on dataset member") {
          runBlocking {
            withContext(Dispatchers.EDT) {
              renameAction.actionPerformed(anActionEventMock)
            }
          }

          assertSoftly { renamed shouldBe true }
        }
        should("not perform rename on dataset member if attributes is null") {
          every { selectedNodeDataMock.attributes } returns null

          runBlocking {
            withContext(Dispatchers.EDT) {
              renameAction.actionPerformed(anActionEventMock)
            }
          }

          assertSoftly { renamed shouldBe false }
        }
      }
      context("rename USS file") {
        val ussFileNodeMock = mockk<UssFileNode>()
        every { ussFileNodeMock.explorer } returns explorerMock

        val attributes = mockk<RemoteUssAttributes>()
        every { attributes.name } returns "ussFile"
        every { attributes.isDirectory } returns false

        beforeEach {
          every { selectedNodeDataMock.node } returns ussFileNodeMock
          every { selectedNodeDataMock.attributes } returns attributes

          every { ussFileNodeMock.parent?.cleanCacheIfPossible(any()) } returns Unit
        }

        should("perform rename on USS file") {
          runBlocking {
            withContext(Dispatchers.EDT) {
              renameAction.actionPerformed(anActionEventMock)
            }
          }

          assertSoftly { renamed shouldBe true }
        }
        should("perform rename on USS file but don't clean cache if parent node is null") {
          every { ussFileNodeMock.parent } returns null

          runBlocking {
            withContext(Dispatchers.EDT) {
              renameAction.actionPerformed(anActionEventMock)
            }
          }

          assertSoftly { renamed shouldBe true }
        }
        should("not perform rename on USS file if dialog is closed") {
          every { anyConstructed<RenameDialog>().showAndGet() } returns false

          runBlocking {
            withContext(Dispatchers.EDT) {
              renameAction.actionPerformed(anActionEventMock)
            }
          }

          assertSoftly { renamed shouldBe false }
        }
        should("not perform rename on USS file if virtual file is null") {
          every { selectedNodeDataMock.file } returns null

          runBlocking {
            withContext(Dispatchers.EDT) {
              renameAction.actionPerformed(anActionEventMock)
            }
          }

          assertSoftly { renamed shouldBe false }
        }
      }
      should("not perform rename action if explorer view is null") {
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns null

        runBlocking {
          withContext(Dispatchers.EDT) {
            renameAction.actionPerformed(anActionEventMock)
          }
        }

        assertSoftly { updated shouldBe false }
      }
      should("not perform rename action if selected node is not a DS mask, dataset, dataset member, USS mask, USS directory or USS file") {
        runBlocking {
          withContext(Dispatchers.EDT) {
            renameAction.actionPerformed(anActionEventMock)
          }
        }

        assertSoftly { updated shouldBe false }
      }
    }

    context("update") {
      should("rename action is enabled and visible") {
        renameAction.update(anActionEventMock)
      }
      should("rename action is enabled and visible if selected node file is null") {
        every { selectedNodeDataMock.node } returns mockk<FileLikeDatasetNode>()
        every { selectedNodeDataMock.file } returns null

        renameAction.update(anActionEventMock)

        assertSoftly { isEnabledAndVisible shouldBe true }
      }
      should("rename action is enabled and visible if file attributes are not dataset attributes") {
        every { selectedNodeDataMock.node } returns mockk<FileLikeDatasetNode>()
        every { selectedNodeDataMock.file } returns mockk()
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
          override fun tryToGetAttributes(file: VirtualFile): FileAttributes? {
            return null
          }
        }

        renameAction.update(anActionEventMock)

        assertSoftly { isEnabledAndVisible shouldBe true }
      }
      should("rename action is enabled and visible if selected node is USS directory") {
        val ussDirNodeMock = mockk<UssDirNode>()
        every { ussDirNodeMock.isUssMask } returns false

        every { selectedNodeDataMock.node } returns ussDirNodeMock

        renameAction.update(anActionEventMock)

        assertSoftly {
          isEnabledAndVisible shouldBe true
        }
      }
      should("rename action is not enabled and not visible if explorer view is null") {
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns null

        renameAction.update(anActionEventMock)

        assertSoftly { isEnabledAndVisible shouldBe false }
      }
      should("rename action is not enabled and not visible if selected nodes size grater than one") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(selectedNodeDataMock, selectedNodeDataMock)

        renameAction.update(anActionEventMock)

        assertSoftly { isEnabledAndVisible shouldBe false }
      }
      should("rename action is not enabled and not visible if selected node is 'files working set' node") {
        every { selectedNodeDataMock.node } returns mockk<FilesWorkingSetNode>()

        renameAction.update(anActionEventMock)

        assertSoftly { isEnabledAndVisible shouldBe false }
      }
      should("rename action is not enabled and not visible if selected node is 'loading' node") {
        every { selectedNodeDataMock.node } returns mockk<LoadingNode<ConnectionConfig>>()

        renameAction.update(anActionEventMock)

        assertSoftly { isEnabledAndVisible shouldBe false }
      }
      should("rename action is not enabled and not visible if selected node is 'load more' mode") {
        every { selectedNodeDataMock.node } returns mockk<LoadMoreNode<ConnectionConfig>>()

        renameAction.update(anActionEventMock)

        assertSoftly { isEnabledAndVisible shouldBe false }
      }
      should("rename action is not enabled and not visible if dataset is migrated") {
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
          override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
            val attributesMock = mockk<RemoteDatasetAttributes>()
            every { attributesMock.isMigrated } returns true
            return attributesMock
          }
        }

        renameAction.update(anActionEventMock)

        assertSoftly { isEnabledAndVisible shouldBe false }
      }
    }

    context("isDumbAware") {
      should("action is dumb aware") {
        val actual = renameAction.isDumbAware

        assertSoftly { actual shouldBe true }
      }
    }
  }
})
