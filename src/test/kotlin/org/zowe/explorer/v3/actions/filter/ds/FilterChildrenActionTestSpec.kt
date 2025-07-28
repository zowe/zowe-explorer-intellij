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

package org.zowe.explorer.v3.actions.filter.ds

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.ProjectManager
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.content.synchronizer.checkForSync
import org.zowe.explorer.explorer.FileExplorer
import org.zowe.explorer.explorer.UIComponentManager
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.LibraryNode
import org.zowe.explorer.explorer.ui.UssDirNode
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.runInEdtAndWait

class FilterChildrenActionTestSpec : AppInitShouldSpec("v3/actions/filter/ds/FilterChildrenAction", {
  context("all functions") {
    var didCallCleanCache = false
    var isEnabledAndVisibleNewValue: Boolean? = null

    val oldFilter = "TESTOLD"
    val newFilter = "TESTNEW"
    val projectMock = ProjectManager.getInstance().defaultProject

    val fileExplorerView = mockk<FileExplorerView>()
    val eventMock = mockk<AnActionEvent>()

    mockkStatic(::checkForSync)
    mockkConstructor(FilterChildrenDialog::class)

    val uiComponentManager = UIComponentManager.getService()
    every { uiComponentManager.getExplorerContentProvider(any<Class<FileExplorer>>()) } returns mockk()

    val filterChildrenAction = FilterChildrenAction()

    beforeEach {
      isEnabledAndVisibleNewValue = null
      didCallCleanCache = false

      every { fileExplorerView.mySelectedNodesData } returns listOf()

      every { eventMock.project } returns projectMock
      every { eventMock.getData(EXPLORER_VIEW) } returns fileExplorerView
      every { eventMock.presentation } returns mockk()

      every { checkForSync(any()) } returns false
      every { anyConstructed<FilterChildrenDialog>().waitForUserInput() } returns newFilter
    }

    context("actionPerformed") {
      should("perform filter create") {
        every { fileExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<LibraryNode>(relaxUnitFun = true) {
              every {
                cleanCache(any(), any(), any(), any())
              } answers {
                didCallCleanCache = true
              }
              every { savedFilter } returns oldFilter
            }
          }
        )

        runInEdtAndWait {
          filterChildrenAction.actionPerformed(eventMock)
        }

        assertSoftly { didCallCleanCache shouldBe true }
      }

      should("perform filter change") {
        every { fileExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<FilterChildrenNode<ConnectionConfig>> {
              every { parent } returns mockk<LibraryNode>(relaxUnitFun = true) {
                every {
                  cleanCache(any(), any(), any(), any())
                } answers {
                  didCallCleanCache = true
                }
                every { savedFilter } returns oldFilter
              }
            }
          }
        )

        runInEdtAndWait {
          filterChildrenAction.actionPerformed(eventMock)
        }

        assertSoftly { didCallCleanCache shouldBe true }
      }

      should("not perform filter create cause a project is not found") {
        every { eventMock.project } returns null

        every { fileExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<LibraryNode>(relaxUnitFun = true) {
              every {
                cleanCache(any(), any(), any(), any())
              } answers {
                didCallCleanCache = true
              }
              every { savedFilter } returns oldFilter
            }
          }
        )

        runInEdtAndWait {
          filterChildrenAction.actionPerformed(eventMock)
        }

        assertSoftly { didCallCleanCache shouldBe false }
      }

      should("not perform filter create cause the node is not compatible") {
        every { fileExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<UssDirNode> {
              every {
                cleanCache(any(), any(), any(), any())
              } answers {
                didCallCleanCache = true
              }
            }
          }
        )

        runInEdtAndWait {
          filterChildrenAction.actionPerformed(eventMock)
        }

        assertSoftly { didCallCleanCache shouldBe false }
      }

      should("not perform filter create cause there is a sync in progress") {
        every { checkForSync(any()) } returns true

        every { fileExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<LibraryNode> {
              every {
                cleanCache(any(), any(), any(), any())
              } answers {
                didCallCleanCache = true
              }
            }
          }
        )

        runInEdtAndWait {
          filterChildrenAction.actionPerformed(eventMock)
        }

        assertSoftly { didCallCleanCache shouldBe false }
      }
    }

    context("update") {
      should("show 'Apply Filter' when a filter is not created yet") {
        var isPresentationTextCorrect = false

        every { eventMock.presentation } returns mockk {
          every {
            isEnabledAndVisible = any()
          } answers {
            isEnabledAndVisibleNewValue = firstArg<Boolean>()
          }
          every {
            text = any()
          } answers {
            isPresentationTextCorrect = firstArg<String>() == "Apply Filter"
          }
        }

        every { fileExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<LibraryNode> {
              every { savedFilter } returns ""
            }
          }
        )

        filterChildrenAction.update(eventMock)

        assertSoftly {
          isEnabledAndVisibleNewValue shouldBe true
          isPresentationTextCorrect shouldBe true
        }
      }

      should("show 'Edit Filter' when a filter is not created yet") {
        var isPresentationTextCorrect = false

        every { eventMock.presentation } returns mockk {
          every {
            isEnabledAndVisible = any()
          } answers {
            isEnabledAndVisibleNewValue = firstArg<Boolean>()
          }
          every {
            text = any()
          } answers {
            isPresentationTextCorrect = firstArg<String>() == "Edit Filter"
          }
        }

        every { fileExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<LibraryNode> {
              every { savedFilter } returns oldFilter
            }
          }
        )

        filterChildrenAction.update(eventMock)

        assertSoftly {
          isEnabledAndVisibleNewValue shouldBe true
          isPresentationTextCorrect shouldBe true
        }
      }

      should("show the action for filter cause the node is not compatible") {
        every { eventMock.presentation } returns mockk(relaxUnitFun = true) {
          every {
            isEnabledAndVisible = any()
          } answers {
            isEnabledAndVisibleNewValue = firstArg<Boolean>()
          }
        }

        every { fileExplorerView.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk<UssDirNode>() }
        )

        filterChildrenAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe false }
      }
    }
  }
})
