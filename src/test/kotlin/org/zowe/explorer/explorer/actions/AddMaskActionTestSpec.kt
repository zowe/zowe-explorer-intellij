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
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.AddOrEditMaskDialog
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.ExplorerUnitTreeNodeBase
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.MaskType
import org.zowe.explorer.utils.runInEdtAndWait

class AddMaskActionTestSpec : AppInitShouldSpec("explorer/actions/AddMaskAction", {
  context("all functions") {
    var didAddZosMask = false
    var didAddUssMask = false
    var didChangeIsEnabledAndVisible = false
    var isEnabledAndVisibleNewValue: Boolean? = null

    val addMaskAction = AddMaskAction()

    val filesWorkingSetMock = mockk<FilesWorkingSet> {
      every {
        addMask(any())
      } answers {
        didAddZosMask = true
      }
      every {
        addUssPath(any())
      } answers {
        didAddUssMask = true
      }
      every { name } returns "test"
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

    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "TEST"

    beforeEach {
      didAddZosMask = false
      didAddUssMask = false
      isEnabledAndVisibleNewValue = null

      every { filesWorkingSetMock.connectionConfig } returns mockk {
        every { uuid } returns "test_uuid"
      }
      every { filesExplorerView.mySelectedNodesData } returns listOf(
        mockk {
          every { node } returns mockk<ExplorerUnitTreeNodeBase<ConnectionConfig, *, *>> {
            every { unit } returns filesWorkingSetMock
          }
        }
      )
      every { eventMock.getData(EXPLORER_VIEW) } returns filesExplorerView
    }

    context("actionPerformed") {
      var didTriggerShowAndGet = false

      mockkConstructor(AddOrEditMaskDialog::class)

      beforeEach {
        didTriggerShowAndGet = false

        every {
          anyConstructed<AddOrEditMaskDialog>().showAndGet()
        } answers {
          didTriggerShowAndGet = true
          true
        }
        every {
          anyConstructed<AddOrEditMaskDialog>().state
        } answers {
          val originalState = fieldValue
          originalState.type = MaskType.ZOS
          originalState
        }
      }

      should("add a new z/OS mask when the Add Mask dialog is fulfilled") {
        runInEdtAndWait {
          addMaskAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe true
          didAddZosMask shouldBe true
          didAddUssMask shouldBe false
        }
      }

      should("add a new USS mask when the Add Mask dialog is fulfilled") {
        every {
          anyConstructed<AddOrEditMaskDialog>().state
        } answers {
          val originalState = fieldValue
          originalState.type = MaskType.USS
          originalState
        }

        runInEdtAndWait {
          addMaskAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe true
          didAddZosMask shouldBe false
          didAddUssMask shouldBe true
        }
      }

      should("add a new mask when the Add Mask dialog is fulfilled and there is no connection config found present") {
        every { filesWorkingSetMock.connectionConfig } returns null

        runInEdtAndWait {
          addMaskAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe true
          didAddZosMask shouldBe true
          didAddUssMask shouldBe false
        }
      }

      should("not add a new mask when the Add Mask dialog is cancelled") {
        every {
          anyConstructed<AddOrEditMaskDialog>().showAndGet()
        } answers {
          didTriggerShowAndGet = true
          false
        }

        runInEdtAndWait {
          addMaskAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe true
          didAddZosMask shouldBe false
          didAddUssMask shouldBe false
        }
      }

      should("not open an Add Mask dialog when there is no working sets") {
        every { filesExplorerView.mySelectedNodesData } returns listOf()

        runInEdtAndWait {
          addMaskAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe false
          didAddZosMask shouldBe false
          didAddUssMask shouldBe false
        }
      }

      should("not open an Add Mask dialog when it is not a Files Explorer view as a current view") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        runInEdtAndWait {
          addMaskAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe false
          didAddZosMask shouldBe false
          didAddUssMask shouldBe false
        }
      }
    }

    context("update") {
      should("show the Add Mask option in a context menu when it is a Files Explorer view and there is a files working set is selected") {
        addMaskAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe true
        }
      }

      should("not show the Add Mask option in a context menu when more than one files working set's element is selected in different working sets") {
        every { filesExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<ExplorerUnitTreeNodeBase<ConnectionConfig, *, *>> {
              every { unit } returns filesWorkingSetMock
            }
          },
          mockk {
            every { node } returns mockk<ExplorerUnitTreeNodeBase<ConnectionConfig, *, *>> {
              every { unit } returns mockk<FilesWorkingSet>()
            }
          }
        )

        addMaskAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
        }
      }

      should("not show the Add Mask option in a context menu when the current view is not a Files Explorer view") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        addMaskAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
        }
      }
    }
  }
})
