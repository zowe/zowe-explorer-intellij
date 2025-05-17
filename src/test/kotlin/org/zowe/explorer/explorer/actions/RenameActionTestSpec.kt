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

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.*
import org.zowe.explorer.dataops.content.synchronizer.checkFileForSync
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.utils.*
import org.zowe.explorer.v3.operations.OperationsService
import org.zowe.explorer.v3.operations.RenameOperationData
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.dataops.content.service.SyncProcessService
import org.zowe.explorer.dataops.operations.RenameOperation
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.v3.ConnectionConfigOldStruct
import org.zowe.kotlinsdk.annotations.ZVersion
import java.util.*

class RenameActionTestSpec : AppInitShouldSpec("explorer/actions/RenameAction", {
  context("all functions") {
    var didChangeIsEnabledAndVisible = false
    var updated = false
    var renamed = false

    val renameAction = RenameAction()

    val fileExplorerViewMock = mockk<FileExplorerView>()
    val selectedNodeDataMock = mockk<NodeData<ConnectionConfig>>()
    val presenationMock = mockk<Presentation> {
      every {
        isEnabledAndVisible = any<Boolean>()
      } answers {
        didChangeIsEnabledAndVisible = firstArg<Boolean>()
        every { isEnabledAndVisible } returns didChangeIsEnabledAndVisible
      }
    }
    val anActionEventMock = mockk<AnActionEvent> {
      every { presentation } returns presenationMock
      every { project } returns mockk()
    }
    val virtualFileMock = mockk<MFVirtualFile> {
      every { name } returns "fileName"
    }
    val explorerMock = mockk<Explorer<ConnectionConfig, *>> {
      every { componentManager } returns ApplicationManager.getApplication()
    }

    val notificationsService = NotificationsService.getService()
    every {
      notificationsService.notifyError(any<Throwable>(), any<Project>(), any<String>(), any<String>(), any<String>())
    } returns Unit

    val dataOpsManager = DataOpsManager.getService()

    val configServiceCrudable = mockk<Crudable>()
    val configService = ConfigService.getService()
    every { configService.crudable } returns configServiceCrudable

    val syncProcessService = SyncProcessService.getService()
    every { syncProcessService.isFileSyncingNow(any<MFVirtualFile>()) } returns false
    every { syncProcessService.areDependentFilesSyncingNow(any<MFVirtualFile>()) } returns false

    mockkConstructor(RenameDialog::class)

    mockkObject(OperationsService)

    mockkStatic(ExplorerTreeNode<ConnectionConfig, *>::cleanCacheIfPossible)
    mockkStatic(::checkFileForSync)

    beforeEach {
      didChangeIsEnabledAndVisible = false
      updated = false
      renamed = false

      every { fileExplorerViewMock.mySelectedNodesData } returns listOf(selectedNodeDataMock)
      every {
        fileExplorerViewMock.myFsTreeStructure
      } returns mockk<CommonExplorerTreeStructure<Explorer<ConnectionConfig, FilesWorkingSet>>>()

      every { anActionEventMock.getData(EXPLORER_VIEW) } returns fileExplorerViewMock

      every { selectedNodeDataMock.node } returns mockk()
      every { selectedNodeDataMock.file } returns virtualFileMock
      every { selectedNodeDataMock.attributes } returns mockk()

      every { checkFileForSync(any(), any(), any()) } returns false

      every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns mockk()
      every {
        dataOpsManager.performOperation(any<RenameOperation>(), any<ProgressIndicator>())
      } answers {
        renamed = true
      }

      val operationsServiceMock = mockk<OperationsService> {
        every {
          performOperation(any<RenameOperationData<ConnectionConfigOldStruct>>(), any())
        } answers {
          renamed = true
          Result.success(Unit)
        }
      }

      every { OperationsService.getService() } returns operationsServiceMock

      every { anyConstructed<RenameDialog>().showAndGet() } returns true

      every {
        configServiceCrudable.update(any<FilesWorkingSetConfig>())
      } answers {
        updated = true
        mockk()
      }
    }

    context("actionPerformed") {
      context("rename dataset") {
        val connectionConfigMockk = mockk<ConnectionConfig> {
          every { uuid } returns "testUuid"
          every { name } returns "testName"
          every { url } returns "testUrl"
          every { isAllowSelfSigned } returns true
          every { zVersion } returns ZVersion.ZOS_2_3
          every { owner } returns "testOwner"
        }

        val filesWorkingSetUnitMock = mockk<FilesWorkingSet> {
          every { connectionConfig } returns connectionConfigMockk
        }

        val libraryNodeMock = mockk<LibraryNode> {
          every { explorer } returns explorerMock
          every { unit } returns filesWorkingSetUnitMock
          every { value } returns mockk()
        }

        val attributes = mockk<RemoteDatasetAttributes> {
          every { datasetInfo.name } returns "dataset"
          every { requesters } returns mutableListOf(MaskedRequester(connectionConfigMockk, DSMask()))
        }

        beforeEach {
          every { selectedNodeDataMock.node } returns libraryNodeMock
          every { selectedNodeDataMock.attributes } returns attributes

          every { libraryNodeMock.virtualFile } returns virtualFileMock
          every { libraryNodeMock.parent?.cleanCacheIfPossible(any()) } returns Unit

          every {
            fileExplorerViewMock.myFsTreeStructure
          } returns mockk<CommonExplorerTreeStructure<Explorer<ConnectionConfig, FilesWorkingSet>>> {
            every { findByValue(any()) } returns listOf(libraryNodeMock as ExplorerTreeNode<*, Any>)
          }
        }

        should("perform rename on dataset") {
          runInEdtAndWait {
            renameAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { renamed shouldBe true }
        }

        should("not perform rename on dataset if dialog is closed") {
          every { anyConstructed<RenameDialog>().showAndGet() } returns false

          runInEdtAndWait {
            renameAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { renamed shouldBe false }
        }

        should("not perform rename on dataset if virtual file is null") {
          every { (libraryNodeMock as ExplorerTreeNode<ConnectionConfig, *>).virtualFile } returns null

          runInEdtAndWait {
            renameAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { renamed shouldBe false }
        }

        should("not perform rename on dataset if virtual file is syncing now") {
          every { checkFileForSync(any(), any(), any()) } returns true

          runInEdtAndWait {
            renameAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { renamed shouldBe false }
        }
      }
      context("rename dataset member") {
        val connectionConfigMockk = mockk<ConnectionConfig> {
          every { uuid } returns "testUuid"
          every { name } returns "testName"
          every { url } returns "testUrl"
          every { isAllowSelfSigned } returns true
          every { zVersion } returns ZVersion.ZOS_2_3
          every { owner } returns "testOwner"
        }

        val filesWorkingSetUnitMock = mockk<FilesWorkingSet> {
          every { connectionConfig } returns connectionConfigMockk
        }

        val fileLikeDSNodeMock = mockk<FileLikeDatasetNode> {
          every { name } returns "TEST"
          every { explorer } returns explorerMock
          every { unit } returns filesWorkingSetUnitMock
          every { virtualFile } returns virtualFileMock
          every { value } returns mockk()
        }

        val attributes = mockk<RemoteMemberAttributes> {
          every { info.name } returns "member"
        }

        beforeEach {
          every { selectedNodeDataMock.node } returns fileLikeDSNodeMock
          every { selectedNodeDataMock.attributes } returns attributes

          every { fileLikeDSNodeMock.parent?.cleanCacheIfPossible(any()) } returns Unit

          every {
            fileExplorerViewMock.myFsTreeStructure
          } returns mockk<CommonExplorerTreeStructure<Explorer<ConnectionConfig, FilesWorkingSet>>> {
            every { findByValue(any()) } returns listOf(fileLikeDSNodeMock as ExplorerTreeNode<*, Any>)
          }
        }

        should("perform rename on dataset member") {
          runInEdtAndWait {
            renameAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { renamed shouldBe true }
        }

        should("not perform rename on dataset member if attributes is null") {
          every { selectedNodeDataMock.attributes } returns null

          runInEdtAndWait {
            renameAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { renamed shouldBe false }
        }
      }
      context("rename USS file") {
        val connectionConfigMockk = mockk<ConnectionConfig> {
          every { uuid } returns "testUuid"
          every { name } returns "testName"
          every { url } returns "testUrl"
          every { isAllowSelfSigned } returns true
          every { zVersion } returns ZVersion.ZOS_2_3
          every { owner } returns "testOwner"
        }

        val filesWorkingSetUnitMock = mockk<FilesWorkingSet> {
          every { connectionConfig } returns connectionConfigMockk
        }

        val ussFileNodeMock = mockk<UssFileNode> {
          every { explorer } returns explorerMock
          every { unit } returns filesWorkingSetUnitMock
          every { value } returns mockk()
        }

        val attributes = mockk<RemoteUssAttributes> {
          every { name } returns "ussFile"
          every { isDirectory } returns false
          every { requesters } returns mutableListOf(UssRequester(connectionConfigMockk))
        }

        beforeEach {
          every { selectedNodeDataMock.node } returns ussFileNodeMock
          every { selectedNodeDataMock.attributes } returns attributes

          every { ussFileNodeMock.parent?.cleanCacheIfPossible(any()) } returns Unit

          every {
            fileExplorerViewMock.myFsTreeStructure
          } returns mockk<CommonExplorerTreeStructure<Explorer<ConnectionConfig, FilesWorkingSet>>> {
            every { findByValue(any()) } returns listOf(ussFileNodeMock as ExplorerTreeNode<*, Any>)
          }
        }

        should("perform rename on USS file") {
          runInEdtAndWait {
            renameAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { renamed shouldBe true }
        }

        should("perform rename on USS file but don't clean cache if parent node is null") {
          every { ussFileNodeMock.parent } returns null

          runInEdtAndWait {
            renameAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { renamed shouldBe true }
        }

        should("not perform rename on USS file if dialog is closed") {
          every { anyConstructed<RenameDialog>().showAndGet() } returns false

          runInEdtAndWait {
            renameAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { renamed shouldBe false }
        }

        should("not perform rename on USS file if virtual file is null") {
          every { selectedNodeDataMock.file } returns null

          runInEdtAndWait {
            renameAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { renamed shouldBe false }
        }
      }

      should("not perform rename action if explorer view is null") {
        every { anActionEventMock.getData(EXPLORER_VIEW) } returns null

        runInEdtAndWait {
          renameAction.actionPerformed(anActionEventMock)
        }

        assertSoftly { updated shouldBe false }
      }

      should("not perform rename action if selected node is not a DS mask, dataset, dataset member, USS mask, USS directory or USS file") {
        runInEdtAndWait {
          renameAction.actionPerformed(anActionEventMock)
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

        assertSoftly { didChangeIsEnabledAndVisible shouldBe true }
      }

      should("rename action is enabled and visible if file attributes are not dataset attributes") {
        every { selectedNodeDataMock.node } returns mockk<FileLikeDatasetNode>()
        every { selectedNodeDataMock.file } returns mockk()
        every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns null

        renameAction.update(anActionEventMock)

        assertSoftly { didChangeIsEnabledAndVisible shouldBe true }
      }

      should("rename action is enabled and visible if selected node is USS directory") {
        val ussDirNodeMock = mockk<UssDirNode>()
        every { ussDirNodeMock.isUssMask } returns false

        every { selectedNodeDataMock.node } returns ussDirNodeMock

        renameAction.update(anActionEventMock)

        assertSoftly { didChangeIsEnabledAndVisible shouldBe true }
      }

      should("rename action is not enabled and not visible if explorer view is null") {
        every { anActionEventMock.getData(EXPLORER_VIEW) } returns null

        renameAction.update(anActionEventMock)

        assertSoftly { didChangeIsEnabledAndVisible shouldBe false }
      }

      should("rename action is not enabled and not visible if selected nodes size grater than one") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(selectedNodeDataMock, selectedNodeDataMock)

        renameAction.update(anActionEventMock)

        assertSoftly { didChangeIsEnabledAndVisible shouldBe false }
      }

      should("rename action is not enabled and not visible if selected node is 'files working set' node") {
        every { selectedNodeDataMock.node } returns mockk<FilesWorkingSetNode>()

        renameAction.update(anActionEventMock)

        assertSoftly { didChangeIsEnabledAndVisible shouldBe false }
      }

      should("rename action is not enabled and not visible if selected node is 'loading' node") {
        every { selectedNodeDataMock.node } returns mockk<LoadingNode<ConnectionConfig>>()

        renameAction.update(anActionEventMock)

        assertSoftly { didChangeIsEnabledAndVisible shouldBe false }
      }

      should("rename action is not enabled and not visible if selected node is 'load more' mode") {
        every { selectedNodeDataMock.node } returns mockk<LoadMoreNode<ConnectionConfig>>()

        renameAction.update(anActionEventMock)

        assertSoftly { didChangeIsEnabledAndVisible shouldBe false }
      }

      should("rename action is not enabled and not visible if dataset is migrated") {
        every {
          dataOpsManager.tryToGetAttributes(any<VirtualFile>())
        } answers {
          val attributesMock = mockk<RemoteDatasetAttributes>()
          every { attributesMock.isMigrated } returns true
          attributesMock
        }

        renameAction.update(anActionEventMock)

        assertSoftly { didChangeIsEnabledAndVisible shouldBe false }
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
