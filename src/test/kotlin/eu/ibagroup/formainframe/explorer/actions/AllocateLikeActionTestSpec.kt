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
import com.intellij.openapi.ui.Messages.showWarningDialog
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.AnalyticsEvent
import eu.ibagroup.formainframe.common.ui.StatefulDialog
import eu.ibagroup.formainframe.common.ui.cleanInvalidateOnExpand
import eu.ibagroup.formainframe.common.ui.showUntilDone
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Operation
import eu.ibagroup.formainframe.dataops.attributes.MaskedRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.dataops.operations.DatasetAllocationParams
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.telemetry.NotificationsService
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestAnalyticsServiceImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestNotificationsServiceImpl
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.kotlinsdk.*
import java.util.*
import javax.swing.Icon
import kotlin.reflect.KFunction

class AllocateLikeActionTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }
  context("explorer module: actions/AllocateLikeAction") {
    val anActionEventMock = mockk<AnActionEvent>()
    val allocateDsActionInst = AllocateLikeAction()

    context("actionPerformed") {
      var isCleanInvalidateOnExpandTriggered = false
      var isAnalitycsTracked = false
      var isThrowableReported = false
      val filesWorkingSetConfigMock = mockk<FilesWorkingSetConfig>()
      val componentManagerMock = mockk<ComponentManager>()
      val explorerMock = mockk<Explorer<ConnectionConfig, *>>()
      lateinit var addMaskActionInst: AnAction

      val analyticsService = AnalyticsService.getService() as TestAnalyticsServiceImpl
      analyticsService.testInstance = object : TestAnalyticsServiceImpl() {
        override fun trackAnalyticsEvent(event: AnalyticsEvent) {
          isAnalitycsTracked = true
          super.trackAnalyticsEvent(event)
        }
      }

      val notificationsService = NotificationsService.getService() as TestNotificationsServiceImpl

      beforeEach {
        isCleanInvalidateOnExpandTriggered = false
        isAnalitycsTracked = false
        isThrowableReported = false

        every { anActionEventMock.project } returns mockk()

        every { explorerMock.componentManager } returns componentManagerMock

        mockkStatic("eu.ibagroup.formainframe.explorer.ui.ExplorerTreeViewKt")

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

      should("perform allocate PS dataset action without creating a new dataset mask") {
        val viewMock = mockk<FileExplorerView>()
        val workingSetMock = mockk<FilesWorkingSet>()
        val nodeMock = mockk<LibraryNode>()
        val dsInfo = Dataset()
        val dsAttributesMock = mockk<RemoteDatasetAttributes>()
        val nodeDataMock = NodeData(nodeMock, null, dsAttributesMock)
        val selectedNodesData = listOf(nodeDataMock)
        val dsMaskNodeMock = mockk<DSMaskNode>()
        lateinit var initState: DatasetAllocationParams
        var isOperationPerformed = false
        var isShowUntilDoneSucceeded = false
        var isUpdateOnConfigCrudableCalled = false

        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock
        every { dsAttributesMock.datasetInfo } returns dsInfo

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
        val connectionConfig = mockk<ConnectionConfig>()
        every { connectionConfig.uuid } returns "fake_uuid"
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns connectionConfig
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
          isThrowableReported shouldBe false
          initState.errorMessage shouldBe ""
          isUpdateOnConfigCrudableCalled shouldBe true
        }
      }
      should("perform allocate PDS dataset with TRACKS action without creating a new dataset mask") {
        val viewMock = mockk<FileExplorerView>()
        val workingSetMock = mockk<FilesWorkingSet>()
        val nodeMock = mockk<LibraryNode>()
        val dsInfo = Dataset(
          datasetOrganization = DatasetOrganization.PO,
          spaceUnits = SpaceUnits.TRACKS,
          recordFormat = RecordFormat.F,
          sizeInTracks = 10
        )
        val dsAttributesMock = mockk<RemoteDatasetAttributes>()
        val nodeDataMock = NodeData(nodeMock, null, dsAttributesMock)
        val selectedNodesData = listOf(nodeDataMock)
        val dsMaskNodeMock = mockk<DSMaskNode>()
        lateinit var initState: DatasetAllocationParams
        var isOperationPerformed = false
        var isShowUntilDoneSucceeded = false

        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock
        every { dsAttributesMock.datasetInfo } returns dsInfo

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

        every { dsMaskNodeMock.cleanCache(any(), any(), any(), any()) } returns Unit
        every { nodeMock.parent } returns dsMaskNodeMock
        every { nodeMock.hint(FilesWorkingSet::class).unit } returns workingSetMock
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { workingSetMock.name } returns "test"
        every { workingSetMock.uuid } returns "test"
        val connectionConfig = mockk<ConnectionConfig>()
        every { connectionConfig.uuid } returns "fake_uuid"
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns connectionConfig
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
          isThrowableReported shouldBe false
          initState.errorMessage shouldBe ""
        }
      }
      should("perform allocate PDS/E dataset with CYLINDERS action without creating a new dataset mask") {
        val viewMock = mockk<FileExplorerView>()
        val workingSetMock = mockk<FilesWorkingSet>()
        val nodeMock = mockk<LibraryNode>()
        val dsInfo = Dataset(
          datasetOrganization = DatasetOrganization.POE,
          spaceUnits = SpaceUnits.CYLINDERS,
          recordFormat = RecordFormat.F,
          sizeInTracks = 30
        )
        val dsAttributesMock = mockk<RemoteDatasetAttributes>()
        val nodeDataMock = NodeData(nodeMock, null, dsAttributesMock)
        val selectedNodesData = listOf(nodeDataMock)
        val dsMaskNodeMock = mockk<DSMaskNode>()
        lateinit var initState: DatasetAllocationParams
        var isOperationPerformed = false
        var isShowUntilDoneSucceeded = false

        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock
        every { dsAttributesMock.datasetInfo } returns dsInfo

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

        every { dsMaskNodeMock.cleanCache(any(), any(), any(), any()) } returns Unit
        every { nodeMock.parent } returns dsMaskNodeMock
        every { nodeMock.hint(FilesWorkingSet::class).unit } returns workingSetMock
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { workingSetMock.name } returns "test"
        every { workingSetMock.uuid } returns "test"
        val connectionConfig = mockk<ConnectionConfig>()
        every { connectionConfig.uuid } returns "fake_uuid"
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns connectionConfig
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
          isThrowableReported shouldBe false
          initState.errorMessage shouldBe ""
        }
      }
      should("perform allocate PS dataset with BLOCKS action without creating a new dataset mask, changing BLOCKS to TRACKS") {
        val viewMock = mockk<FileExplorerView>()
        val workingSetMock = mockk<FilesWorkingSet>()
        val nodeMock = mockk<LibraryNode>()
        val dsInfo = Dataset(spaceUnits = SpaceUnits.BLOCKS)
        val dsAttributesMock = mockk<RemoteDatasetAttributes>()
        val nodeDataMock = NodeData(nodeMock, null, dsAttributesMock)
        val selectedNodesData = listOf(nodeDataMock)
        val dsMaskNodeMock = mockk<DSMaskNode>()
        lateinit var initState: DatasetAllocationParams
        var isOperationPerformed = false
        var isShowUntilDoneSucceeded = false
        var isBlocksChangedToTracks = false

        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock
        every { dsAttributesMock.datasetInfo } returns dsInfo

        val showWarningDialogMock: (String, String) -> Unit = ::showWarningDialog
        mockkStatic(showWarningDialogMock as KFunction<*>)
        every {
          hint(Unit::class)
          showWarningDialogMock(any<String>(), any<String>())
        } answers {
          isBlocksChangedToTracks = true
        }

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

        every { dsMaskNodeMock.cleanCache(any(), any(), any(), any()) } returns Unit
        every { nodeMock.parent } returns dsMaskNodeMock
        every { nodeMock.hint(FilesWorkingSet::class).unit } returns workingSetMock
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { workingSetMock.name } returns "test"
        every { workingSetMock.uuid } returns "test"
        val connectionConfig = mockk<ConnectionConfig>()
        every { connectionConfig.uuid } returns "fake_uuid"
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns connectionConfig
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
          isBlocksChangedToTracks shouldBe true
          isThrowableReported shouldBe false
          initState.errorMessage shouldBe ""
        }
      }
      should("not perform 'allocate like' action as the file explorer view is not found") {
        var isOperationPerformed = false

        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns null

        val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            isOperationPerformed = true
            return true as R
          }
        }

        allocateDsActionInst.actionPerformed(anActionEventMock)

        assertSoftly {
          isCleanInvalidateOnExpandTriggered shouldBe false
          isAnalitycsTracked shouldBe false
          isOperationPerformed shouldBe false
          isThrowableReported shouldBe false
        }
      }
    }
    context("update") {
      val presentationMock = mockk<Presentation>()
      var isPresentationEnabledAndVisible = false

      beforeEach {
        every {
          presentationMock.isEnabledAndVisible = any<Boolean>()
        } answers {
          isPresentationEnabledAndVisible = firstArg<Boolean>()
        }
        every { presentationMock.icon = any<Icon>() } just Runs
        every { anActionEventMock.presentation } returns presentationMock
      }
      afterEach {
        clearAllMocks()
        unmockkAll()
      }

      should("show the 'allocate like' action on update function is triggered for LibraryNode") {
        val viewMock = mockk<FileExplorerView>()
        val nodeMock = mockk<LibraryNode>()
        val dsAttributesMock = mockk<RemoteDatasetAttributes>()
        val nodeDataMock = NodeData(nodeMock, null, dsAttributesMock)
        val selectedNodesData = listOf(nodeDataMock)

        every { dsAttributesMock.hasDsOrg } returns true
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe true }
      }
      should("not show the 'allocate like' action as there are more than one nodes selected") {
        val viewMock = mockk<FileExplorerView>()
        val nodeMock = mockk<LibraryNode>()
        val dsAttributesMock = mockk<RemoteDatasetAttributes>()
        val nodeDataMock = NodeData(nodeMock, null, dsAttributesMock)
        val selectedNodesData = listOf(nodeDataMock, nodeDataMock)

        every { dsAttributesMock.isMigrated } returns false
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe false }
      }
      should("not show the 'allocate like' action as the selected dataset is migrated") {
        val dsInfo = mockk<Dataset>()
        every { dsInfo.migrated } returns HasMigrated.YES

        val nodeMock = mockk<LibraryNode>()

        val requesters = mockk<MutableList<MaskedRequester>>()
        val dsAttributesMock = spyk(RemoteDatasetAttributes(dsInfo, "test", requesters))

        val nodeDataMock = NodeData(nodeMock, null, dsAttributesMock)
        val selectedNodesData = listOf(nodeDataMock)
        val viewMock = mockk<FileExplorerView>()

        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe false }
      }
      should("not show the 'allocate like' action as the selected dataset does not have dataset organization") {
        val dsInfo = mockk<Dataset>()
        every { dsInfo.migrated } returns HasMigrated.NO
        every { dsInfo.datasetOrganization } returns null

        val nodeMock = mockk<LibraryNode>()

        val requesters = mockk<MutableList<MaskedRequester>>()
        val dsAttributesMock = spyk(RemoteDatasetAttributes(dsInfo, "test", requesters))

        val nodeDataMock = NodeData(nodeMock, null, dsAttributesMock)
        val selectedNodesData = listOf(nodeDataMock)
        val viewMock = mockk<FileExplorerView>()

        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe false }
      }
      should("not show the 'allocate like' action as the selected node is not a dataset") {
        val viewMock = mockk<FileExplorerView>()
        val nodeMock = mockk<JobNode>()
        val dsAttributesMock = mockk<RemoteJobAttributes>()
        val nodeDataMock = NodeData(nodeMock, null, dsAttributesMock)
        val selectedNodesData = listOf(nodeDataMock)

        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe false }
      }
      should("not show the 'allocate like' action as the view is not a file explorer view") {
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns null

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe false }
      }
    }
  }
})
