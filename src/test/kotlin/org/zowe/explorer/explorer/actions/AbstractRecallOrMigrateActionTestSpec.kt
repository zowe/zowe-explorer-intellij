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
import io.mockk.mockkStatic
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.content.synchronizer.checkFileForSync
import org.zowe.explorer.dataops.operations.migration.MigrateOperation
import org.zowe.explorer.dataops.operations.migration.MigrateOperationParams
import org.zowe.explorer.dataops.operations.migration.RecallOperation
import org.zowe.explorer.dataops.operations.migration.RecallOperationParams
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.JesWorkingSet
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.FileLikeDatasetNode
import org.zowe.explorer.explorer.ui.JesFilterNode
import org.zowe.explorer.explorer.ui.LibraryNode
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.AppInitShouldSpec

class AbstractRecallOrMigrateActionTestSpec : AppInitShouldSpec("explorer/actions/AbstractRecallOrMigrateAction", {
  context("all functions") {
    var didPerformRecallOperation = false
    var didPerformMigrateOperation = false
    var didCleanCache = false
    var didNotifyError = false
    var isEnabledAndVisibleNewValue: Boolean? = null

    val fileExplorerViewMock = mockk<FileExplorerView>()
    val eventMock = mockk<AnActionEvent> {
      every { presentation } returns mockk {
        every {
          isEnabledAndVisible = any()
        } answers {
          isEnabledAndVisibleNewValue = firstArg<Boolean>()
        }
      }
      every { project } returns mockk()
    }

    mockkStatic(::checkFileForSync)

    val dataOpsManager = DataOpsManager.getService()
    val notificationsService = NotificationsService.getService()
    every {
      notificationsService
        .notifyError(any(), any(), any(), any(), any())
    } answers {
      didNotifyError = true
    }

    val migrateAction = MigrateAction()
    val recallAction = RecallAction()

    beforeEach {
      didPerformRecallOperation = false
      didPerformMigrateOperation = false
      didCleanCache = false
      didNotifyError = false
      isEnabledAndVisibleNewValue = null

      every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
        mockk {
          every { file } returns mockk()
          every { node } returns mockk<FileLikeDatasetNode> {
            every { parent } returns mockk<LibraryNode> {
              every {
                cleanCache(any(), any(), any(), any())
              } answers {
                didCleanCache = true
              }
            }
            every { virtualFile } returns mockk()
            every { unit } returns mockk<FilesWorkingSet> {
              every { connectionConfig } returns mockk()
            }
          }
          every { attributes } returns mockk<RemoteDatasetAttributes> {
            every { hasDsOrg } returns true
          }
        }
      )
      every { eventMock.getData(EXPLORER_VIEW) } returns fileExplorerViewMock

      every { checkFileForSync(any(), any(), any()) } returns false

      every {
        dataOpsManager.performOperation(any<Operation<Any>>(), any())
      } answers {
        when (val operation = firstArg<Operation<*>>()) {
          is RecallOperation -> {
            didPerformRecallOperation = true
            mockk<RecallOperationParams>()
          }
          is MigrateOperation -> {
            didPerformMigrateOperation = true
            mockk<MigrateOperationParams>()
          }
          else -> fail("Unexpected operation: $operation")
        }
      }
    }

    context("action performed") {
      should("perform migrate action on a dataset") {
        migrateAction.actionPerformed(eventMock)

        assertSoftly {
          didPerformRecallOperation shouldBe false
          didPerformMigrateOperation shouldBe true
          didCleanCache shouldBe true
          didNotifyError shouldBe false
        }
      }

      should("perform recall action on a dataset") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { file } returns mockk()
            every { node } returns mockk<FileLikeDatasetNode> {
              every { parent } returns mockk<LibraryNode> {
                every {
                  cleanCache(any(), any(), any(), any())
                } answers {
                  didCleanCache = true
                }
              }
              every { virtualFile } returns mockk()
              every { unit } returns mockk<FilesWorkingSet> {
                every { connectionConfig } returns mockk()
              }
            }
          }
        )

        recallAction.actionPerformed(eventMock)

        assertSoftly {
          didPerformRecallOperation shouldBe true
          didPerformMigrateOperation shouldBe false
          didCleanCache shouldBe true
          didNotifyError shouldBe false
        }
      }

      should("fail to perform migrate action on a dataset due to a remote error") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { file } returns mockk()
            every { node } returns mockk<FileLikeDatasetNode> {
              every { parent } returns null
              every { virtualFile } returns mockk()
              every { unit } returns mockk<FilesWorkingSet> {
                every { connectionConfig } returns mockk()
              }
            }
          }
        )

        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any())
        } answers {
          when (val operation = firstArg<Operation<*>>()) {
            is MigrateOperation -> {
              didPerformMigrateOperation = true
              throw Throwable("Test exception")
            }
            else -> fail("Unexpected operation: $operation")
          }
        }

        migrateAction.actionPerformed(eventMock)

        assertSoftly {
          didPerformRecallOperation shouldBe false
          didPerformMigrateOperation shouldBe true
          didCleanCache shouldBe false
          didNotifyError shouldBe true
        }
      }

      should("not perform migrate action on a dataset cause the File Explorer view is not initialized yet") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        migrateAction.actionPerformed(eventMock)

        assertSoftly {
          didPerformRecallOperation shouldBe false
          didPerformMigrateOperation shouldBe false
          didCleanCache shouldBe false
          didNotifyError shouldBe false
        }
      }
    }

    context("getRequestDataForNode") {
      should("return null when the node does not have a related connection config") {
        val node = mockk<FileLikeDatasetNode> {
          every { virtualFile } returns mockk()
          every { unit } returns mockk<FilesWorkingSet> {
            every { connectionConfig } returns null
          }
        }

        val result = getRequestDataForNode(node)

        assertSoftly { result shouldBe null }
      }

      should("return null when the node does not belong to a Files Working set") {
        val node = mockk<JesFilterNode> {
          every { unit } returns mockk<JesWorkingSet>()
        }

        val result = getRequestDataForNode(node)

        assertSoftly { result shouldBe null }
      }
    }

    context("MigrateAction.update") {
      should("show the action") {
        migrateAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe true }
      }

      should("not show the action cause there is a migrated node selected") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { attributes } returns mockk<RemoteDatasetAttributes> {
              every { hasDsOrg } returns false
            }
          }
        )

        migrateAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe false }
      }

      should("not show the action cause there is a wrong node selected") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { attributes } returns mockk()
          }
        )

        migrateAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe false }
      }

      should("not show the action cause the File Explorer view is not initialized yet") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        migrateAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe false }
      }
    }

    context("RecallAction.update") {
      should("show the action") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { attributes } returns mockk<RemoteDatasetAttributes> {
              every { isMigrated } returns true
            }
          }
        )

        recallAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe true }
      }

      should("not show the action cause the selected dataset is not migrated") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { attributes } returns mockk<RemoteDatasetAttributes> {
              every { isMigrated } returns false
            }
          }
        )

        recallAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe false }
      }

      should("not show the action cause a wrong node is selected") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk { every { attributes } returns mockk() }
        )

        recallAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe false }
      }

      should("not show the action cause the File Explorer view is not initialized yet") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        recallAction.update(eventMock)

        assertSoftly { isEnabledAndVisibleNewValue shouldBe false }
      }
    }
  }
})
