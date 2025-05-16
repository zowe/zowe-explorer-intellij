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
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.config.ws.ui.files.FilesWorkingSetDialog
import org.zowe.explorer.config.ws.ui.jes.JesWsDialog
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.FilesWorkingSetNode
import org.zowe.explorer.explorer.ui.JesExplorerView
import org.zowe.explorer.explorer.ui.JesWsNode
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.runInEdtAndWait
import java.util.Optional

class EditWorkingSetActionTestSpec : AppInitShouldSpec("explorer/actions/EditWorkingSetAction", {
  context("all functions") {
    var getByUniqueKeyCallCount = 0
    var updateCallCount = 0
    var didOpenDialog = false
    var didChangeIsEnabledAndVisible = false
    var isEnabledAndVisibleNewValue: Boolean? = null

    val fileExplorerViewMock = mockk<FileExplorerView>()
    val jesExplorerViewMock = mockk<JesExplorerView>()
    val eventMock = mockk<AnActionEvent> {
      every { presentation } returns mockk {
        every {
          isEnabledAndVisible = any()
        } answers {
          didChangeIsEnabledAndVisible = true
          isEnabledAndVisibleNewValue = firstArg<Boolean>()
        }
      }
    }

    val editFilesWorkingSetAction = EditFilesWorkingSetAction()
    val editJesWorkingSetAction = EditJesWorkingSetAction()

    mockkConstructor(FilesWorkingSetDialog::class)
    mockkConstructor(JesWsDialog::class)

    val configServiceCrudable = mockk<Crudable> {
      every {
        getAll(ConnectionConfig::class.java)
      } answers {
        listOf(
          mockk<ConnectionConfig> { every { uuid } returns "test_uuid" }
        )
          .stream()
      }
      every {
        getByUniqueKey(ConnectionConfig::class.java, any<String>())
      } answers {
        Optional.ofNullable(null)
      }
      every {
        update(any())
      } answers {
        updateCallCount++
        mockk()
      }
    }
    val configService = ConfigService.getService()
    every { configService.crudable } returns configServiceCrudable

    beforeEach {
      getByUniqueKeyCallCount = 0
      updateCallCount = 0
      didOpenDialog = false
      didChangeIsEnabledAndVisible = false
      isEnabledAndVisibleNewValue = null

      every { eventMock.getData(EXPLORER_VIEW) } returns fileExplorerViewMock
      every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
        mockk {
          every { node } returns mockk<FilesWorkingSetNode> {
            every { value } returns mockk {
              every { uuid } returns "test_uuid"
            }
          }
        },
        mockk {
          every { node } returns mockk()
        }
      )

      every {
        anyConstructed<FilesWorkingSetDialog>().showAndGet()
      } answers {
        didOpenDialog = true
        true
      }
      every {
        anyConstructed<JesWsDialog>().showAndGet()
      } answers {
        didOpenDialog = true
        true
      }

      every {
        configServiceCrudable.getByUniqueKey(FilesWorkingSetConfig::class.java, any<String>())
      } answers {
        getByUniqueKeyCallCount++
        Optional.of(mockk())
      }
      every {
        configServiceCrudable.getByUniqueKey(JesWorkingSetConfig::class.java, any<String>())
      } answers {
        getByUniqueKeyCallCount++
        Optional.of(mockk())
      }
    }

    context("actionPerformed") {
      should("perform Edit Files Working set when the first selected node is a Files Working set") {
        runInEdtAndWait {
          editFilesWorkingSetAction.actionPerformed(eventMock)
        }

        assertSoftly {
          getByUniqueKeyCallCount shouldBe 1
          didOpenDialog shouldBe true
          updateCallCount shouldBe 1
        }
      }

      should("perform Edit JES Working set when the first selected node is a JES Working set") {
        every { eventMock.getData(EXPLORER_VIEW) } returns jesExplorerViewMock
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<JesWsNode> {
              every { value } returns mockk {
                every { uuid } returns "test_uuid"
              }
            }
          }
        )

        runInEdtAndWait {
          editJesWorkingSetAction.actionPerformed(eventMock)
        }

        assertSoftly {
          getByUniqueKeyCallCount shouldBe 1
          didOpenDialog shouldBe true
          updateCallCount shouldBe 1
        }
      }

      should("perform Edit Files Working set without update cause a user cancelled the dialog") {
        every {
          anyConstructed<FilesWorkingSetDialog>().showAndGet()
        } answers {
          didOpenDialog = true
          false
        }

        runInEdtAndWait {
          editFilesWorkingSetAction.actionPerformed(eventMock)
        }

        assertSoftly {
          getByUniqueKeyCallCount shouldBe 1
          didOpenDialog shouldBe true
          updateCallCount shouldBe 0
        }
      }

      should("not perform Edit Files Working set cause there is no Files Working set config found for the selected node") {
        every {
          configServiceCrudable.getByUniqueKey(FilesWorkingSetConfig::class.java, any<String>())
        } answers {
          getByUniqueKeyCallCount++
          Optional.ofNullable(null)
        }

        runInEdtAndWait {
          editFilesWorkingSetAction.actionPerformed(eventMock)
        }

        assertSoftly {
          getByUniqueKeyCallCount shouldBe 1
          didOpenDialog shouldBe false
          updateCallCount shouldBe 0
        }
      }

      should("not perform Edit Files Working set cause there is no appropriate node selected") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk() }
        )

        runInEdtAndWait {
          editFilesWorkingSetAction.actionPerformed(eventMock)
        }

        assertSoftly {
          getByUniqueKeyCallCount shouldBe 0
          didOpenDialog shouldBe false
          updateCallCount shouldBe 0
        }
      }

      should("not perform Edit Files Working set cause File Explorer view is not initialized") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        runInEdtAndWait {
          editFilesWorkingSetAction.actionPerformed(eventMock)
        }

        assertSoftly {
          getByUniqueKeyCallCount shouldBe 0
          didOpenDialog shouldBe false
          updateCallCount shouldBe 0
        }
      }
    }

    context("update") {
      should("show the Edit action for a Files Working set node") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk<FilesWorkingSetNode>() }
        )

        editFilesWorkingSetAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe true
        }
      }

      should("not show the Edit action cause a different node is selected") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk() }
        )

        editFilesWorkingSetAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
        }
      }

      should("not show the Edit action cause File Explorer view is not initialized") {
        editFilesWorkingSetAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
        }
      }

      should("not show the Edit action cause there is more than one node is selected") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        editFilesWorkingSetAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
        }
      }
    }
  }
})
