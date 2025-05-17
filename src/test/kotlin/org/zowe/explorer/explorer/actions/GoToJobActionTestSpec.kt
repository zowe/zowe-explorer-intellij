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
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.impl.ContentImpl
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.zowe.explorer.ui.build.jobs.JOBS_LOG_VIEW
import io.mockk.*
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.config.ws.JobsFilter
import org.zowe.explorer.config.ws.ui.jes.JesWsDialog
import org.zowe.explorer.dataops.log.JobLogFetcher
import org.zowe.explorer.explorer.JesExplorer
import org.zowe.explorer.explorer.JesExplorerContentProvider
import org.zowe.explorer.explorer.JesWorkingSet
import org.zowe.explorer.explorer.UIComponentManager
import org.zowe.explorer.explorer.ui.AddJobsFilterDialog
import org.zowe.explorer.explorer.ui.CommonExplorerTreeStructure
import org.zowe.explorer.explorer.ui.ExplorerTreeView
import org.zowe.explorer.explorer.ui.JesFilterNode
import org.zowe.explorer.explorer.ui.JesWsNode
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.ui.build.jobs.JobBuildTreeView
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.runInEdtAndWait
import java.util.Optional

class GoToJobActionTestSpec : AppInitShouldSpec("explorer/actions/GoToJobAction", {
  context("all functions") {
    var didTriggerJesWsDialogShowAndGet = false
    var didTriggerAddJobsFilterDialogShowAndGet = false
    var didTriggerShowNotification = false
    var didTriggerSetSelectedContent = false
    var didTriggerAddMask = false
    var didTriggerSelectTree = false
    var didTriggerExpandTree = false
    var addCallCount = 0
    var isEnabledNewValue: Boolean? = null
    var isVisibleNewValue: Boolean? = null

    val connectionConfigMock = mockk<ConnectionConfig> {
      every { uuid } returns "test"
    }
    val logFetcherMock = mockk<JobLogFetcher>()
    val jobsLogViewMock = mockk<JobBuildTreeView> {
      every { getConnectionConfig() } returns connectionConfigMock
      every { getJobLogger() } returns mockk {
        every { logFetcher } returns logFetcherMock
      }
    }
    val eventMock = mockk<AnActionEvent> {
      every { presentation } returns mockk {
        every {
          isEnabled = any()
        } answers {
          isEnabledNewValue = firstArg<Boolean>()
        }
        every {
          isVisible = any()
        } answers {
          isVisibleNewValue = firstArg<Boolean>()
        }
        every {
          isEnabledAndVisible = any()
        } answers {
          isEnabled = firstArg<Boolean>()
          isVisible = firstArg<Boolean>()
        }
      }
    }

    val goToJobAction = GoToJobAction()

    mockkObject(ToolWindowManager.Companion)
    every { ToolWindowManager.getInstance(any()) } returns mockk {
      every { getToolWindow(any()) } returns mockk {
        every { contentManager } returns mockk {
          every { getContent(any<ExplorerTreeView<*, *, *>>()) } returns mockk<ContentImpl>()
          every {
            setSelectedContent(any(), any())
          } answers {
            didTriggerSetSelectedContent = true
          }
        }
      }
    }

    mockkConstructor(JesWsDialog::class)
    every { anyConstructed<JesWsDialog>().state } returns mockk {
      every { workingSetConfig } returns mockk {
        every { jobsFilters } returns mutableListOf(JobsFilter("TEST", "TEST", "TEST"))
      }
    }

    mockkConstructor(AddJobsFilterDialog::class)

    val fsTreeStructureMock = mockk<CommonExplorerTreeStructure<*>>()

    val jesExplorerContentProviderMock = mockk<JesExplorerContentProvider>()

    var uiComponentManager = UIComponentManager.getService()
    every {
      uiComponentManager.getExplorerContentProvider(JesExplorer::class.java)
    } returns jesExplorerContentProviderMock

    val configServiceCrudable = mockk<Crudable> {
      every {
        add(any())
      } answers {
        addCallCount++
        Optional.ofNullable(null)
      }
      every {
        nextUniqueValue<JesWorkingSetConfig, String>(JesWorkingSetConfig::class.java)
      } returns "test_uuid"
      every {
        getAll(ConnectionConfig::class.java)
      } answers {
        listOf<ConnectionConfig>().stream()
      }
    }
    val configService = ConfigService.getService()
    every { configService.crudable } returns configServiceCrudable

    beforeEach {
      didTriggerJesWsDialogShowAndGet = false
      didTriggerAddJobsFilterDialogShowAndGet = false
      didTriggerShowNotification = false
      didTriggerSetSelectedContent = false
      didTriggerAddMask = false
      didTriggerSelectTree = false
      didTriggerExpandTree = false
      addCallCount = 0
      isEnabledNewValue = null
      isVisibleNewValue = null

      every { eventMock.project } returns mockk()
      every { logFetcherMock.getCachedJobStatus() } returns mockk {
        every { status } returns mockk()
      }
      every { jobsLogViewMock.jobLogInfo } returns mockk {
        every { jobId } returns "TEST"
      }
      every { eventMock.getData(JOBS_LOG_VIEW) } returns jobsLogViewMock

      every { fsTreeStructureMock.findByPredicate(any()) } returns listOf(
        mockk<JesWsNode> {
          every { unit } returns mockk {
            every { connectionConfig } returns mockk {
              every { uuid } returns "test"
            }
          }
        }
      )

      every { jesExplorerContentProviderMock.getExplorerView(any()) } returns mockk {
        every { myFsTreeStructure } returns fsTreeStructureMock
        every { explorer } returns mockk {
          every {
            showNotification(any(), any(), any(), any())
          } answers {
            didTriggerShowNotification = true
          }
        }
        every { myTree } returns mockk()
        every { myStructure } returns mockk {
          every {
            select(any(), any(), any())
          } answers {
            didTriggerSelectTree = true
          }
          every {
            expand(any(), any(), any())
          } answers {
            didTriggerExpandTree = true
          }
        }
      }

      every {
        anyConstructed<JesWsDialog>().showAndGet()
      } answers {
        didTriggerJesWsDialogShowAndGet = true
        true
      }

      every { anyConstructed<AddJobsFilterDialog>().state } returns mockk {
        every { toJobsFilter() } returns JobsFilter()
        every { selectedWS } returns mockk {
          every {
            addMask(any())
          } answers {
            didTriggerAddMask = true
          }
        }
      }
      every {
        anyConstructed<AddJobsFilterDialog>().showAndGet()
      } answers {
        didTriggerAddJobsFilterDialogShowAndGet = true
        true
      }
    }

    context("actionPerformed") {
      should("perform Go To Job action, creating a JES Working set with a defined jobs filter when there is no suitable JES Working sets on the same connection") {
        runInEdtAndWait {
          goToJobAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerJesWsDialogShowAndGet shouldBe true
          didTriggerAddJobsFilterDialogShowAndGet shouldBe false
          didTriggerShowNotification shouldBe true
          didTriggerSetSelectedContent shouldBe true
          didTriggerAddMask shouldBe false
          didTriggerSelectTree shouldBe false
          didTriggerExpandTree shouldBe false
          addCallCount shouldBe 1
        }
      }

      should("perform Go To Job action, not creating a JES Working set with a defined jobs filter when there is no suitable JES Working sets on the same connection, cause a user cancelled the dialog to create the JES Working set") {
        every {
          anyConstructed<JesWsDialog>().showAndGet()
        } answers {
          didTriggerJesWsDialogShowAndGet = true
          false
        }

        runInEdtAndWait {
          goToJobAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerJesWsDialogShowAndGet shouldBe true
          didTriggerAddJobsFilterDialogShowAndGet shouldBe false
          didTriggerShowNotification shouldBe false
          didTriggerSetSelectedContent shouldBe false
          didTriggerAddMask shouldBe false
          didTriggerSelectTree shouldBe false
          didTriggerExpandTree shouldBe false
          addCallCount shouldBe 0
        }
      }

      should("perform Go To Job action, creating a jobs filter in the first found suitable JES Working set on the same connection") {
        val compatibleWS = mockk<JesWorkingSet> {
          every { name } returns "test1"
          every { connectionConfig } returns connectionConfigMock
          every {
            addMask(any())
          } answers {
            didTriggerAddMask = true
          }
        }

        every { fsTreeStructureMock.findByPredicate(any()) } returns listOf(
          mockk<JesWsNode> {
            every { unit } returns compatibleWS
            every { name } returns "test1"
            every { children } returns mutableListOf(
              mockk<JesFilterNode> {
                every { value } returns mockk<JobsFilter> {
                  every { jobId } returns "UNIQUE"
                }
              }
            )
          },
          mockk<JesWsNode> {
            every { unit } returns mockk<JesWorkingSet> {
              every { connectionConfig } returns connectionConfigMock
            }
            every { name } returns null
          },
          mockk<JesWsNode> {
            every { unit } returns mockk<JesWorkingSet> {
              every { connectionConfig } returns connectionConfigMock
            }
            every { name } returns "test2"
            every { children } returns mutableListOf(
              mockk<JesFilterNode> {
                every { value } returns mockk<JobsFilter> {
                  every { jobId } returns "TEST"
                }
              }
            )
          }
        )

        every { anyConstructed<AddJobsFilterDialog>().state } returns mockk {
          every { toJobsFilter() } returns JobsFilter()
          every { selectedWS } returns compatibleWS
        }

        runInEdtAndWait {
          goToJobAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerJesWsDialogShowAndGet shouldBe false
          didTriggerAddJobsFilterDialogShowAndGet shouldBe true
          didTriggerShowNotification shouldBe true
          didTriggerSetSelectedContent shouldBe true
          didTriggerAddMask shouldBe true
          didTriggerSelectTree shouldBe true
          didTriggerExpandTree shouldBe true
          addCallCount shouldBe 0
        }
      }

      should("perform Go To Job action, creating a jobs filter in the first found suitable JES Working set on the same connection, without expanding the filter when there is no working set selected") {
        val compatibleWS = mockk<JesWorkingSet> {
          every { name } returns "test1"
          every { connectionConfig } returns connectionConfigMock
        }

        every { fsTreeStructureMock.findByPredicate(any()) } returns listOf(
          mockk<JesWsNode> {
            every { unit } returns compatibleWS
            every { name } returns "test1"
            every { children } returns mutableListOf(
              mockk<JesFilterNode> {
                every { value } returns mockk<JobsFilter> {
                  every { jobId } returns "UNIQUE"
                }
              }
            )
          },
          mockk<JesWsNode> {
            every { unit } returns mockk<JesWorkingSet> {
              every { connectionConfig } returns connectionConfigMock
            }
            every { name } returns null
          },
          mockk<JesWsNode> {
            every { unit } returns mockk<JesWorkingSet> {
              every { connectionConfig } returns connectionConfigMock
            }
            every { name } returns "test2"
            every { children } returns mutableListOf(
              mockk<JesFilterNode> {
                every { value } returns mockk<JobsFilter> {
                  every { jobId } returns "TEST"
                }
              }
            )
          }
        )

        runInEdtAndWait {
          goToJobAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerJesWsDialogShowAndGet shouldBe false
          didTriggerAddJobsFilterDialogShowAndGet shouldBe true
          didTriggerShowNotification shouldBe true
          didTriggerSetSelectedContent shouldBe true
          didTriggerAddMask shouldBe true
          didTriggerSelectTree shouldBe false
          didTriggerExpandTree shouldBe false
          addCallCount shouldBe 0
        }
      }

      should("not perform Go To Job action, creating a jobs filter in the first found suitable JES Working set on the same connection, cause a user cancelled the jobs filter creation dialog") {
        every {
          anyConstructed<AddJobsFilterDialog>().showAndGet()
        } answers {
          didTriggerAddJobsFilterDialogShowAndGet = true
          false
        }

        val compatibleWS = mockk<JesWorkingSet> {
          every { name } returns "test1"
          every { connectionConfig } returns connectionConfigMock
        }

        every { fsTreeStructureMock.findByPredicate(any()) } returns listOf(
          mockk<JesWsNode> {
            every { unit } returns compatibleWS
            every { name } returns "test1"
            every { children } returns mutableListOf(
              mockk<JesFilterNode> {
                every { value } returns mockk<JobsFilter> {
                  every { jobId } returns "UNIQUE"
                }
              }
            )
          },
          mockk<JesWsNode> {
            every { unit } returns mockk<JesWorkingSet> {
              every { connectionConfig } returns connectionConfigMock
            }
            every { name } returns null
          },
          mockk<JesWsNode> {
            every { unit } returns mockk<JesWorkingSet> {
              every { connectionConfig } returns connectionConfigMock
            }
            every { name } returns "test2"
            every { children } returns mutableListOf(
              mockk<JesFilterNode> {
                every { value } returns mockk<JobsFilter> {
                  every { jobId } returns "TEST"
                }
              }
            )
          }
        )

        runInEdtAndWait {
          goToJobAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerJesWsDialogShowAndGet shouldBe false
          didTriggerAddJobsFilterDialogShowAndGet shouldBe true
          didTriggerShowNotification shouldBe false
          didTriggerSetSelectedContent shouldBe false
          didTriggerAddMask shouldBe false
          didTriggerSelectTree shouldBe false
          didTriggerExpandTree shouldBe false
          addCallCount shouldBe 0
        }
      }

      should("perform Go To Job action, not creating a jobs filter cause there is already existing jobs filter with the same job ID exists") {
        every { fsTreeStructureMock.findByPredicate(any()) } returns listOf(
          mockk<JesWsNode> {
            every { unit } returns mockk<JesWorkingSet> {
              every { connectionConfig } returns connectionConfigMock
            }
            every { name } returns "test2"
            every { children } returns mutableListOf(
              mockk<JesFilterNode> {
                every { value } returns mockk<JobsFilter> {
                  every { jobId } returns "TEST"
                }
              }
            )
          }
        )

        runInEdtAndWait {
          goToJobAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerJesWsDialogShowAndGet shouldBe false
          didTriggerAddJobsFilterDialogShowAndGet shouldBe false
          didTriggerShowNotification shouldBe true
          didTriggerSetSelectedContent shouldBe true
          didTriggerAddMask shouldBe false
          didTriggerSelectTree shouldBe false
          didTriggerExpandTree shouldBe false
          addCallCount shouldBe 0
        }
      }

      should("not perform Go To Job action cause there is no JES Explorer view") {
        every { jesExplorerContentProviderMock.getExplorerView(any()) } returns null

        runInEdtAndWait {
          goToJobAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerJesWsDialogShowAndGet shouldBe false
          didTriggerAddJobsFilterDialogShowAndGet shouldBe false
          didTriggerShowNotification shouldBe false
          didTriggerSetSelectedContent shouldBe false
          didTriggerAddMask shouldBe false
          didTriggerSelectTree shouldBe false
          didTriggerExpandTree shouldBe false
          addCallCount shouldBe 0
        }
      }

      should("not perform Go To Job action cause there is no project open") {
        every { eventMock.project } returns null

        runInEdtAndWait {
          goToJobAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerJesWsDialogShowAndGet shouldBe false
          didTriggerAddJobsFilterDialogShowAndGet shouldBe false
          didTriggerShowNotification shouldBe false
          didTriggerSetSelectedContent shouldBe false
          didTriggerAddMask shouldBe false
          didTriggerSelectTree shouldBe false
          didTriggerExpandTree shouldBe false
          addCallCount shouldBe 0
        }
      }

      should("not perform Go To Job action cause there is no job ID for the job in the console view") {
        every { jobsLogViewMock.jobLogInfo } returns mockk {
          every { jobId } returns null
        }

        runInEdtAndWait {
          goToJobAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerJesWsDialogShowAndGet shouldBe false
          didTriggerAddJobsFilterDialogShowAndGet shouldBe false
          didTriggerShowNotification shouldBe false
          didTriggerSetSelectedContent shouldBe false
          didTriggerAddMask shouldBe false
          didTriggerSelectTree shouldBe false
          didTriggerExpandTree shouldBe false
          addCallCount shouldBe 0
        }
      }

      should("not perform Go To Job action cause there is no job console view") {
        every { eventMock.getData(JOBS_LOG_VIEW) } returns null

        runInEdtAndWait {
          goToJobAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerJesWsDialogShowAndGet shouldBe false
          didTriggerAddJobsFilterDialogShowAndGet shouldBe false
          didTriggerShowNotification shouldBe false
          didTriggerSetSelectedContent shouldBe false
          didTriggerAddMask shouldBe false
          didTriggerSelectTree shouldBe false
          didTriggerExpandTree shouldBe false
          addCallCount shouldBe 0
        }
      }
    }

    context("update") {
      should("show the Go To Job action") {
        goToJobAction.update(eventMock)

        assertSoftly {
          isEnabledNewValue shouldBe true
          isVisibleNewValue shouldBe true
        }
      }

      should("show the Go To Job action as disabled if there is no job status") {
        every { logFetcherMock.getCachedJobStatus() } returns mockk {
          every { status } returns null
        }

        goToJobAction.update(eventMock)

        assertSoftly {
          isEnabledNewValue shouldBe false
          isVisibleNewValue shouldBe true
        }
      }

      should("show the Go To Job action as disabled if there is no cached job status holder") {
        every { logFetcherMock.getCachedJobStatus() } returns null

        goToJobAction.update(eventMock)

        assertSoftly {
          isEnabledNewValue shouldBe false
          isVisibleNewValue shouldBe true
        }
      }

      should("not show the Go To Job action cause there is no job console view") {
        every { eventMock.getData(JOBS_LOG_VIEW) } returns null

        goToJobAction.update(eventMock)

        assertSoftly {
          isEnabledNewValue shouldBe false
          isVisibleNewValue shouldBe false
        }
      }
    }
  }
})
