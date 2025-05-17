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
import com.intellij.openapi.vfs.VirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.zowe.explorer.common.ui.cleanInvalidateOnExpand
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.WorkingSetConfig
import org.zowe.explorer.dataops.content.synchronizer.checkFileForSync
import org.zowe.explorer.explorer.WorkingSet
import org.zowe.explorer.explorer.ui.DSMaskNode
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.ExplorerTreeView
import org.zowe.explorer.explorer.ui.FetchNode
import org.zowe.explorer.explorer.ui.JobNode
import org.zowe.explorer.explorer.ui.LibraryNode
import org.zowe.explorer.explorer.ui.LoadMoreNode
import org.zowe.explorer.explorer.ui.WorkingSetNode
import org.zowe.explorer.testutils.AppInitShouldSpec
import kotlin.reflect.KFunction

class RefreshNodeActionTestSpec : AppInitShouldSpec("explorer/actions/RefreshNodeAction", {
  context("all functions") {
    var didTriggerCleanInvalidateOnExpand = false
    var didTriggerInvalidateNodes = false
    var didTriggerInvalidate = false
    var isEnabledAndVisibleNewValue = false
    var cleanCacheTriggerCount = 0
    var checkFileForSyncTriggerCount = 0

    val explorerView = mockk<ExplorerTreeView<ConnectionConfig, WorkingSet<ConnectionConfig, Any>, WorkingSetConfig>> {
      every { getNodesByQuery(any()) } returns listOf(mockk())
      every {
        invalidateNodes(any())
      } answers {
        didTriggerInvalidateNodes = true
        mockk()
      }
      every { myFsTreeStructure } returns mockk {
        every { findByValue(any()) } returns listOf(mockk())
      }
      every { myStructure } returns mockk {
        every {
          invalidate(any<Object>(), any())
        } answers {
          didTriggerInvalidate = true
          mockk()
        }
      }
    }
    val eventMock = mockk<AnActionEvent> {
      every { project } returns mockk()
      every {
        presentation
      } returns mockk {
        every {
          isEnabledAndVisible = any()
        } answers {
          isEnabledAndVisibleNewValue = firstArg<Boolean>()
        }
      }
    }

    mockkStatic(::checkFileForSync)

    val cleanInvalidateOnExpandMock: (
      FetchNode,
      ExplorerTreeView<ConnectionConfig, WorkingSet<ConnectionConfig, Any>, WorkingSetConfig>
    ) -> Unit = ::cleanInvalidateOnExpand
    mockkStatic(cleanInvalidateOnExpandMock as KFunction<*>)
    every {
      cleanInvalidateOnExpandMock(any(), any())
    } answers {
      didTriggerCleanInvalidateOnExpand = true
    }

    var refreshNodeAction = RefreshNodeAction()

    beforeEach {
      didTriggerCleanInvalidateOnExpand = false
      didTriggerInvalidateNodes = false
      didTriggerInvalidate = false
      isEnabledAndVisibleNewValue = false
      cleanCacheTriggerCount = 0
      checkFileForSyncTriggerCount = 0

      every { explorerView.mySelectedNodesData } returns listOf(
        mockk {
          every { node } returns mockk<LibraryNode> {
            every { query } returns mockk()
            every { virtualFile } returns mockk()
            every {
              cleanCache(any(), any(), any(), any())
            } answers {
              cleanCacheTriggerCount++
            }
          }
        },
        mockk {
          every { node } returns mockk<WorkingSetNode<ConnectionConfig, Any>> {
            every { value } returns mockk()
            every { cachedChildren } returns mutableListOf(
              mockk<DSMaskNode> {
                every { virtualFile } returns null
                every { children } returns mutableListOf(
                  mockk<FetchNode> {
                    every { value } returns mockk<VirtualFile>()
                  },
                  mockk<LoadMoreNode<ConnectionConfig>> {
                    every { value } returns null
                  }
                )
                every {
                  cleanCache(any(), any(), any(), any())
                } answers {
                  cleanCacheTriggerCount++
                }
              },
              mockk()
            )
          }
        },
        mockk { every { node } returns mockk() }
      )
      every { eventMock.getData(EXPLORER_VIEW) } returns explorerView

      every {
        checkFileForSync(any(), any(), any())
      } answers {
        checkFileForSyncTriggerCount++
        false
      }
    }

    context("actionPerformed") {
      should("perform Refresh action for a working set and a PDS/E dataset") {
        refreshNodeAction.actionPerformed(eventMock)

        assertSoftly {
          didTriggerCleanInvalidateOnExpand shouldBe true
          didTriggerInvalidateNodes shouldBe true
          didTriggerInvalidate shouldBe true
          cleanCacheTriggerCount shouldBe 2
          checkFileForSyncTriggerCount shouldBe 2
        }
      }

      should("not perform Refresh action for a JES job without a defined query") {
        every { explorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<JobNode> {
              every { query } returns null
              every { virtualFile } returns mockk()
              every {
                cleanCache(any(), any(), any(), any())
              } answers {
                cleanCacheTriggerCount++
              }
            }
          }
        )
        refreshNodeAction.actionPerformed(eventMock)

        assertSoftly {
          didTriggerCleanInvalidateOnExpand shouldBe true
          didTriggerInvalidateNodes shouldBe false
          didTriggerInvalidate shouldBe false
          cleanCacheTriggerCount shouldBe 1
          checkFileForSyncTriggerCount shouldBe 1
        }
      }

      should("not perform Refresh action for a PDS/E dataset that is being synchronized") {
        every { explorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<LibraryNode> {
              every { query } returns null
              every { virtualFile } returns mockk()
              every {
                cleanCache(any(), any(), any(), any())
              } answers {
                cleanCacheTriggerCount++
              }
            }
          }
        )

        every {
          checkFileForSync(any(), any(), any())
        } answers {
          checkFileForSyncTriggerCount++
          true
        }

        refreshNodeAction.actionPerformed(eventMock)

        assertSoftly {
          didTriggerCleanInvalidateOnExpand shouldBe false
          didTriggerInvalidateNodes shouldBe false
          didTriggerInvalidate shouldBe false
          cleanCacheTriggerCount shouldBe 0
          checkFileForSyncTriggerCount shouldBe 1
        }
      }

      should("not perform Refresh action for a JES job without a virtual file") {
        every { explorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<JobNode> {
              every { query } returns mockk()
              every { virtualFile } returns null
              every {
                cleanCache(any(), any(), any(), any())
              } answers {
                cleanCacheTriggerCount++
              }
            }
          }
        )
        refreshNodeAction.actionPerformed(eventMock)

        assertSoftly {
          didTriggerCleanInvalidateOnExpand shouldBe true
          didTriggerInvalidateNodes shouldBe true
          didTriggerInvalidate shouldBe false
          cleanCacheTriggerCount shouldBe 1
          checkFileForSyncTriggerCount shouldBe 0
        }
      }

      should("not perform Refresh action when there is no explorer view initialized") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        refreshNodeAction.actionPerformed(eventMock)

        assertSoftly {
          didTriggerCleanInvalidateOnExpand shouldBe false
          didTriggerInvalidateNodes shouldBe false
          didTriggerInvalidate shouldBe false
          cleanCacheTriggerCount shouldBe 0
          checkFileForSyncTriggerCount shouldBe 0
        }
      }
    }

    context("update") {
      should("show the Refresh action") {
        refreshNodeAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe true }
      }

      should("not show the Refresh action for non-refreshable nodes") {
        every { explorerView.mySelectedNodesData } returns listOf(mockk { every { node } returns mockk() })

        refreshNodeAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe false }
      }

      should("not show the Refresh action when there is no explorer view initialized") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        refreshNodeAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe false }
      }
    }
  }
})