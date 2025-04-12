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
 *   Dzianis Lisiankou
 */

package org.zowe.explorer.explorer.actions

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.common.ui.StatefulDialog
import org.zowe.explorer.common.ui.cleanInvalidateOnExpand
import org.zowe.explorer.common.ui.showUntilDone
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.DatasetAllocationOperation
import org.zowe.explorer.dataops.operations.DatasetAllocationParams
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.kotlinsdk.DatasetOrganization
import org.zowe.kotlinsdk.DsnameType
import java.util.*
import kotlin.reflect.KFunction

class AllocateDatasetActionTestSpec : AppInitShouldSpec("explorer/actions/AllocateDatasetAction", {
  context("all functions") {
    val allocateDsActionInst = AllocateDatasetAction()

    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "test"
    every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()

    context("actionPerformed") {
      var isCleanInvalidateOnExpandTriggered = false
      var isThrowableReported = false
      var isOperationPerformed = false
      var isUpdateOnConfigCrudableCalled = false

      val anActionEventMock = mockk<AnActionEvent>()
      val viewMock = mockk<FileExplorerView>()
      val filesWorkingSetConfigMock = mockk<FilesWorkingSetConfig>()
      val componentManagerMock = mockk<ComponentManager>()
      val configServiceCrudableMock = mockk<Crudable>()
      val workingSetMock = mockk<FilesWorkingSet> {
        every { name } returns "test"
        every { uuid } returns "test"
        every { connectionConfig } returns mockk {
          every { uuid } returns "fake_uuid"
        }
        every { explorer } returns mockk {
          every { componentManager } returns componentManagerMock
        }
      }
      val libraryNodeMock = mockk<LibraryNode> {
        every { parent } returns mockk<DSMaskNode>(relaxUnitFun = true)
        every { unit } returns workingSetMock
      }

      val notificationsService = NotificationsService.getService()
      every {
        notificationsService.notifyError(any<Throwable>(), any<Project>(), any<String>(), any<String>(), any<String>())
      } answers {
        isThrowableReported = true
      }

      val dataOpsManager = DataOpsManager.getService()

      val configService = ConfigService.getService()
      every { configService.crudable } returns configServiceCrudableMock

      lateinit var addMaskActionInst: AnAction

      val cleanInvalidateOnExpandMock: (
        node: ExplorerTreeNode<*, *>,
        view: ExplorerTreeView<ConnectionConfig, *, *>
      ) -> Unit = ::cleanInvalidateOnExpand
      mockkStatic(cleanInvalidateOnExpandMock as KFunction<*>)

      val notifyRef: (Notification) -> Unit = Notifications.Bus::notify
      mockkStatic(notifyRef as KFunction<*>)
      mockkStatic(Notification::get)

      val showUntilDoneMockk: (
        DatasetAllocationParams,
        (DatasetAllocationParams) -> StatefulDialog<DatasetAllocationParams>,
        (DatasetAllocationParams) -> Boolean
      ) -> DatasetAllocationParams? = ::showUntilDone
      mockkStatic(showUntilDoneMockk as KFunction<*>)

      beforeEach {
        isCleanInvalidateOnExpandTriggered = false
        isThrowableReported = false
        isOperationPerformed = false
        isUpdateOnConfigCrudableCalled = false

        every { filesWorkingSetConfigMock.dsMasks } returns mutableListOf()
        every { viewMock.mySelectedNodesData } returns listOf()
        every { anActionEventMock.getData(EXPLORER_VIEW) } returns viewMock
        every { anActionEventMock.project } returns mockk()

        every {
          configServiceCrudableMock.getByUniqueKey(FilesWorkingSetConfig::class.java, any<String>())
        } returns Optional.of(filesWorkingSetConfigMock)
        every {
          configServiceCrudableMock.update(any<FilesWorkingSetConfig>())
        } answers {
          isUpdateOnConfigCrudableCalled = true
          Optional.of(mockk())
        }

        every {
          dataOpsManager.performOperation(any<DatasetAllocationOperation>(), any<ProgressIndicator>())
        } answers {
          isOperationPerformed = true
        }

        every {
          cleanInvalidateOnExpandMock(any<ExplorerTreeNode<*, *>>(), any<ExplorerTreeView<ConnectionConfig, *, *>>())
        } answers {
          isCleanInvalidateOnExpandTriggered = true
        }

        every {
          Notifications.Bus.notify(any<Notification>())
        } answers {
          val notification = firstArg<Notification>()
          every { Notification.get(any()) } returns notification
          addMaskActionInst = notification.actions.first { it.templateText == "Add mask" }
        }

        every {
          showUntilDoneMockk(
            any<DatasetAllocationParams>(),
            any<(DatasetAllocationParams) -> StatefulDialog<DatasetAllocationParams>>(),
            any<(DatasetAllocationParams) -> Boolean>()
          )
        } returns null
      }

      should("perform allocate PDS dataset action creating a new dataset mask") {
        var isShowUntilDoneSucceeded = false

        lateinit var initState: DatasetAllocationParams

        every {
          showUntilDoneMockk(
            any<DatasetAllocationParams>(),
            any<(DatasetAllocationParams) -> StatefulDialog<DatasetAllocationParams>>(),
            any<(DatasetAllocationParams) -> Boolean>()
          )
        } answers {
          initState = firstArg<DatasetAllocationParams>()
          val thirdBlockResult = thirdArg<(DatasetAllocationParams) -> Boolean>()
          isShowUntilDoneSucceeded = thirdBlockResult(initState)
          initState
        }

        every { viewMock.mySelectedNodesData } returns listOf(NodeData(libraryNodeMock, null, null))

        allocateDsActionInst.actionPerformed(anActionEventMock)
        addMaskActionInst.actionPerformed(anActionEventMock)

        assertSoftly {
          isCleanInvalidateOnExpandTriggered shouldBe true
          isShowUntilDoneSucceeded shouldBe true
          isOperationPerformed shouldBe true
          isUpdateOnConfigCrudableCalled shouldBe true
          isThrowableReported shouldBe false
          initState.errorMessage shouldBe ""
        }
      }
      should("perform allocate PS dataset action creating a new dataset mask") {
        var isShowUntilDoneSucceeded = false

        lateinit var initState: DatasetAllocationParams

        every {
          showUntilDoneMockk(
            any<DatasetAllocationParams>(),
            any<(DatasetAllocationParams) -> StatefulDialog<DatasetAllocationParams>>(),
            any<(DatasetAllocationParams) -> Boolean>()
          )
        } answers {
          initState = firstArg<DatasetAllocationParams>()
          initState.datasetName = "test.test.test"
          val thirdBlockResult = thirdArg<(DatasetAllocationParams) -> Boolean>()
          initState.allocationParameters.datasetOrganization = DatasetOrganization.PS
          initState.allocationParameters.managementClass = "test"
          initState.allocationParameters.storageClass = "test"
          initState.allocationParameters.deviceType = "test"
          initState.allocationParameters.dataClass = "test"
          initState.allocationParameters.volumeSerial = "test"
          initState.allocationParameters.directoryBlocks = 1
          isShowUntilDoneSucceeded = thirdBlockResult(initState)
          initState
        }

        every { viewMock.mySelectedNodesData } returns listOf(NodeData(libraryNodeMock, null, null))

        allocateDsActionInst.actionPerformed(anActionEventMock)
        addMaskActionInst.actionPerformed(anActionEventMock)

        assertSoftly {
          isCleanInvalidateOnExpandTriggered shouldBe true
          isShowUntilDoneSucceeded shouldBe true
          isOperationPerformed shouldBe true
          isUpdateOnConfigCrudableCalled shouldBe true
          isThrowableReported shouldBe false
          initState.errorMessage shouldBe ""
          initState.allocationParameters.directoryBlocks shouldBe null
        }
      }
      should("perform allocate PO-E dataset action creating a new dataset mask") {
        var isShowUntilDoneSucceeded = false

        lateinit var initState: DatasetAllocationParams

        every {
          showUntilDoneMockk(
            any<DatasetAllocationParams>(),
            any<(DatasetAllocationParams) -> StatefulDialog<DatasetAllocationParams>>(),
            any<(DatasetAllocationParams) -> Boolean>()
          )
        } answers {
          initState = firstArg<DatasetAllocationParams>()
          val thirdBlockResult = thirdArg<(DatasetAllocationParams) -> Boolean>()
          initState.allocationParameters.datasetOrganization = DatasetOrganization.PS
          initState.allocationParameters.managementClass = ""
          initState.allocationParameters.directoryBlocks = 0
          initState.allocationParameters.datasetOrganization = DatasetOrganization.POE
          isShowUntilDoneSucceeded = thirdBlockResult(initState)
          initState
        }


        every { viewMock.mySelectedNodesData } returns listOf(NodeData(libraryNodeMock, null, null))

        allocateDsActionInst.actionPerformed(anActionEventMock)
        addMaskActionInst.actionPerformed(anActionEventMock)

        assertSoftly {
          isCleanInvalidateOnExpandTriggered shouldBe true
          isShowUntilDoneSucceeded shouldBe true
          isOperationPerformed shouldBe true
          isUpdateOnConfigCrudableCalled shouldBe true
          isThrowableReported shouldBe false
          initState.errorMessage shouldBe ""
          initState.allocationParameters.datasetOrganization shouldBe DatasetOrganization.PO
          initState.allocationParameters.dsnType shouldBe DsnameType.LIBRARY
        }
      }
      should("perform allocate dataset action without creating a new dataset mask") {
        var isShowUntilDoneSucceeded = false

        lateinit var initState: DatasetAllocationParams

        every {
          showUntilDoneMockk(
            any<DatasetAllocationParams>(),
            any<(DatasetAllocationParams) -> StatefulDialog<DatasetAllocationParams>>(),
            any<(DatasetAllocationParams) -> Boolean>()
          )
        } answers {
          initState = firstArg<DatasetAllocationParams>()
          val thirdBlockResult = thirdArg<(DatasetAllocationParams) -> Boolean>()
          isShowUntilDoneSucceeded = thirdBlockResult(initState)
          initState
        }

        every { viewMock.mySelectedNodesData } returns listOf(NodeData(libraryNodeMock, null, null))

        allocateDsActionInst.actionPerformed(anActionEventMock)

        assertSoftly {
          isCleanInvalidateOnExpandTriggered shouldBe true
          isShowUntilDoneSucceeded shouldBe true
          isOperationPerformed shouldBe true
          isUpdateOnConfigCrudableCalled shouldBe false
          isThrowableReported shouldBe false
          initState.errorMessage shouldBe ""
        }
      }
      should("perform allocate dataset action when dataset mask already exists") {
        var isShowUntilDoneSucceeded = false

        lateinit var initState: DatasetAllocationParams

        every {
          showUntilDoneMockk(
            any<DatasetAllocationParams>(),
            any<(DatasetAllocationParams) -> StatefulDialog<DatasetAllocationParams>>(),
            any<(DatasetAllocationParams) -> Boolean>()
          )
        } answers {
          initState = firstArg<DatasetAllocationParams>()
          initState.datasetName = "test.test.test"
          val thirdBlockResult = thirdArg<(DatasetAllocationParams) -> Boolean>()
          isShowUntilDoneSucceeded = thirdBlockResult(initState)
          initState
        }

        every {
          filesWorkingSetConfigMock.dsMasks
        } returns mutableListOf(
          mockk<DSMask> {
            every { mask } returns "test.test.*"
          }
        )

        every { viewMock.mySelectedNodesData } returns listOf(NodeData(libraryNodeMock, null, null))

        allocateDsActionInst.actionPerformed(anActionEventMock)

        assertSoftly {
          isCleanInvalidateOnExpandTriggered shouldBe true
          isShowUntilDoneSucceeded shouldBe true
          isOperationPerformed shouldBe true
          isUpdateOnConfigCrudableCalled shouldBe false
          isThrowableReported shouldBe false
          initState.errorMessage shouldBe ""
        }
      }
      should("perform allocate dataset action creating new dataset mask without adding as the connection config is not found") {
        var isShowUntilDoneSucceeded = false

        val filesWorkingSetNodeMock = mockk<FilesWorkingSetNode> {
          every { parent } returns null
          every { unit } returns workingSetMock
        }

        lateinit var initState: DatasetAllocationParams

        every {
          showUntilDoneMockk(
            any<DatasetAllocationParams>(),
            any<(DatasetAllocationParams) -> StatefulDialog<DatasetAllocationParams>>(),
            any<(DatasetAllocationParams) -> Boolean>()
          )
        } answers {
          initState = firstArg<DatasetAllocationParams>()
          val thirdBlockResult = thirdArg<(DatasetAllocationParams) -> Boolean>()
          isShowUntilDoneSucceeded = thirdBlockResult(initState)
          initState
        }

        every {
          configServiceCrudableMock.getByUniqueKey(FilesWorkingSetConfig::class.java, any<String>())
        } returns Optional.ofNullable(null)

        every {
          viewMock.mySelectedNodesData
        } returns listOf(NodeData(filesWorkingSetNodeMock, null, null))

        allocateDsActionInst.actionPerformed(anActionEventMock)

        assertSoftly {
          isCleanInvalidateOnExpandTriggered shouldBe false
          isShowUntilDoneSucceeded shouldBe true
          isOperationPerformed shouldBe true
          isUpdateOnConfigCrudableCalled shouldBe false
          isThrowableReported shouldBe false
          initState.errorMessage shouldBe ""
        }
      }
      should("perform allocate dataset action with failure on operation performing") {
        var isShowUntilDoneSucceeded = false

        val filesWorkingSetNodeMock = mockk<FilesWorkingSetNode> {
          every { unit } returns workingSetMock
        }
        val exceptionMsg = "test exception"

        lateinit var initState: DatasetAllocationParams

        every {
          showUntilDoneMockk(
            any<DatasetAllocationParams>(),
            any<(DatasetAllocationParams) -> StatefulDialog<DatasetAllocationParams>>(),
            any<(DatasetAllocationParams) -> Boolean>()
          )
        } answers {
          initState = firstArg<DatasetAllocationParams>()
          val thirdBlockResult = thirdArg<(DatasetAllocationParams) -> Boolean>()
          isShowUntilDoneSucceeded = thirdBlockResult(initState)
          initState
        }

        every {
          viewMock.mySelectedNodesData
        } returns listOf(NodeData(filesWorkingSetNodeMock, null, null))

        every {
          dataOpsManager.performOperation(any<DatasetAllocationOperation>(), any<ProgressIndicator>())
        } throws Exception(exceptionMsg)

        allocateDsActionInst.actionPerformed(anActionEventMock)

        assertSoftly {
          isCleanInvalidateOnExpandTriggered shouldBe false
          isShowUntilDoneSucceeded shouldBe false
          isThrowableReported shouldBe true
          initState.errorMessage shouldBe exceptionMsg
        }
      }
    }

    context("update") {
      var isPresentationEnabledAndVisible = false
      var isPresentationEnabled = false

      val viewMock = mockk<FileExplorerView>()
      val anActionEventMock = mockk<AnActionEvent> {
        every { presentation } returns mockk(relaxUnitFun = true) {
          every {
            isEnabledAndVisible = any<Boolean>()
          } answers {
            isPresentationEnabledAndVisible = firstArg<Boolean>()
          }
          every {
            isEnabled = any<Boolean>()
          } answers {
            isPresentationEnabled = firstArg<Boolean>()
          }
        }
      }

      beforeEach {
        isPresentationEnabledAndVisible = false
        isPresentationEnabled = false

        every { viewMock.mySelectedNodesData } returns listOf()
        every { anActionEventMock.getData(EXPLORER_VIEW) } returns viewMock
      }

      should("show the action on update function is triggered for LibraryNode") {
        val nodeMock = mockk<LibraryNode> {
          every { unit } returns mockk {
            every { connectionConfig } returns mockk()
          }
        }

        every { viewMock.mySelectedNodesData } returns listOf(NodeData(nodeMock, null, null))

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe true }
      }
      should("show the action on update function is triggered for FilesWorkingSetNode") {
        val nodeMock = mockk<FilesWorkingSetNode> {
          every { unit } returns mockk {
            every { connectionConfig } returns mockk()
          }
        }

        every { viewMock.mySelectedNodesData } returns listOf(NodeData(nodeMock, null, null))

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe true }
      }
      should("show the action on update function is triggered for DSMaskNode") {
        val nodeMock = mockk<DSMaskNode> {
          every { unit } returns mockk {
            every { connectionConfig } returns mockk()
          }
        }

        every { viewMock.mySelectedNodesData } returns listOf(NodeData(nodeMock, null, null))

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe true }
      }
      should("show the action on update function is triggered for FileLikeDatasetNode") {
        val nodeMock = mockk<FileLikeDatasetNode> {
          every { unit } returns mockk {
            every { connectionConfig } returns mockk()
          }
        }

        every { viewMock.mySelectedNodesData } returns listOf(NodeData(nodeMock, null, null))

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe true }
      }
      should("not show the action on update function is triggered for JobNode") {
        val nodeMock = mockk<JobNode> {
          every { unit } returns mockk {
            every { connectionConfig } returns mockk()
          }
        }

        every { viewMock.mySelectedNodesData } returns listOf(NodeData(nodeMock, null, null))

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe false }
      }
      should("not show the action on update function is triggered without selected node") {
        every { viewMock.mySelectedNodesData } returns listOf()

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly {
          isPresentationEnabledAndVisible shouldBe false
          isPresentationEnabled shouldBe false
        }
      }
      should("not show the action on update function is triggered outside the file explorer view") {
        every { anActionEventMock.getData(EXPLORER_VIEW) } returns null

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe false }
      }
      should("not enable the action on update function is triggered without connection config") {
        val nodeMock = mockk<FilesWorkingSetNode> {
          every { unit } returns mockk {
            every { connectionConfig } returns null
          }
        }

        every { viewMock.mySelectedNodesData } returns listOf(NodeData(nodeMock, null, null))

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabled shouldBe false }
      }
    }
  }
})
