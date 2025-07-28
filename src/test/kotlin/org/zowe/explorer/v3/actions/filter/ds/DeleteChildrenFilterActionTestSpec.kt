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
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.showYesNoDialog
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
import javax.swing.Icon
import kotlin.reflect.KFunction

class DeleteChildrenFilterActionTestSpec : AppInitShouldSpec("v3/actions/filter/ds/DeleteChildrenFilterAction", {
  context("all functions") {
    var isShowYesNoDialogCalled = false
    var didDeleteFilter = false
    var didCallCleanCache = false
    var isEnabledAndVisibleNewValue: Boolean? = null

    val oldFilter = "TESTOLD"
    val newFilter = "TESTNEW"
    val projectMock = ProjectManager.getInstance().defaultProject

    val fileExplorerView = mockk<FileExplorerView>()
    val eventMock = mockk<AnActionEvent>()

    mockkStatic(::checkForSync)
    mockkConstructor(FilterChildrenDialog::class)

    val showYesNoDialogMock: (String, String, Project?, String, String, Icon?) -> Boolean = ::showYesNoDialog
    mockkStatic(showYesNoDialogMock as KFunction<*>)

    val uiComponentManager = UIComponentManager.getService()
    every { uiComponentManager.getExplorerContentProvider(any<Class<FileExplorer>>()) } returns mockk()

    val deleteChildrenFilterAction = DeleteChildrenFilterAction()

    beforeEach {
      isShowYesNoDialogCalled = false
      didDeleteFilter = false
      didCallCleanCache = false
      isEnabledAndVisibleNewValue = null

      every { fileExplorerView.mySelectedNodesData } returns listOf()

      every { eventMock.project } returns projectMock
      every { eventMock.getData(EXPLORER_VIEW) } returns fileExplorerView
      every { eventMock.presentation } returns mockk()

      every { checkForSync(any()) } returns false
      every { anyConstructed<FilterChildrenDialog>().waitForUserInput() } returns newFilter

      every {
        showYesNoDialogMock(any<String>(), any<String>(), any(), any<String>(), any<String>(), any())
      } answers {
        isShowYesNoDialogCalled = true
        true
      }
    }

    context("actionPerformed") {
      should("perform filter delete") {
        every { fileExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<LibraryNode>(relaxUnitFun = true) {
              every {
                cleanCache(any(), any(), any(), any())
              } answers {
                didCallCleanCache = true
              }
              every { savedFilter } returns oldFilter
              every {
                savedFilter = any()
              } answers {
                didDeleteFilter = firstArg<String>() == ""
              }
            }
          }
        )

        runInEdtAndWait {
          deleteChildrenFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          isShowYesNoDialogCalled shouldBe true
          didDeleteFilter shouldBe true
          didCallCleanCache shouldBe true
        }
      }

      should("not perform filter delete cause the delete dialog is canceled") {
        every { fileExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<LibraryNode>(relaxUnitFun = true) {
              every {
                cleanCache(any(), any(), any(), any())
              } answers {
                didCallCleanCache = true
              }
              every { savedFilter } returns oldFilter
              every {
                savedFilter = any()
              } answers {
                didDeleteFilter = firstArg<String>() == ""
              }
            }
          }
        )

        every {
          showYesNoDialogMock(any<String>(), any<String>(), any(), any<String>(), any<String>(), any())
        } answers {
          isShowYesNoDialogCalled = true
          false
        }

        runInEdtAndWait {
          deleteChildrenFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          isShowYesNoDialogCalled shouldBe true
          didDeleteFilter shouldBe false
          didCallCleanCache shouldBe false
        }
      }

      should("not perform filter delete cause a project is not found") {
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
              every {
                savedFilter = any()
              } answers {
                didDeleteFilter = firstArg<String>() == ""
              }
            }
          }
        )

        runInEdtAndWait {
          deleteChildrenFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          isShowYesNoDialogCalled shouldBe false
          didDeleteFilter shouldBe false
          didCallCleanCache shouldBe false
        }
      }

      should("not perform filter delete cause the node is not compatible") {
        every { fileExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<UssDirNode>(relaxUnitFun = true) {
              every {
                cleanCache(any(), any(), any(), any())
              } answers {
                didCallCleanCache = true
              }
              every { savedFilter } returns oldFilter
              every {
                savedFilter = any()
              } answers {
                didDeleteFilter = firstArg<String>() == ""
              }
            }
          }
        )

        runInEdtAndWait {
          deleteChildrenFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          isShowYesNoDialogCalled shouldBe false
          didDeleteFilter shouldBe false
          didCallCleanCache shouldBe false
        }
      }

      should("not perform filter delete cause there is a sync in progress") {
        every { checkForSync(any()) } returns true

        every { fileExplorerView.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<LibraryNode>(relaxUnitFun = true) {
              every {
                cleanCache(any(), any(), any(), any())
              } answers {
                didCallCleanCache = true
              }
              every { savedFilter } returns oldFilter
              every {
                savedFilter = any()
              } answers {
                didDeleteFilter = firstArg<String>() == ""
              }
            }
          }
        )

        runInEdtAndWait {
          deleteChildrenFilterAction.actionPerformed(eventMock)
        }

        assertSoftly {
          isShowYesNoDialogCalled shouldBe false
          didDeleteFilter shouldBe false
          didCallCleanCache shouldBe false
        }
      }
    }

    context("update") {
      should("show 'Delete Filter' on a filter node") {
        every { eventMock.presentation } returns mockk {
          every {
            isEnabledAndVisible = any()
          } answers {
            isEnabledAndVisibleNewValue = firstArg<Boolean>()
          }
        }

        every { fileExplorerView.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk<FilterChildrenNode<ConnectionConfig>>() }
        )

        deleteChildrenFilterAction.update(eventMock)

        assertSoftly {
          isEnabledAndVisibleNewValue shouldBe true
        }
      }

      should("not show 'Delete Filter' on a non-filter node") {
        every { eventMock.presentation } returns mockk {
          every {
            isEnabledAndVisible = any()
          } answers {
            isEnabledAndVisibleNewValue = firstArg<Boolean>()
          }
        }

        every { fileExplorerView.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk<UssDirNode>() }
        )

        deleteChildrenFilterAction.update(eventMock)

        assertSoftly {
          isEnabledAndVisibleNewValue shouldBe false
        }
      }
    }
  }
})
