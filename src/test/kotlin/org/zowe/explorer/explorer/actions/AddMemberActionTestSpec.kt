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
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributesService
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.operations.DeleteMemberOperation
import org.zowe.explorer.dataops.operations.MemberAllocationOperation
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.JesWorkingSet
import org.zowe.explorer.explorer.ui.AddMemberDialog
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.FileLikeDatasetNode
import org.zowe.explorer.explorer.ui.FilesWorkingSetNode
import org.zowe.explorer.explorer.ui.JesFilterNode
import org.zowe.explorer.explorer.ui.LibraryNode
import org.zowe.explorer.explorer.ui.UssDirNode
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.runInEdtAndWait
import org.zowe.explorer.vfs.MFVirtualFile

class AddMemberActionTestSpec : AppInitShouldSpec("explorer/actions/AddMemberAction", {
  context("all functions") {
    var didTriggerGetAttributesService = false
    var didTriggerMemberAllocationOperation = false
    var didTriggerDeleteMemberOperation = false
    var didTriggerNotifyError = false
    var didChangeIsEnabledAndVisible = false
    var isEnabledAndVisibleNewValue: Boolean? = null
    var cleanCacheTriggerCount = 0

    val addMemberAction = AddMemberAction()

    val filesWorkingSetMock = mockk<FilesWorkingSet>()
    val libraryNode = mockk<LibraryNode> {
      every { unit } returns filesWorkingSetMock
      every { virtualFile } returns mockk()
      every {
        cleanCache(any(), any(), any(), any())
      } answers {
        cleanCacheTriggerCount += 1
      }
    }
    val filesExplorerView = mockk<FileExplorerView>()
    val eventMock = mockk<AnActionEvent> {
      every { project } returns mockk()
      every { presentation } returns mockk {
        every {
          isEnabledAndVisible = any<Boolean>()
        } answers {
          didChangeIsEnabledAndVisible = true
          isEnabledAndVisibleNewValue = firstArg<Boolean>()
        }
      }
    }
    val remoteDatasetAttributesService = mockk<RemoteDatasetAttributesService>()

    val dataOpsManager = DataOpsManager.getService()
    every {
      dataOpsManager
        .getAttributesService(RemoteDatasetAttributes::class.java, MFVirtualFile::class.java)
    } answers {
      didTriggerGetAttributesService = true
      remoteDatasetAttributesService
    }

    val notificationsService = NotificationsService.getService()
    every {
      notificationsService
        .notifyError(
          any<Throwable>(),
          any<Project>(),
          any<String>(),
          any<String>(),
          any<String>()
        )
    } answers {
      didTriggerNotifyError = true
    }

    beforeEach {
      didTriggerGetAttributesService = false
      didTriggerMemberAllocationOperation = false
      didTriggerDeleteMemberOperation = false
      didTriggerNotifyError = false
      didChangeIsEnabledAndVisible = false
      isEnabledAndVisibleNewValue = null
      cleanCacheTriggerCount = 0

      every { filesWorkingSetMock.connectionConfig } returns mockk()
      every { filesExplorerView.mySelectedNodesData } returns listOf(
        mockk { every { node } returns libraryNode }
      )
      every { eventMock.getData(EXPLORER_VIEW) } returns filesExplorerView
      every { remoteDatasetAttributesService.getAttributes(any()) } returns mockk {
        every { name } returns "TEST.DATASET.NAME"
      }

      every {
        dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
      } answers {
        when (val operation = firstArg<Operation<*>>()) {
          is MemberAllocationOperation -> {
            didTriggerMemberAllocationOperation = true
            mockk()
          }
          is DeleteMemberOperation -> {
            didTriggerDeleteMemberOperation = true
            mockk()
          }
          else -> fail("Unknown operation triggered: $operation")
        }
      }
    }

    context("actionPerformed") {
      var didTriggerShowAndGet = false

      mockkConstructor(AddMemberDialog::class)

      beforeEach {
        didTriggerShowAndGet = false

        every {
          anyConstructed<AddMemberDialog>().showAndGet()
        } answers {
          didTriggerShowAndGet = true
          true
        }
      }

      should("successfully allocate a member") {
        runInEdtAndWait {
          addMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerGetAttributesService shouldBe true
          didTriggerShowAndGet shouldBe true
          didTriggerMemberAllocationOperation shouldBe true
          didTriggerDeleteMemberOperation shouldBe false
          didTriggerNotifyError shouldBe false
          cleanCacheTriggerCount shouldBe 1
        }
      }

      should("successfully allocate a member when a context menu was called for an element that is a child of a container node") {
        every { filesExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<FileLikeDatasetNode> {
              every { parent } returns libraryNode
            }
          }
        )

        runInEdtAndWait {
          addMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerGetAttributesService shouldBe true
          didTriggerShowAndGet shouldBe true
          didTriggerMemberAllocationOperation shouldBe true
          didTriggerDeleteMemberOperation shouldBe false
          didTriggerNotifyError shouldBe false
          cleanCacheTriggerCount shouldBe 1
        }
      }

      should("fail to allocate a member due to a storage area insufficiency, successfully deleting it afterwards") {
        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } answers {
          when (val operation = firstArg<Operation<*>>()) {
            is MemberAllocationOperation -> {
              didTriggerMemberAllocationOperation = true
              throw CallException(500, "Directory full")
            }
            is DeleteMemberOperation -> {
              didTriggerDeleteMemberOperation = true
              mockk()
            }
            else -> fail("Unknown operation triggered: $operation")
          }
        }

        runInEdtAndWait {
          addMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerGetAttributesService shouldBe true
          didTriggerShowAndGet shouldBe true
          didTriggerMemberAllocationOperation shouldBe true
          didTriggerDeleteMemberOperation shouldBe true
          didTriggerNotifyError shouldBe true
          cleanCacheTriggerCount shouldBe 0
        }
      }

      should("fail to allocate a member due to a storage area insufficiency, and fail to delete it afterwards") {
        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } answers {
          when (val operation = firstArg<Operation<*>>()) {
            is MemberAllocationOperation -> {
              didTriggerMemberAllocationOperation = true
              throw CallException(500, "Directory full")
            }
            is DeleteMemberOperation -> {
              didTriggerDeleteMemberOperation = true
              throw Throwable("Test exception")
            }
            else -> fail("Unknown operation triggered: $operation")
          }
        }

        runInEdtAndWait {
          addMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerGetAttributesService shouldBe true
          didTriggerShowAndGet shouldBe true
          didTriggerMemberAllocationOperation shouldBe true
          didTriggerDeleteMemberOperation shouldBe true
          didTriggerNotifyError shouldBe true
          cleanCacheTriggerCount shouldBe 1
        }
      }

      should("fail to allocate a member due to some different error") {
        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } answers {
          when (val operation = firstArg<Operation<*>>()) {
            is MemberAllocationOperation -> {
              didTriggerMemberAllocationOperation = true
              throw Throwable("Test error")
            }
            is DeleteMemberOperation -> {
              didTriggerDeleteMemberOperation = true
              mockk()
            }
            else -> fail("Unknown operation triggered: $operation")
          }
        }

        runInEdtAndWait {
          addMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerGetAttributesService shouldBe true
          didTriggerShowAndGet shouldBe true
          didTriggerMemberAllocationOperation shouldBe true
          didTriggerDeleteMemberOperation shouldBe false
          didTriggerNotifyError shouldBe true
          cleanCacheTriggerCount shouldBe 0
        }
      }

      should("not allocate a member as a user cancelled the dialog") {
        every {
          anyConstructed<AddMemberDialog>().showAndGet()
        } answers {
          didTriggerShowAndGet = true
          false
        }

        runInEdtAndWait {
          addMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerGetAttributesService shouldBe true
          didTriggerShowAndGet shouldBe true
          didTriggerMemberAllocationOperation shouldBe false
          didTriggerDeleteMemberOperation shouldBe false
          didTriggerNotifyError shouldBe false
          cleanCacheTriggerCount shouldBe 0
        }
      }

      should("not allocate a member as there is no attributes of the dataset to allocate the member in") {
        every { remoteDatasetAttributesService.getAttributes(any()) } returns null

        runInEdtAndWait {
          addMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerGetAttributesService shouldBe true
          didTriggerShowAndGet shouldBe false
          didTriggerMemberAllocationOperation shouldBe false
          didTriggerDeleteMemberOperation shouldBe false
          didTriggerNotifyError shouldBe false
          cleanCacheTriggerCount shouldBe 0
        }
      }

      should("not allocate a member as there is no connection config associated with the dataset") {
        every { filesWorkingSetMock.connectionConfig } returns null

        runInEdtAndWait {
          addMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerGetAttributesService shouldBe false
          didTriggerShowAndGet shouldBe false
          didTriggerMemberAllocationOperation shouldBe false
          didTriggerDeleteMemberOperation shouldBe false
          didTriggerNotifyError shouldBe false
          cleanCacheTriggerCount shouldBe 0
        }
      }

      should("not allocate a member as the selected element is a USS directory") {
        every { filesExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<UssDirNode> {
              every { unit } returns filesWorkingSetMock
            }
          }
        )

        runInEdtAndWait {
          addMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerGetAttributesService shouldBe false
          didTriggerShowAndGet shouldBe false
          didTriggerMemberAllocationOperation shouldBe false
          didTriggerDeleteMemberOperation shouldBe false
          didTriggerNotifyError shouldBe false
          cleanCacheTriggerCount shouldBe 0
        }
      }

      should("not allocate a member as the selected element is a Jobs filter") {
        every { filesExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<JesFilterNode> {
              every { unit } returns mockk<JesWorkingSet>()
            }
          }
        )

        runInEdtAndWait {
          addMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerGetAttributesService shouldBe false
          didTriggerShowAndGet shouldBe false
          didTriggerMemberAllocationOperation shouldBe false
          didTriggerDeleteMemberOperation shouldBe false
          didTriggerNotifyError shouldBe false
          cleanCacheTriggerCount shouldBe 0
        }
      }

      should("not allocate a member as the selected element is not a library node and not a child of a library node") {
        every { filesExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<FileLikeDatasetNode> {
              every { parent } returns mockk<FilesWorkingSetNode>()
            }
          }
        )

        runInEdtAndWait {
          addMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerGetAttributesService shouldBe false
          didTriggerShowAndGet shouldBe false
          didTriggerMemberAllocationOperation shouldBe false
          didTriggerDeleteMemberOperation shouldBe false
          didTriggerNotifyError shouldBe false
          cleanCacheTriggerCount shouldBe 0
        }
      }

      should("not allocate a member as the selected element is a Files Working set") {
        every { filesExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<FilesWorkingSetNode> {
              every { parent } returns null
            }
          }
        )

        runInEdtAndWait {
          addMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerGetAttributesService shouldBe false
          didTriggerShowAndGet shouldBe false
          didTriggerMemberAllocationOperation shouldBe false
          didTriggerDeleteMemberOperation shouldBe false
          didTriggerNotifyError shouldBe false
          cleanCacheTriggerCount shouldBe 0
        }
      }

      should("not allocate a member as the current view is not a Files Explorer view") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        runInEdtAndWait {
          addMemberAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerGetAttributesService shouldBe false
          didTriggerShowAndGet shouldBe false
          didTriggerMemberAllocationOperation shouldBe false
          didTriggerDeleteMemberOperation shouldBe false
          didTriggerNotifyError shouldBe false
          cleanCacheTriggerCount shouldBe 0
        }
      }
    }

    context("update") {
      should("show the Add Member option in a context menu when it is a Files Explorer view and there is a PDS / PDS/E selected") {
        addMemberAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe true
        }
      }

      should("show the Add Member option in a context menu when it is a Files Explorer view and there is a PDS's / PDS/E's child selected") {
        every { filesExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<FileLikeDatasetNode>()
            every { attributes } returns mockk<RemoteMemberAttributes>()
          }
        )

        addMemberAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe true
        }
      }

      should("not show the Add Member option in a context menu when it is a Files Explorer view and there is a PS selected") {
        every { filesExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<FileLikeDatasetNode>()
            every { attributes } returns mockk()
          }
        )

        addMemberAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
        }
      }

      should("not show the Add Member option in a context menu when it is a Files Explorer view and there is a USS directory selected") {
        every { filesExplorerView.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk<UssDirNode>() }
        )

        addMemberAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
        }
      }

      should("not show the Add Member option in a context menu cause there is no elements selected") {
        every { filesExplorerView.mySelectedNodesData } returns listOf()

        addMemberAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
        }
      }

      should("not show the Add Member option in a context menu when the current view is not a Files Explorer view") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        addMemberAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
        }
      }
    }
  }
})
