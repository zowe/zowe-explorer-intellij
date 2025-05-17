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
import io.mockk.mockkStatic
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.content.synchronizer.checkFileForSync
import org.zowe.explorer.dataops.operations.RenameOperation
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.FileLikeDatasetNode
import org.zowe.explorer.explorer.ui.LibraryNode
import org.zowe.explorer.explorer.ui.RenameDialog
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.runInEdtAndWait

class DuplicateMemberActionTestSpec : AppInitShouldSpec("explorer/actions/DuplicateMemberAction", {
  context("all functions") {
    var didCheckFileForSync = false
    var didPerformRenameOperation = false
    var didCleanCache = false
    var didNotifyError = false
    var didChangeIsEnabledAndVisible = false
    var didChangeIsVisible = false
    var isEnabledAndVisibleNewValue: Boolean? = null
    var isVisibleNewValue: Boolean? = null

    val fileExplorerViewMock = mockk<FileExplorerView>()
    val eventMock = mockk<AnActionEvent> {
      every { presentation } returns mockk {
        every {
          isEnabledAndVisible = any()
        } answers {
          didChangeIsEnabledAndVisible = true
          isEnabledAndVisibleNewValue = firstArg<Boolean>()
        }
        every {
          isVisible = any()
        } answers {
          didChangeIsVisible = true
          isVisibleNewValue = firstArg<Boolean>()
        }
      }
    }
    mockkStatic(::checkFileForSync)
    mockkConstructor(RenameDialog::class)

    val duplicateMemberAction = DuplicateMemberAction()

    val dataOpsManager = DataOpsManager.getService()
    val notificationsService = NotificationsService.getService()
    every {
      notificationsService
        .notifyError(any(), any(), any(), any(), any())
    } answers {
      didNotifyError = true
    }

    beforeEach {
      didCheckFileForSync = false
      didPerformRenameOperation = false
      didCleanCache = false
      didNotifyError = false
      didChangeIsEnabledAndVisible = false
      didChangeIsVisible = false
      isEnabledAndVisibleNewValue = null
      isVisibleNewValue = null

      every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
        mockk {
          every { attributes } returns mockk<RemoteMemberAttributes>()
          every { file } returns mockk {
            every { name } returns "TESTMBR"
          }
          every { node } returns mockk<FileLikeDatasetNode> {
            every { parent } returns mockk<LibraryNode> {
              every {
                cleanCache(any(), any(), any(), any())
              } answers {
                didCleanCache = true
              }
            }
          }
        }
      )
      every { eventMock.project } returns mockk()
      every { eventMock.getData(EXPLORER_VIEW) } returns fileExplorerViewMock
      every {
        checkFileForSync(any(), any(), any())
      } answers {
        didCheckFileForSync = true
        false
      }
      every { anyConstructed<RenameDialog>().showAndGet() } returns true

      every {
        dataOpsManager.performOperation(any<Operation<Any>>(), any())
      } answers {
        val operation = firstArg<Operation<*>>()

        if (operation is RenameOperation) {
          didPerformRenameOperation = true
        } else {
          fail("Unexpected operation: $operation")
        }
      }
    }

    context("actionPerformed") {
      should("perform Duplicate on a member") {
        runInEdtAndWait {
          duplicateMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didCheckFileForSync shouldBe true
          didPerformRenameOperation shouldBe true
          didCleanCache shouldBe true
          didNotifyError shouldBe false
        }
      }

      should("fail to perform Duplicate on a member due to a remote connection error") {
        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any())
        } answers {
          didPerformRenameOperation = true
          throw Throwable("Test error")
        }

        runInEdtAndWait {
          duplicateMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didCheckFileForSync shouldBe true
          didPerformRenameOperation shouldBe true
          didCleanCache shouldBe false
          didNotifyError shouldBe true
        }
      }

      should("not perform Duplicate on a member cause a user cancelled the duplicate dialog") {
        every { anyConstructed<RenameDialog>().showAndGet() } returns false

        runInEdtAndWait {
          duplicateMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didCheckFileForSync shouldBe true
          didPerformRenameOperation shouldBe false
          didCleanCache shouldBe false
          didNotifyError shouldBe false
        }
      }

      should("not perform Duplicate on a member cause the member being synced") {
        every {
          checkFileForSync(any(), any(), any())
        } answers {
          didCheckFileForSync = true
          true
        }

        runInEdtAndWait {
          duplicateMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didCheckFileForSync shouldBe true
          didPerformRenameOperation shouldBe false
          didCleanCache shouldBe false
          didNotifyError shouldBe false
        }
      }

      should("not perform Duplicate on a member cause there is no associated virtual file for the member") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { attributes } returns mockk()
            every { file } returns null
          }
        )

        runInEdtAndWait {
          duplicateMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didCheckFileForSync shouldBe false
          didPerformRenameOperation shouldBe false
          didCleanCache shouldBe false
          didNotifyError shouldBe false
        }
      }

      should("not perform Duplicate on a member cause there is no associated attributes for the member") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { attributes } returns null
          }
        )

        runInEdtAndWait {
          duplicateMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didCheckFileForSync shouldBe false
          didPerformRenameOperation shouldBe false
          didCleanCache shouldBe false
          didNotifyError shouldBe false
        }
      }

      should("not perform Duplicate on a member cause File Explorer view is not initialized") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        runInEdtAndWait {
          duplicateMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didCheckFileForSync shouldBe false
          didPerformRenameOperation shouldBe false
          didCleanCache shouldBe false
          didNotifyError shouldBe false
        }
      }

      should("not perform Duplicate on a member cause there is no project initialized") {
        every { eventMock.project } returns null

        runInEdtAndWait {
          duplicateMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didCheckFileForSync shouldBe false
          didPerformRenameOperation shouldBe false
          didCleanCache shouldBe false
          didNotifyError shouldBe false
        }
      }
    }

    context("update") {
      should("show the Duplicate action on a member") {
        duplicateMemberAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          isEnabledAndVisibleNewValue shouldBe null
          didChangeIsVisible shouldBe true
          isVisibleNewValue shouldBe true
        }
      }

      should("not show the Duplicate action on a USS entity") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { attributes } returns mockk<RemoteUssAttributes>()
            every { node } returns mockk<FileLikeDatasetNode>()
          }
        )

        duplicateMemberAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          isEnabledAndVisibleNewValue shouldBe null
          didChangeIsVisible shouldBe true
          isVisibleNewValue shouldBe false
        }
      }

      should("not show the Duplicate action on a dataset entity") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<LibraryNode>()
            every { attributes } returns null
          }
        )

        duplicateMemberAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          isEnabledAndVisibleNewValue shouldBe null
          didChangeIsVisible shouldBe true
          isVisibleNewValue shouldBe false
        }
      }

      should("not show the Duplicate action cause there is more than one entity selected") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk()
            every { attributes } returns null
          },
          mockk {
            every { node } returns mockk()
            every { attributes } returns null
          }
        )

        duplicateMemberAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          isEnabledAndVisibleNewValue shouldBe null
          didChangeIsVisible shouldBe true
          isVisibleNewValue shouldBe false
        }
      }

      should("not show the Duplicate action cause there is no entities selected") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf()

        duplicateMemberAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          isEnabledAndVisibleNewValue shouldBe null
          didChangeIsVisible shouldBe true
          isVisibleNewValue shouldBe false
        }
      }

      should("not show the Duplicate action cause File Explorer view is not initialized") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        duplicateMemberAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
          didChangeIsVisible shouldBe false
          isVisibleNewValue shouldBe null
        }
      }
    }
  }
})
