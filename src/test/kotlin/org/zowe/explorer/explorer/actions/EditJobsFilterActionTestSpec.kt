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
import io.mockk.mockkStatic
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.config.ws.JobFilterStateWithMultipleWS
import org.zowe.explorer.config.ws.JobsFilter
import org.zowe.explorer.explorer.JesWorkingSet
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.EditJobsFilterDialog
import org.zowe.explorer.explorer.ui.JesExplorerView
import org.zowe.explorer.explorer.ui.JesFilterNode
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.getSelectedNodesWorkingSets
import org.zowe.explorer.utils.runInEdtAndWait
import java.util.Optional
import kotlin.reflect.KFunction

class EditJobsFilterActionTestSpec : AppInitShouldSpec("explorer/actions/EditJobsFilterAction", {
  context("all functions") {
    var didTriggerShowAndGet = false
    var didTriggerGetByUniqueKey = false
    var didTriggerUpdate = false
    var isEnabledAndVisibleNewValue: Boolean? = null

    val getSelectedNodesWorkingSetsMock: (JesExplorerView) -> List<JesWorkingSet> = ::getSelectedNodesWorkingSets
    mockkStatic(getSelectedNodesWorkingSetsMock as KFunction<*>)

    val jesExplorerViewMock = mockk<JesExplorerView>()
    val eventMock = mockk<AnActionEvent> {
      every { project } returns mockk()
      every { presentation } returns mockk {
        every {
          isEnabledAndVisible = any()
        } answers {
          isEnabledAndVisibleNewValue = firstArg<Boolean>()
        }
      }
    }

    val editJobsFilterAction = EditJobsFilterAction()

    mockkConstructor(EditJobsFilterDialog::class)

    val jobFilterDialogState = mockk<JobFilterStateWithMultipleWS>()
    every { anyConstructed<EditJobsFilterDialog>().state } returns jobFilterDialogState

    val configServiceCrudable = mockk<Crudable> {
      every {
        update(any())
      } answers {
        didTriggerUpdate = true
        Optional.ofNullable(null)
      }
    }
    val configService = ConfigService.getService()
    every { configService.crudable } returns configServiceCrudable

    beforeEach {
      didTriggerShowAndGet = false
      didTriggerGetByUniqueKey = false
      didTriggerUpdate = false
      isEnabledAndVisibleNewValue = null

      every { eventMock.getData(EXPLORER_VIEW) } returns jesExplorerViewMock
      every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
        mockk {
          every { node } returns mockk<JesFilterNode> {
            every { value } returns JobsFilter("TEST", "TEST", "TEST")
          }
        }
      )
      every { getSelectedNodesWorkingSetsMock(any()) } returns listOf(
        mockk {
          every { uuid } returns "test_uuid"
          every { name } returns "test"
          every { masks } returns listOf()
        }
      )

      every { jobFilterDialogState.toJobsFilter() } returns JobsFilter("TEST", "TEST", "TEST")

      every {
        anyConstructed<EditJobsFilterDialog>().showAndGet()
      } answers {
        didTriggerShowAndGet = true
        true
      }

      every {
        configServiceCrudable.getByUniqueKey(JesWorkingSetConfig::class.java, any<String>())
      } answers {
        didTriggerGetByUniqueKey = true
        Optional.of(
          mockk {
            every { jobsFilters } returns mutableListOf(
              JobsFilter("TEST", "TEST", "TEST")
            )
          }
        )
      }
    }

    context("actionPerformed") {
      should("perform jobs filter Edit action, changing job ID only") {
        every { jobFilterDialogState.toJobsFilter() } returns JobsFilter("TEST", "TEST", "NEW")

        runInEdtAndWait {
          editJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe true
          didTriggerGetByUniqueKey shouldBe true
          didTriggerUpdate shouldBe true
        }
      }

      should("perform jobs filter Edit action, changing prefix only") {
        every { jobFilterDialogState.toJobsFilter() } returns JobsFilter("TEST", "NEW", "TEST")

        runInEdtAndWait {
          editJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe true
          didTriggerGetByUniqueKey shouldBe true
          didTriggerUpdate shouldBe true
        }
      }

      should("perform jobs filter Edit action, changing owner only") {
        every { jobFilterDialogState.toJobsFilter() } returns JobsFilter("NEW", "TEST", "TEST")

        runInEdtAndWait {
          editJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe true
          didTriggerGetByUniqueKey shouldBe true
          didTriggerUpdate shouldBe true
        }
      }

      should("not perform jobs filter Edit action when there is no jobs filters in the working set") {
        every { jobFilterDialogState.toJobsFilter() } returns JobsFilter("TEST", "TEST", "NEW")

        every {
          configServiceCrudable.getByUniqueKey(JesWorkingSetConfig::class.java, any<String>())
        } answers {
          didTriggerGetByUniqueKey = true
          Optional.of(
            mockk { every { jobsFilters } returns mutableListOf() }
          )
        }

        runInEdtAndWait {
          editJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe true
          didTriggerGetByUniqueKey shouldBe true
          didTriggerUpdate shouldBe false
        }
      }

      should("not perform jobs filter Edit action when there is no related job filters in the working set") {
        every { jobFilterDialogState.toJobsFilter() } returns JobsFilter("TEST", "TEST", "NEW")

        every {
          configServiceCrudable.getByUniqueKey(JesWorkingSetConfig::class.java, any<String>())
        } answers {
          didTriggerGetByUniqueKey = true
          Optional.of(
            mockk {
              every { jobsFilters } returns mutableListOf(JobsFilter("OTHER", "OTHER", "OTHER"))
            }
          )
        }

        runInEdtAndWait {
          editJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe true
          didTriggerGetByUniqueKey shouldBe true
          didTriggerUpdate shouldBe false
        }
      }

      should("not perform jobs filter Edit action when there is no JES working sets to update") {
        every { jobFilterDialogState.toJobsFilter() } returns JobsFilter("TEST", "TEST", "NEW")

        every {
          configServiceCrudable.getByUniqueKey(JesWorkingSetConfig::class.java, any<String>())
        } answers {
          didTriggerGetByUniqueKey = true
          Optional.ofNullable(null)
        }

        runInEdtAndWait {
          editJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe true
          didTriggerGetByUniqueKey shouldBe true
          didTriggerUpdate shouldBe false
        }
      }

      should("not perform jobs filter Edit action when there is no updates for the job filter") {
        every { jobFilterDialogState.toJobsFilter() } returns JobsFilter("TEST", "TEST", "TEST")

        runInEdtAndWait {
          editJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe true
          didTriggerGetByUniqueKey shouldBe false
          didTriggerUpdate shouldBe false
        }
      }

      should("not perform jobs filter Edit action cause a user cancelled the edit dialog") {
        every {
          anyConstructed<EditJobsFilterDialog>().showAndGet()
        } answers {
          didTriggerShowAndGet = true
          false
        }

        runInEdtAndWait {
          editJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe true
          didTriggerGetByUniqueKey shouldBe false
          didTriggerUpdate shouldBe false
        }
      }

      should("not perform jobs filter Edit action cause there JES working sets") {
        every { getSelectedNodesWorkingSetsMock(any()) } returns listOf()

        runInEdtAndWait {
          editJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe false
          didTriggerGetByUniqueKey shouldBe false
          didTriggerUpdate shouldBe false
        }
      }

      should("not perform jobs filter Edit action cause the selected node is not a JES filter node") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(mockk { every { node } returns mockk() })

        runInEdtAndWait {
          editJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe false
          didTriggerGetByUniqueKey shouldBe false
          didTriggerUpdate shouldBe false
        }
      }

      should("not perform jobs filter Edit action cause there is no selected nodes") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf()

        runInEdtAndWait {
          editJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe false
          didTriggerGetByUniqueKey shouldBe false
          didTriggerUpdate shouldBe false
        }
      }

      should("not perform jobs filter Edit action cause the JES Explorer view is not initialized yet") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        runInEdtAndWait {
          editJobsFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerShowAndGet shouldBe false
          didTriggerGetByUniqueKey shouldBe false
          didTriggerUpdate shouldBe false
        }
      }
    }

    context("update") {
      should("show the Edit Job Filter action") {
        editJobsFilterAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe true }
      }

      should("not show the Edit Job Filter action cause there is no selected nodes") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf()

        editJobsFilterAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe false }
      }

      should("not show the Edit Job Filter action cause the JES Explorer view is not initialized yet") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        editJobsFilterAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe false }
      }
    }
  }
})
