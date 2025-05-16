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
import org.zowe.explorer.explorer.JesWorkingSet
import org.zowe.explorer.explorer.ui.AddJobsFilterDialog
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.ExplorerUnitTreeNodeBase
import org.zowe.explorer.explorer.ui.JesExplorerView
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.runInEdtAndWait

class AddJobsFilterActionTestSpec : AppInitShouldSpec("explorer/actions/AddJobsFilterAction", {
  context("all functions") {
    var didAddMask = false
    var didChangeIsEnabledAndVisible = false
    var isEnabledAndVisibleNewValue: Boolean? = null

    val addJobsFilterAction = AddJobsFilterAction()

    val jesWorkingSetMock = mockk<JesWorkingSet> {
      every {
        addMask(any())
      } answers {
        didAddMask = true
      }
      every { name } returns "test"
    }
    val jesExplorerView = mockk<JesExplorerView>()
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
      didAddMask = false
      isEnabledAndVisibleNewValue = null

      every { jesWorkingSetMock.connectionConfig } returns mockk {
        every { uuid } returns "test_uuid"
      }
      every { jesExplorerView.mySelectedNodesData } returns listOf(
        mockk {
          every { node } returns mockk<ExplorerUnitTreeNodeBase<ConnectionConfig, *, *>> {
            every { unit } returns jesWorkingSetMock
          }
        }
      )
      every { eventMock.getData(EXPLORER_VIEW) } returns jesExplorerView
    }

    context("actionPerformed") {
      var didTriggerShowAndGet = false

      mockkConstructor(AddJobsFilterDialog::class)

      beforeEach {
        didTriggerShowAndGet = false

        every {
          anyConstructed<AddJobsFilterDialog>().showAndGet()
        } answers {
          didTriggerShowAndGet = true
          true
        }
      }

      should("add a new job filter when the Add Job Filter dialog is fulfilled") {
        runInEdtAndWait {
          addJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe true
          didAddMask shouldBe true
        }
      }

      should("add a new job filter when the Add Job Filter dialog is fulfilled and there is no connection config found present") {
        every { jesWorkingSetMock.connectionConfig } returns null

        runInEdtAndWait {
          addJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe true
          didAddMask shouldBe true
        }
      }

      should("not add a new job filter when the Add Job Filter dialog is cancelled") {
        every {
          anyConstructed<AddJobsFilterDialog>().showAndGet()
        } answers {
          didTriggerShowAndGet = true
          false
        }

        runInEdtAndWait {
          addJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe true
          didAddMask shouldBe false
        }
      }

      should("not open an Add Job Filter dialog when there is no working sets") {
        every { jesExplorerView.mySelectedNodesData } returns listOf()

        runInEdtAndWait {
          addJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe false
          didAddMask shouldBe false
        }
      }

      should("not open an Add Job Filter dialog when it is not a JES Explorer view as a current view") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        runInEdtAndWait {
          addJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe false
          didAddMask shouldBe false
        }
      }
    }

    context("update") {
      should("show the Add Job Filter option in a context menu when it is a JES Explorer view and there is a JES working set is selected") {
        addJobsFilterAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe true
        }
      }

      should("not show the Add Job Filter option in a context menu when more than one JES working set's element is selected in different working sets") {
        every { jesExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<ExplorerUnitTreeNodeBase<ConnectionConfig, *, *>> {
              every { unit } returns jesWorkingSetMock
            }
          },
          mockk {
            every { node } returns mockk<ExplorerUnitTreeNodeBase<ConnectionConfig, *, *>> {
              every { unit } returns mockk<JesWorkingSet>()
            }
          }
        )

        addJobsFilterAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
        }
      }

      should("not show the Add Job Filter option in a context menu when the current view is not a JES Explorer view") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        addJobsFilterAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
        }
      }
    }
  }
})
