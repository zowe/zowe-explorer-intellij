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
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.ProjectManager
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.content.synchronizer.DocumentedSyncProvider
import org.zowe.explorer.dataops.operations.jobs.GetJclRecordsOperation
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.JesExplorerView
import org.zowe.explorer.explorer.ui.JobNode
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.vfs.MFVirtualFile

class EditJclActionTestSpec : AppInitShouldSpec("explorer/actions/EditJclAction", {
  context("all functions") {
    var didTriggerGetJclRecordsOperation = false
    var didPutInitialContent = false
    var didRetrieveCurrentContent = false
    var didLoadNewContent = false
    var didNotifyError = false
    var didTriggerNavigate = false
    var didChangeIsEnabledAndVisible = false
    var didChangeIsVisible = false
    var isEnabledAndVisibleNewValue: Boolean? = null
    var isVisibleNewValue: Boolean? = null

    val jclNodeVirtualFile = mockk<MFVirtualFile> {
      every { createChildData(any(), any()) } returns mockk<MFVirtualFile>()
    }
    val jesExplorerViewMock = mockk<JesExplorerView>()
    val eventMock = mockk<AnActionEvent> {
      every { presentation } returns mockk {
        every {
          isEnabledAndVisible = any()
        } answers {
          didChangeIsEnabledAndVisible = true
          isEnabledAndVisibleNewValue = firstArg<Boolean>()
        }
        every {
          isVisible = any<Boolean>()
        } answers {
          didChangeIsVisible = true
          isVisibleNewValue = firstArg<Boolean>()
        }
      }
    }

    mockkConstructor(DocumentedSyncProvider::class)
    every {
      anyConstructed<DocumentedSyncProvider>().putInitialContent(any())
    } answers {
      didPutInitialContent = true
    }
    every {
      anyConstructed<DocumentedSyncProvider>().loadNewContent(any())
    } answers {
      didLoadNewContent = true
    }

    mockkConstructor(OpenFileDescriptor::class)
    every {
      anyConstructed<OpenFileDescriptor>().navigate(any())
    } answers {
      didTriggerNavigate = true
    }

    val editJclAction = EditJclAction()

    val dataOpsManager = DataOpsManager.getService()

    var notificationsService = NotificationsService.getService()
    every {
      notificationsService
        .notifyError(any(), any(), any(), any(), any())
    } answers {
      didNotifyError = true
    }

    beforeEach {
      didTriggerGetJclRecordsOperation = false
      didPutInitialContent = false
      didRetrieveCurrentContent = false
      didLoadNewContent = false
      didNotifyError = false
      didTriggerNavigate = false
      didChangeIsEnabledAndVisible = false
      didChangeIsVisible = false
      isEnabledAndVisibleNewValue = null
      isVisibleNewValue = null

      every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
        mockk {
          every { node } returns mockk<JobNode> {
            every { unit } returns mockk {
              every { connectionConfig } returns mockk()
            }
            every { virtualFile } returns jclNodeVirtualFile
          }
          every { attributes } returns mockk<RemoteJobAttributes> {
            every { jobInfo } returns mockk {
              every { jobName } returns "TESTJOB"
              every { jobId } returns "TESTJID"
            }
            every { name } returns "TESTJOB"
          }
        }
      )
      every { eventMock.getData(EXPLORER_VIEW) } returns jesExplorerViewMock
      every { eventMock.project } returns ProjectManager.getInstance().defaultProject
      every { jclNodeVirtualFile.findChild(any()) } returns null

      every {
        anyConstructed<DocumentedSyncProvider>().retrieveCurrentContent()
      } answers {
        didRetrieveCurrentContent = true
        byteArrayOf(1, 2, 3)
      }

      every {
        dataOpsManager.performOperation(any<Operation<Any>>(), any())
      } answers {
        when (val operation = firstArg<Operation<*>>()) {
          is GetJclRecordsOperation -> {
            didTriggerGetJclRecordsOperation = true
            byteArrayOf(1, 2, 3)
          }

          else -> fail("Unexpected operation: $operation")
        }
      }
    }

    context("actionPerformed") {
      should("create a new editor document with the JCL to edit") {
        editJclAction.actionPerformed(eventMock)

        assertSoftly {
          didTriggerGetJclRecordsOperation shouldBe true
          didPutInitialContent shouldBe true
          didRetrieveCurrentContent shouldBe false
          didLoadNewContent shouldBe false
          didTriggerNavigate shouldBe true
          didNotifyError shouldBe false
        }
      }

      should("reuse an editor document with the old JCL to edit") {
        every { jclNodeVirtualFile.findChild(any()) } returns mockk<MFVirtualFile>()

        editJclAction.actionPerformed(eventMock)

        assertSoftly {
          didTriggerGetJclRecordsOperation shouldBe true
          didPutInitialContent shouldBe false
          didRetrieveCurrentContent shouldBe true
          didLoadNewContent shouldBe false
          didTriggerNavigate shouldBe true
          didNotifyError shouldBe false
        }
      }

      should("reuse an editor document with the new JCL to edit") {
        every { jclNodeVirtualFile.findChild(any()) } returns mockk<MFVirtualFile>()

        every {
          anyConstructed<DocumentedSyncProvider>().retrieveCurrentContent()
        } answers {
          didRetrieveCurrentContent = true
          byteArrayOf(1, 2)
        }

        editJclAction.actionPerformed(eventMock)

        assertSoftly {
          didTriggerGetJclRecordsOperation shouldBe true
          didPutInitialContent shouldBe false
          didRetrieveCurrentContent shouldBe true
          didLoadNewContent shouldBe true
          didTriggerNavigate shouldBe true
          didNotifyError shouldBe false
        }
      }

      should("not allow to edit JCL as there is no open project found") {
        every { eventMock.project } returns null

        editJclAction.actionPerformed(eventMock)

        assertSoftly {
          didTriggerGetJclRecordsOperation shouldBe true
          didPutInitialContent shouldBe false
          didRetrieveCurrentContent shouldBe false
          didLoadNewContent shouldBe false
          didTriggerNavigate shouldBe false
          didNotifyError shouldBe false
        }
      }

      should("not allow to edit JCL as there is no virtual file for the selected node") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<JobNode> {
              every { unit } returns mockk {
                every { connectionConfig } returns mockk()
              }
              every { virtualFile } returns null
            }
            every { attributes } returns mockk<RemoteJobAttributes> {
              every { jobInfo } returns mockk {
                every { jobName } returns "TESTJOB"
                every { jobId } returns "TESTJID"
              }
            }
          }
        )

        editJclAction.actionPerformed(eventMock)

        assertSoftly {
          didTriggerGetJclRecordsOperation shouldBe true
          didPutInitialContent shouldBe false
          didRetrieveCurrentContent shouldBe false
          didLoadNewContent shouldBe false
          didTriggerNavigate shouldBe false
          didNotifyError shouldBe false
        }
      }

      should("fail to edit JCL as there was an exception during the content retrieval operation") {
        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any())
        } answers {
          didTriggerGetJclRecordsOperation = true
          Throwable("Test exception")
        }

        editJclAction.actionPerformed(eventMock)

        assertSoftly {
          didTriggerGetJclRecordsOperation shouldBe true
          didPutInitialContent shouldBe false
          didRetrieveCurrentContent shouldBe false
          didLoadNewContent shouldBe false
          didTriggerNavigate shouldBe false
          didNotifyError shouldBe true
        }
      }

      should("not allow to edit JCL as there is no connection config associated with the node") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns mockk<JobNode> {
              every { unit } returns mockk {
                every { connectionConfig } returns null
              }
            }
          }
        )

        editJclAction.actionPerformed(eventMock)

        assertSoftly {
          didTriggerGetJclRecordsOperation shouldBe false
          didPutInitialContent shouldBe false
          didRetrieveCurrentContent shouldBe false
          didLoadNewContent shouldBe false
          didTriggerNavigate shouldBe false
          didNotifyError shouldBe false
        }
      }

      should("not allow to edit JCL as the selected node is not a job node") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk() }
        )

        editJclAction.actionPerformed(eventMock)

        assertSoftly {
          didTriggerGetJclRecordsOperation shouldBe false
          didPutInitialContent shouldBe false
          didRetrieveCurrentContent shouldBe false
          didLoadNewContent shouldBe false
          didTriggerNavigate shouldBe false
          didNotifyError shouldBe false
        }
      }

      should("not allow to edit JCL as there is no selected node") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf()

        editJclAction.actionPerformed(eventMock)

        assertSoftly {
          didTriggerGetJclRecordsOperation shouldBe false
          didPutInitialContent shouldBe false
          didRetrieveCurrentContent shouldBe false
          didLoadNewContent shouldBe false
          didTriggerNavigate shouldBe false
          didNotifyError shouldBe false
        }
      }

      should("not allow to edit JCL as JES Explorer view is not initialized yet") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        editJclAction.actionPerformed(eventMock)

        assertSoftly {
          didTriggerGetJclRecordsOperation shouldBe false
          didPutInitialContent shouldBe false
          didRetrieveCurrentContent shouldBe false
          didLoadNewContent shouldBe false
          didTriggerNavigate shouldBe false
          didNotifyError shouldBe false
        }
      }
    }

    context("update") {
      should("show the Edit JCL option for a job node") {
        editJclAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          didChangeIsVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe null
          isVisibleNewValue shouldBe true
        }
      }

      should("not show the Edit JCL option for a non-job node") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk() }
        )

        editJclAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          didChangeIsVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe null
          isVisibleNewValue shouldBe false
        }
      }

      should("not show the Edit JCL option when no nodes selected") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf()

        editJclAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe false
          didChangeIsVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe null
          isVisibleNewValue shouldBe false
        }
      }

      should("not show the Edit JCL option when JES Explorer view is not initialized yet") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        editJclAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          didChangeIsVisible shouldBe false
          isEnabledAndVisibleNewValue shouldBe false
          isVisibleNewValue shouldBe null
        }
      }
    }
  }
})
