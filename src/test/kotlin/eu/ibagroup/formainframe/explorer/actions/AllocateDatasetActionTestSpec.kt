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
 */

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.AnalyticsEvent
import eu.ibagroup.formainframe.common.ui.StatefulDialog
import eu.ibagroup.formainframe.common.ui.cleanInvalidateOnExpand
import eu.ibagroup.formainframe.common.ui.showUntilDone
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Operation
import eu.ibagroup.formainframe.dataops.operations.DatasetAllocationParams
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.explorer.ui.DSMaskNode
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeNode
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeView
import eu.ibagroup.formainframe.explorer.ui.FileExplorerView
import eu.ibagroup.formainframe.explorer.ui.FileLikeDatasetNode
import eu.ibagroup.formainframe.explorer.ui.FilesWorkingSetNode
import eu.ibagroup.formainframe.explorer.ui.JobNode
import eu.ibagroup.formainframe.explorer.ui.LibraryNode
import eu.ibagroup.formainframe.explorer.ui.NodeData
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import eu.ibagroup.formainframe.telemetry.NotificationsService
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestAnalyticsServiceImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestNotificationsServiceImpl
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.zowe.kotlinsdk.DatasetOrganization
import org.zowe.kotlinsdk.DsnameType
import java.util.*
import javax.swing.Icon
import kotlin.reflect.KFunction

class AllocateDatasetActionTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }
  context("explorer module: actions/AllocateDatasetAction") {
    val anActionEventMock = mockk<AnActionEvent>()
    val viewMock = mockk<FileExplorerView>()
    val allocateDsActionInst = AllocateDatasetAction()

    context("actionPerformed") {
      var isCleanInvalidateOnExpandTriggered = false
      var isAnalitycsTracked = false
      var isThrowableReported = false
      val filesWorkingSetConfigMock = mockk<FilesWorkingSetConfig>()
      val componentManagerMock = mockk<ComponentManager>()
      val explorerMock = mockk<Explorer<ConnectionConfig, *>>()
      val notificationsService = NotificationsService.getService() as TestNotificationsServiceImpl

      lateinit var addMaskActionInst: AnAction

      val analyticsService = AnalyticsService.getService() as TestAnalyticsServiceImpl
      analyticsService.testInstance = object : TestAnalyticsServiceImpl() {
        override fun trackAnalyticsEvent(event: AnalyticsEvent) {
          isAnalitycsTracked = true
          super.trackAnalyticsEvent(event)
        }
      }

      beforeEach {
        isCleanInvalidateOnExpandTriggered = false
        isAnalitycsTracked = false
        isThrowableReported = false

        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock
        every { anActionEventMock.project } returns mockk()

        every { explorerMock.componentManager } returns componentManagerMock

        notificationsService.testInstance = object : TestNotificationsServiceImpl() {
          override fun notifyError(
            t: Throwable,
            project: Project?,
            custTitle: String?,
            custDetailsShort: String?,
            custDetailsLong: String?
          ) {
            isThrowableReported = true
          }
        }

        val cleanInvalidateOnExpandMock: (
          node: ExplorerTreeNode<*, *>,
          view: ExplorerTreeView<ConnectionConfig, *, *>
        ) -> Unit = ::cleanInvalidateOnExpand
        mockkStatic(cleanInvalidateOnExpandMock as KFunction<*>)
        every {
          cleanInvalidateOnExpandMock(any<ExplorerTreeNode<*, *>>(), any<ExplorerTreeView<ConnectionConfig, *, *>>())
        } answers {
          isCleanInvalidateOnExpandTriggered = true
        }

        val notifyRef: (Notification) -> Unit = Notifications.Bus::notify
        mockkStatic(notifyRef as KFunction<*>)
        mockkStatic(Notification::get)
        every { Notifications.Bus.notify(any<Notification>()) } answers {
          val notification = firstArg<Notification>()
          every { Notification.get(any()) } returns notification
          addMaskActionInst = notification.actions.first { it.templateText == "Add mask" }
        }
      }

      afterEach {
        clearAllMocks()
        unmockkAll()
      }

      should("perform allocate PDS dataset action creating a new dataset mask") {
        val workingSetMock = mockk<FilesWorkingSet>()
        val nodeMock = mockk<LibraryNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)
        val dsMaskNodeMock = mockk<DSMaskNode>()
        lateinit var initState: DatasetAllocationParams
        var isOperationPerformed = false
        var isUpdateOnConfigCrudableCalled = false
        var isShowUntilDoneSucceeded = false

        val showUntilDoneMockk: (
          DatasetAllocationParams,
          (DatasetAllocationParams) -> StatefulDialog<DatasetAllocationParams>,
          (DatasetAllocationParams) -> Boolean
        ) -> DatasetAllocationParams? = ::showUntilDone
        mockkStatic(showUntilDoneMockk as KFunction<*>)
        every {
          hint(DatasetAllocationParams::class)
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
          ConfigService.getService().crudable.getByUniqueKey<FilesWorkingSetConfig, String>(any(), any())
        } returns Optional.of(filesWorkingSetConfigMock)
        every {
          ConfigService.getService().crudable.update(any<FilesWorkingSetConfig>())
        } answers {
          isUpdateOnConfigCrudableCalled = true
          Optional.of(mockk())
        }

        every { dsMaskNodeMock.cleanCache(any(), any(), any(), any()) } returns Unit
        every { nodeMock.parent } returns dsMaskNodeMock
        every { nodeMock.hint(FilesWorkingSet::class).unit } returns workingSetMock
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { workingSetMock.name } returns "test"
        every { workingSetMock.uuid } returns "test"
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns mockk<ConnectionConfig>()
        val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            isOperationPerformed = true
            return true as R
          }
        }
        every { workingSetMock.explorer } returns explorerMock

        allocateDsActionInst.actionPerformed(anActionEventMock)
        addMaskActionInst.actionPerformed(anActionEventMock)

        assertSoftly {
          isCleanInvalidateOnExpandTriggered shouldBe true
          isShowUntilDoneSucceeded shouldBe true
          isAnalitycsTracked shouldBe true
          isOperationPerformed shouldBe true
          isUpdateOnConfigCrudableCalled shouldBe true
          isThrowableReported shouldBe false
          initState.errorMessage shouldBe ""
        }
      }
      should("perform allocate PS dataset action creating a new dataset mask") {
        val workingSetMock = mockk<FilesWorkingSet>()
        val nodeMock = mockk<LibraryNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)
        val dsMaskNodeMock = mockk<DSMaskNode>()
        lateinit var initState: DatasetAllocationParams
        var isOperationPerformed = false
        var isUpdateOnConfigCrudableCalled = false
        var isShowUntilDoneSucceeded = false

        val showUntilDoneMockk: (
          DatasetAllocationParams,
          (DatasetAllocationParams) -> StatefulDialog<DatasetAllocationParams>,
          (DatasetAllocationParams) -> Boolean
        ) -> DatasetAllocationParams? = ::showUntilDone
        mockkStatic(showUntilDoneMockk as KFunction<*>)
        every {
          hint(DatasetAllocationParams::class)
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

        every {
          ConfigService.getService().crudable.getByUniqueKey<FilesWorkingSetConfig, String>(any(), any())
        } returns Optional.of(filesWorkingSetConfigMock)
        every {
          ConfigService.getService().crudable.update(any<FilesWorkingSetConfig>())
        } answers {
          isUpdateOnConfigCrudableCalled = true
          Optional.of(mockk())
        }

        every { dsMaskNodeMock.cleanCache(any(), any(), any(), any()) } returns Unit
        every { nodeMock.parent } returns dsMaskNodeMock
        every { nodeMock.hint(FilesWorkingSet::class).unit } returns workingSetMock
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { workingSetMock.name } returns "test"
        every { workingSetMock.uuid } returns "test"
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns mockk<ConnectionConfig>()
        val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            isOperationPerformed = true
            return true as R
          }
        }
        every { workingSetMock.explorer } returns explorerMock

        allocateDsActionInst.actionPerformed(anActionEventMock)
        addMaskActionInst.actionPerformed(anActionEventMock)

        assertSoftly {
          isCleanInvalidateOnExpandTriggered shouldBe true
          isShowUntilDoneSucceeded shouldBe true
          isAnalitycsTracked shouldBe true
          isOperationPerformed shouldBe true
          isUpdateOnConfigCrudableCalled shouldBe true
          isThrowableReported shouldBe false
          initState.errorMessage shouldBe ""
          initState.allocationParameters.directoryBlocks shouldBe null
        }
      }
      should("perform allocate PO-E dataset action creating a new dataset mask") {
        val workingSetMock = mockk<FilesWorkingSet>()
        val nodeMock = mockk<LibraryNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)
        val dsMaskNodeMock = mockk<DSMaskNode>()
        lateinit var initState: DatasetAllocationParams
        var isOperationPerformed = false
        var isUpdateOnConfigCrudableCalled = false
        var isShowUntilDoneSucceeded = false

        val showUntilDoneMockk: (
          DatasetAllocationParams,
          (DatasetAllocationParams) -> StatefulDialog<DatasetAllocationParams>,
          (DatasetAllocationParams) -> Boolean
        ) -> DatasetAllocationParams? = ::showUntilDone
        mockkStatic(showUntilDoneMockk as KFunction<*>)
        every {
          hint(DatasetAllocationParams::class)
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

        every {
          ConfigService.getService().crudable.getByUniqueKey<FilesWorkingSetConfig, String>(any(), any())
        } returns Optional.of(filesWorkingSetConfigMock)
        every {
          ConfigService.getService().crudable.update(any<FilesWorkingSetConfig>())
        } answers {
          isUpdateOnConfigCrudableCalled = true
          Optional.of(mockk())
        }

        every { dsMaskNodeMock.cleanCache(any(), any(), any(), any()) } returns Unit
        every { nodeMock.parent } returns dsMaskNodeMock
        every { nodeMock.hint(FilesWorkingSet::class).unit } returns workingSetMock
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { workingSetMock.name } returns "test"
        every { workingSetMock.uuid } returns "test"
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns mockk<ConnectionConfig>()
        val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            isOperationPerformed = true
            return true as R
          }
        }
        every { workingSetMock.explorer } returns explorerMock

        allocateDsActionInst.actionPerformed(anActionEventMock)
        addMaskActionInst.actionPerformed(anActionEventMock)

        assertSoftly {
          isCleanInvalidateOnExpandTriggered shouldBe true
          isShowUntilDoneSucceeded shouldBe true
          isAnalitycsTracked shouldBe true
          isOperationPerformed shouldBe true
          isUpdateOnConfigCrudableCalled shouldBe true
          isThrowableReported shouldBe false
          initState.errorMessage shouldBe ""
          initState.allocationParameters.datasetOrganization shouldBe DatasetOrganization.PO
          initState.allocationParameters.dsnType shouldBe DsnameType.LIBRARY
        }
      }
      should("perform allocate dataset action without creating a new dataset mask") {
        val workingSetMock = mockk<FilesWorkingSet>()
        val nodeMock = mockk<LibraryNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)
        val dsMaskNodeMock = mockk<DSMaskNode>()
        lateinit var initState: DatasetAllocationParams
        var isOperationPerformed = false
        var isUpdateOnConfigCrudableCalled = false
        var isShowUntilDoneSucceeded = false

        val showUntilDoneMockk: (
          DatasetAllocationParams,
          (DatasetAllocationParams) -> StatefulDialog<DatasetAllocationParams>,
          (DatasetAllocationParams) -> Boolean
        ) -> DatasetAllocationParams? = ::showUntilDone
        mockkStatic(showUntilDoneMockk as KFunction<*>)
        every {
          hint(DatasetAllocationParams::class)
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
          ConfigService.getService().crudable.getByUniqueKey<FilesWorkingSetConfig, String>(any(), any())
        } returns Optional.of(filesWorkingSetConfigMock)
        every {
          ConfigService.getService().crudable.update(any<FilesWorkingSetConfig>())
        } answers {
          isUpdateOnConfigCrudableCalled = true
          Optional.of(mockk())
        }

        every { dsMaskNodeMock.cleanCache(any(), any(), any(), any()) } returns Unit
        every { nodeMock.parent } returns dsMaskNodeMock
        every { nodeMock.hint(FilesWorkingSet::class).unit } returns workingSetMock
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { workingSetMock.name } returns "test"
        every { workingSetMock.uuid } returns "test"
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns mockk<ConnectionConfig>()
        val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            isOperationPerformed = true
            return true as R
          }
        }
        every { workingSetMock.explorer } returns explorerMock

        allocateDsActionInst.actionPerformed(anActionEventMock)

        assertSoftly {
          isCleanInvalidateOnExpandTriggered shouldBe true
          isShowUntilDoneSucceeded shouldBe true
          isAnalitycsTracked shouldBe true
          isOperationPerformed shouldBe true
          isUpdateOnConfigCrudableCalled shouldBe false
          isThrowableReported shouldBe false
          initState.errorMessage shouldBe ""
        }
      }
      should("perform allocate dataset action when dataset mask already exists") {
        val workingSetMock = mockk<FilesWorkingSet>()
        val nodeMock = mockk<LibraryNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)
        val dsMaskNodeMock = mockk<DSMaskNode>()
        lateinit var initState: DatasetAllocationParams
        var isOperationPerformed = false
        var isUpdateOnConfigCrudableCalled = false
        var isShowUntilDoneSucceeded = false

        val showUntilDoneMockk: (
          DatasetAllocationParams,
          (DatasetAllocationParams) -> StatefulDialog<DatasetAllocationParams>,
          (DatasetAllocationParams) -> Boolean
        ) -> DatasetAllocationParams? = ::showUntilDone
        mockkStatic(showUntilDoneMockk as KFunction<*>)
        every {
          hint(DatasetAllocationParams::class)
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

        val dsMaskMock = mockk<DSMask>()
        every { dsMaskMock.mask } returns "test.test.*"
        every { filesWorkingSetConfigMock.dsMasks } returns mutableListOf(dsMaskMock)

        every {
          ConfigService.getService().crudable.getByUniqueKey<FilesWorkingSetConfig, String>(any(), any())
        } returns Optional.of(filesWorkingSetConfigMock)
        every {
          ConfigService.getService().crudable.update(any<FilesWorkingSetConfig>())
        } answers {
          isUpdateOnConfigCrudableCalled = true
          Optional.of(mockk())
        }

        every { dsMaskNodeMock.cleanCache(any(), any(), any(), any()) } returns Unit
        every { nodeMock.parent } returns dsMaskNodeMock
        every { nodeMock.hint(FilesWorkingSet::class).unit } returns workingSetMock
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { workingSetMock.name } returns "test"
        every { workingSetMock.uuid } returns "test"
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns mockk<ConnectionConfig>()
        val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            isOperationPerformed = true
            return true as R
          }
        }
        every { workingSetMock.explorer } returns explorerMock

        allocateDsActionInst.actionPerformed(anActionEventMock)

        assertSoftly {
          isCleanInvalidateOnExpandTriggered shouldBe true
          isShowUntilDoneSucceeded shouldBe true
          isAnalitycsTracked shouldBe true
          isOperationPerformed shouldBe true
          isUpdateOnConfigCrudableCalled shouldBe false
          isThrowableReported shouldBe false
          initState.errorMessage shouldBe ""
        }
      }
      should("perform allocate dataset action creating new dataset mask without adding as the connection config is not found") {
        val workingSetMock = mockk<FilesWorkingSet>()
        val nodeMock = mockk<FilesWorkingSetNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)
        lateinit var initState: DatasetAllocationParams
        var isOperationPerformed = false
        var isUpdateOnConfigCrudableCalled = false
        var isShowUntilDoneSucceeded = false

        val showUntilDoneMockk: (
          DatasetAllocationParams,
          (DatasetAllocationParams) -> StatefulDialog<DatasetAllocationParams>,
          (DatasetAllocationParams) -> Boolean
        ) -> DatasetAllocationParams? = ::showUntilDone
        mockkStatic(showUntilDoneMockk as KFunction<*>)
        every {
          hint(DatasetAllocationParams::class)
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
          ConfigService.getService().crudable.getByUniqueKey<FilesWorkingSetConfig, String>(any(), any())
        } returns Optional.ofNullable(null)
        every {
          ConfigService.getService().crudable.update(any<FilesWorkingSetConfig>())
        } answers {
          isUpdateOnConfigCrudableCalled = true
          Optional.of(mockk())
        }

        every { nodeMock.parent } returns null
        every { nodeMock.hint(FilesWorkingSet::class).unit } returns workingSetMock
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { workingSetMock.name } returns "test"
        every { workingSetMock.uuid } returns "test"
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns mockk<ConnectionConfig>()
        val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            isOperationPerformed = true
            return true as R
          }
        }
        every { workingSetMock.explorer } returns explorerMock

        allocateDsActionInst.actionPerformed(anActionEventMock)

        assertSoftly {
          isCleanInvalidateOnExpandTriggered shouldBe false
          isShowUntilDoneSucceeded shouldBe true
          isAnalitycsTracked shouldBe true
          isOperationPerformed shouldBe true
          isUpdateOnConfigCrudableCalled shouldBe false
          isThrowableReported shouldBe false
          initState.errorMessage shouldBe ""
        }
      }
      should("perform allocate dataset action with failure on operation performing") {
        val workingSetMock = mockk<FilesWorkingSet>()
        val nodeMock = mockk<FilesWorkingSetNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)
        lateinit var initState: DatasetAllocationParams
        val exceptionMsg = "test exception"
        var isShowUntilDoneSucceeded = false

        val showUntilDoneMockk: (
          DatasetAllocationParams,
          (DatasetAllocationParams) -> StatefulDialog<DatasetAllocationParams>,
          (DatasetAllocationParams) -> Boolean
        ) -> DatasetAllocationParams? = ::showUntilDone
        mockkStatic(showUntilDoneMockk as KFunction<*>)
        every {
          hint(DatasetAllocationParams::class)
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

        every { nodeMock.hint(FilesWorkingSet::class).unit } returns workingSetMock
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns mockk<ConnectionConfig>()
        val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            throw Exception(exceptionMsg)
          }
        }
        every { workingSetMock.explorer } returns explorerMock

        allocateDsActionInst.actionPerformed(anActionEventMock)

        assertSoftly {
          isCleanInvalidateOnExpandTriggered shouldBe false
          isShowUntilDoneSucceeded shouldBe false
          isAnalitycsTracked shouldBe true
          isThrowableReported shouldBe true
          initState.errorMessage shouldBe exceptionMsg
        }
      }
    }
    context("update") {
      val presentationMock = mockk<Presentation>()
      var isPresentationEnabledAndVisible = false
      var isPresentationEnabled = false

      beforeEach {
        every {
          presentationMock.isEnabledAndVisible = any<Boolean>()
        } answers {
          isPresentationEnabledAndVisible = firstArg<Boolean>()
        }
        every {
          presentationMock.isEnabled = any<Boolean>()
        } answers {
          isPresentationEnabled = firstArg<Boolean>()
        }
        every { presentationMock.icon = any<Icon>() } just Runs
        every { anActionEventMock.presentation } returns presentationMock
      }
      afterEach {
        clearAllMocks()
        unmockkAll()
      }

      should("show the action on update function is triggered for LibraryNode") {
        val nodeMock = mockk<LibraryNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)

        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock
        every { nodeMock.unit.connectionConfig } returns mockk()

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe true }
      }
      should("show the action on update function is triggered for FilesWorkingSetNode") {
        val nodeMock = mockk<FilesWorkingSetNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)

        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock
        every { nodeMock.unit.connectionConfig } returns mockk()

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe true }
      }
      should("show the action on update function is triggered for DSMaskNode") {
        val nodeMock = mockk<DSMaskNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)

        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock
        every { nodeMock.unit.connectionConfig } returns mockk()

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe true }
      }
      should("show the action on update function is triggered for FileLikeDatasetNode") {
        val nodeMock = mockk<FileLikeDatasetNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)

        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock
        every { nodeMock.unit.connectionConfig } returns mockk()

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe true }
      }
      should("not show the action on update function is triggered for JobNode") {
        val nodeMock = mockk<JobNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)

        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock
        every { nodeMock.unit.connectionConfig } returns mockk()

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe false }
      }
      should("not show the action on update function is triggered without selected node") {
        val selectedNodesData = listOf<NodeData<ConnectionConfig>>()

        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly {
          isPresentationEnabledAndVisible shouldBe false
          isPresentationEnabled shouldBe false
        }
      }
      should("not show the action on update function is triggered outside the file explorer view") {
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns null

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe false }
      }
      should("not enable the action on update function is triggered without connection config") {
        val nodeMock = mockk<FilesWorkingSetNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)

        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock
        every { nodeMock.unit.connectionConfig } returns null

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabled shouldBe false }
      }
    }
  }
})
