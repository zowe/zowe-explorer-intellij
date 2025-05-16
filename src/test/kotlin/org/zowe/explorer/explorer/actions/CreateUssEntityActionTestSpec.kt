/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.exceptions.CredentialsNotFoundForConnectionException
import org.zowe.explorer.dataops.fetch.UssQuery
import org.zowe.explorer.dataops.operations.UssAllocationOperation
import org.zowe.explorer.dataops.operations.UssChangeModeOperation
import org.zowe.explorer.explorer.ui.CreateFileDialog
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.ExplorerTreeNode
import org.zowe.explorer.explorer.ui.FetchNode
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.LibraryNode
import org.zowe.explorer.explorer.ui.UssDirNode
import org.zowe.explorer.explorer.ui.UssFileNode
import org.zowe.explorer.explorer.ui.emptyDirState
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.runInEdtAndWait
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.FileMode

class CreateUssEntityActionTestSpec : AppInitShouldSpec("explorer/actions/CreateUssEntityAction", {
  context("all functions") {
    var didTriggerUssAllocationOperation = false
    var didTriggerUssChangeModeOperation = false
    var didCleanNodeCacheTriggered = false
    var didTriggerPerformOperationError = false
    var didTriggerOperationIsNotAvailableError = false
    var didChangeIsEnabledAndVisible = false
    var didChangeIsEnabled = false
    var isEnabledAndVisibleNewValue: Boolean? = null
    var isEnabledNewValue: Boolean? = null

    val createUssFileAction = CreateUssFileAction()
    val createUssDirectoryAction = CreateUssDirectoryAction()

    val performOperationTestErrMsg = "performOperation test error"

    val parentUssVFile = mockk<MFVirtualFile>()
    val queryMock = mockk<RemoteQuery<ConnectionConfig, UssQuery, Unit>>()
    val fileExplorerViewMock = mockk<FileExplorerView> {
      every { myFsTreeStructure } returns mockk {
        every {
          findByPredicate(any())
        } answers {
          val predicate = firstArg<Function1<ExplorerTreeNode<*, *>, Boolean>>()
          listOf(
            mockk<ExplorerTreeNode<*, *>>(),
            mockk<FetchNode> { every { query } returns mockk() },
            mockk<FetchNode> {
              every { query } returns queryMock
              every {
                cleanCache(any())
              } answers {
                didCleanNodeCacheTriggered = true
              }
            }
          )
            .filter(predicate)
        }
      }
    }
    val eventMock = mockk<AnActionEvent> {
      every { project } returns mockk()
      every { presentation } returns mockk {
        every {
          isEnabledAndVisible = any()
        } answers {
          didChangeIsEnabledAndVisible = true
          isEnabledAndVisibleNewValue = firstArg<Boolean>()
        }
        every {
          isEnabled = any()
        } answers {
          didChangeIsEnabled = true
          isEnabledNewValue = firstArg<Boolean>()
        }
      }
    }

    val credentialService = CredentialService.getService()

    val dataOpsManagerService = DataOpsManager.getService()

    val notificationsService = NotificationsService.getService()
    every {
      notificationsService
        .notifyError(any(), any(), any(), any(), any())
    } answers {
      val throwable = firstArg<Throwable>()
      if (throwable.message?.contains(performOperationTestErrMsg) == true) {
        didTriggerPerformOperationError = true
      } else if (throwable.message?.contains("username is not found") == true) {
        didTriggerOperationIsNotAvailableError = true
      } else {
        fail("Unexpected throwable: $throwable")
      }
    }

    beforeEach {
      didTriggerUssAllocationOperation = false
      didTriggerUssChangeModeOperation = false
      didCleanNodeCacheTriggered = false
      didTriggerPerformOperationError = false
      didTriggerOperationIsNotAvailableError = false
      didChangeIsEnabledAndVisible = false
      didChangeIsEnabled = false
      isEnabledAndVisibleNewValue = null
      isEnabledNewValue = null

      every { parentUssVFile.findChild(any()) } returns mockk()
      every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
        mockk {
          every { node } returns mockk<UssDirNode> {
            every { virtualFile } returns parentUssVFile
            every { unit } returns mockk {
              every { connectionConfig } returns mockk<ConnectionConfig> {
                every { uuid } returns "test"
              }
            }
            every { query } returns queryMock
          }
        }
      )
      every { eventMock.getData(EXPLORER_VIEW) } returns fileExplorerViewMock

      every { credentialService.getUsernameByKey(any()) } returns "TESTUSR"

      every {
        dataOpsManagerService.performOperation(any<Operation<Any>>(), any())
      } answers {
        when(val operation = firstArg<Operation<*>>()) {
          is UssAllocationOperation -> {
            didTriggerUssAllocationOperation = true
            mockk()
          }
          is UssChangeModeOperation -> {
            didTriggerUssChangeModeOperation = true
            mockk()
          }
          else -> fail("Unexpected operation: $operation")
        }
      }
      every {
        dataOpsManagerService
          .getAttributesService(RemoteUssAttributes::class.java, MFVirtualFile::class.java)
      } returns mockk {
        every { getAttributes(any()) } returns mockk {
          every { path } returns "test/path"
          every { fileMode } returns FileMode(4, 2, 1)
        }
      }
      every { dataOpsManagerService.tryToGetAttributes(any()) } returns mockk<RemoteUssAttributes> {
        every { fileMode } returns FileMode(0, 1, 2)
      }
    }

    context("actionPerformed") {
      var dialogShowAndGetCount = 0

      mockkConstructor(CreateFileDialog::class)

      beforeEach {
        dialogShowAndGetCount = 0

        every {
          anyConstructed<CreateFileDialog>().showAndGet()
        } answers {
          dialogShowAndGetCount++
          dialogShowAndGetCount == 1
        }
      }

      should("create a new USS file in the directory, updating the permissions to the selected ones and reloading the parent directory") {
        runInEdtAndWait {
          createUssFileAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerUssAllocationOperation shouldBe true
          didTriggerUssChangeModeOperation shouldBe true
          didCleanNodeCacheTriggered shouldBe true
          didTriggerPerformOperationError shouldBe false
          didTriggerOperationIsNotAvailableError shouldBe false
          dialogShowAndGetCount shouldBe 1
        }
      }

      should("create a new USS directory in the USS file's parent directory, not changing the permissions as the default ones are selected and not reloading the parent directory") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<UssFileNode> {
              every { parent } returns mockk<UssDirNode> {
                every { virtualFile } returns parentUssVFile
                every { unit } returns mockk {
                  every { connectionConfig } returns mockk<ConnectionConfig> {
                    every { uuid } returns "test"
                  }
                }
                every { query } returns queryMock
              }
            }
          }
        )

        every {
          dataOpsManagerService
            .getAttributesService(RemoteUssAttributes::class.java, MFVirtualFile::class.java)
        } returns mockk {
          every { getAttributes(any()) } returns mockk {
            every { path } returns "test/path"
            every { fileMode } returns FileMode(0, 0, 0)
          }
        }
        every { dataOpsManagerService.tryToGetAttributes(any()) } returns mockk<RemoteUssAttributes> {
          every { fileMode } returns emptyDirState.parameters.mode
        }

        runInEdtAndWait {
          createUssDirectoryAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerUssAllocationOperation shouldBe true
          didTriggerUssChangeModeOperation shouldBe false
          didCleanNodeCacheTriggered shouldBe false
          didTriggerPerformOperationError shouldBe false
          didTriggerOperationIsNotAvailableError shouldBe false
          dialogShowAndGetCount shouldBe 1
        }
      }

      should("create a new USS file in the directory, not changing the permissions as the virtual file for the USS parent directory is not found and reloading the parent directory") {
        every { parentUssVFile.findChild(any()) } returns null

        every {
          dataOpsManagerService
            .getAttributesService(RemoteUssAttributes::class.java, MFVirtualFile::class.java)
        } returns mockk {
          every { getAttributes(any()) } returns mockk {
            every { path } returns "test/path"
            every { fileMode } returns FileMode(5, 0, 0)
          }
        }

        runInEdtAndWait {
          createUssFileAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerUssAllocationOperation shouldBe true
          didTriggerUssChangeModeOperation shouldBe false
          didCleanNodeCacheTriggered shouldBe true
          didTriggerPerformOperationError shouldBe false
          didTriggerOperationIsNotAvailableError shouldBe false
          dialogShowAndGetCount shouldBe 1
        }
      }

      should("fail to create a USS file due to the remote connection error") {
        every {
          dataOpsManagerService.performOperation(any<Operation<Any>>(), any())
        } answers {
          when(val operation = firstArg<Operation<*>>()) {
            is UssAllocationOperation -> {
              didTriggerUssAllocationOperation = true
              throw Throwable(performOperationTestErrMsg)
            }
            else -> fail("Unexpected operation: $operation")
          }
        }

        runInEdtAndWait {
          createUssFileAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerUssAllocationOperation shouldBe true
          didTriggerUssChangeModeOperation shouldBe false
          didCleanNodeCacheTriggered shouldBe false
          didTriggerPerformOperationError shouldBe true
          didTriggerOperationIsNotAvailableError shouldBe false
          dialogShowAndGetCount shouldBe 2
        }
      }

      should("fail to create a USS file cause the username is not defined for the connection config of the USS parent directory") {
        every {
          credentialService.getUsernameByKey(any())
        } throws CredentialsNotFoundForConnectionException(mockk())

        runInEdtAndWait {
          createUssFileAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerUssAllocationOperation shouldBe false
          didTriggerUssChangeModeOperation shouldBe false
          didCleanNodeCacheTriggered shouldBe false
          didTriggerPerformOperationError shouldBe false
          didTriggerOperationIsNotAvailableError shouldBe true
          dialogShowAndGetCount shouldBe 0
        }
      }

      should("not allow to create a USS file cause there is no connection config associated with the selected entity") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<UssDirNode> {
              every { virtualFile } returns parentUssVFile
              every { unit } returns mockk {
                every { connectionConfig } returns null
              }
            }
          }
        )

        runInEdtAndWait {
          createUssFileAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerUssAllocationOperation shouldBe false
          didTriggerUssChangeModeOperation shouldBe false
          didCleanNodeCacheTriggered shouldBe false
          didTriggerPerformOperationError shouldBe false
          didTriggerOperationIsNotAvailableError shouldBe false
          dialogShowAndGetCount shouldBe 0
        }
      }

      should("not allow to create a USS file cause the selected node is not a USS entity") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk<LibraryNode>() }
        )

        runInEdtAndWait {
          createUssFileAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerUssAllocationOperation shouldBe false
          didTriggerUssChangeModeOperation shouldBe false
          didCleanNodeCacheTriggered shouldBe false
          didTriggerPerformOperationError shouldBe false
          didTriggerOperationIsNotAvailableError shouldBe false
          dialogShowAndGetCount shouldBe 0
        }
      }

      should("not allow to create a USS file outside of File Explorer view") {
        every { eventMock.getData(EXPLORER_VIEW) } returns mockk()

        runInEdtAndWait {
          createUssFileAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerUssAllocationOperation shouldBe false
          didTriggerUssChangeModeOperation shouldBe false
          didCleanNodeCacheTriggered shouldBe false
          didTriggerPerformOperationError shouldBe false
          didTriggerOperationIsNotAvailableError shouldBe false
          dialogShowAndGetCount shouldBe 0
        }
      }
    }

    context("update") {
      should("show the action as enabled") {
        runInEdtAndWait {
          createUssFileAction.update(eventMock)
        }

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          didChangeIsEnabled shouldBe false
          isEnabledAndVisibleNewValue shouldBe true
          isEnabledNewValue shouldBe null
        }
      }

      should("show the action as disabled cause the connection config is not associated with the selected node") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<UssFileNode> {
              every { unit } returns mockk {
                every { connectionConfig } returns null
              }
            }
          }
        )

        runInEdtAndWait {
          createUssFileAction.update(eventMock)
        }

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          didChangeIsEnabled shouldBe true
          isEnabledAndVisibleNewValue shouldBe true
          isEnabledNewValue shouldBe false
        }
      }

      should("not show the action as there is no selected nodes") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf()

        runInEdtAndWait {
          createUssFileAction.update(eventMock)
        }

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          didChangeIsEnabled shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
          isEnabledNewValue shouldBe false
        }
      }

      should("not show the action when there is no File Explorer view initialized") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        runInEdtAndWait {
          createUssFileAction.update(eventMock)
        }

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          didChangeIsEnabled shouldBe false
          isEnabledAndVisibleNewValue shouldBe false
          isEnabledNewValue shouldBe null
        }
      }
    }
  }
})
