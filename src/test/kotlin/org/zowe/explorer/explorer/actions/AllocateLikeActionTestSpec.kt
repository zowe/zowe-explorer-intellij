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
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages.showWarningDialog
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.common.ui.StatefulDialog
import org.zowe.explorer.common.ui.cleanInvalidateOnExpand
import org.zowe.explorer.common.ui.showUntilDone
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.MaskedRequester
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.operations.DatasetAllocationOperation
import org.zowe.explorer.dataops.operations.DatasetAllocationParams
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.kotlinsdk.*
import java.util.*
import kotlin.reflect.KFunction

class AllocateLikeActionTestSpec : AppInitShouldSpec("explorer/actions/AllocateLikeAction", {
  val allocateDsActionInst = AllocateLikeAction()

  context("actionPerformed") {
    var isCleanInvalidateOnExpandTriggered = false
    var isThrowableReported = false
    var isOperationPerformed = false
    var isShowUntilDoneSucceeded = false

    val anActionEventMock = mockk<AnActionEvent> {
      every { project } returns mockk()
    }
    val configServiceCrudableMock = mockk<Crudable> {
      every {
        getByUniqueKey(FilesWorkingSetConfig::class.java, any<String>())
      } returns Optional.of(mockk())
    }
    val viewMock = mockk<FileExplorerView>()
    val libraryNodeMock = mockk<LibraryNode> {
      every { parent } returns mockk<DSMaskNode>(relaxUnitFun = true)
      every { unit } returns mockk {
        every { name } returns "test"
        every { uuid } returns "test"
        every { connectionConfig } returns mockk {
          every { uuid } returns "fake_uuid"
        }
        every { explorer } returns mockk {
          every { componentManager } returns mockk()
        }
      }
    }
    val dsAttributesMock = mockk<RemoteDatasetAttributes>()

    lateinit var addMaskActionInst: AnAction
    lateinit var initState: DatasetAllocationParams

    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "test"
    every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()

    val notificationsService = NotificationsService.getService()
    every {
      notificationsService.notifyError(any<Throwable>(), any<Project>(), any<String>(), any<String>(), any<String>())
    } answers {
      isThrowableReported = true
    }

    val dataOpsManager = DataOpsManager.getService()
    every {
      dataOpsManager.performOperation(any<DatasetAllocationOperation>(), any<ProgressIndicator>())
    } answers {
      isOperationPerformed = true
    }

    val configService = ConfigService.getService()
    every { configService.crudable } returns configServiceCrudableMock

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
      isShowUntilDoneSucceeded = false

      every { viewMock.mySelectedNodesData } returns listOf(NodeData(libraryNodeMock, null, dsAttributesMock))
      every { anActionEventMock.getData(EXPLORER_VIEW) } returns viewMock
      every { dsAttributesMock.datasetInfo } returns mockk()
      every { configServiceCrudableMock.update(any<FilesWorkingSetConfig>()) } returns Optional.of(mockk())

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
    }

    should("perform allocate PS dataset action without creating a new dataset mask") {
      var isUpdateOnConfigCrudableCalled = false

      val dsInfo = Dataset()

      every {
        configServiceCrudableMock.update(any<FilesWorkingSetConfig>())
      } answers {
        isUpdateOnConfigCrudableCalled = true
        Optional.of(mockk())
      }

      every { dsAttributesMock.datasetInfo } returns dsInfo


      allocateDsActionInst.actionPerformed(anActionEventMock)
      addMaskActionInst.actionPerformed(anActionEventMock)

      assertSoftly {
        isCleanInvalidateOnExpandTriggered shouldBe true
        isShowUntilDoneSucceeded shouldBe true
        isOperationPerformed shouldBe true
        isThrowableReported shouldBe false
        initState.errorMessage shouldBe ""
        isUpdateOnConfigCrudableCalled shouldBe true
      }
    }
    should("perform allocate PDS dataset with TRACKS action without creating a new dataset mask") {
      val dsInfo = Dataset(
        datasetOrganization = DatasetOrganization.PO,
        spaceUnits = SpaceUnits.TRACKS,
        recordFormat = RecordFormat.F,
        sizeInTracks = 10
      )

      every { dsAttributesMock.datasetInfo } returns dsInfo

      allocateDsActionInst.actionPerformed(anActionEventMock)

      assertSoftly {
        isCleanInvalidateOnExpandTriggered shouldBe true
        isShowUntilDoneSucceeded shouldBe true
        isOperationPerformed shouldBe true
        isThrowableReported shouldBe false
        initState.errorMessage shouldBe ""
      }
    }
    should("perform allocate PDS/E dataset with CYLINDERS action without creating a new dataset mask") {
      val dsInfo = Dataset(
        datasetOrganization = DatasetOrganization.POE,
        spaceUnits = SpaceUnits.CYLINDERS,
        recordFormat = RecordFormat.F,
        sizeInTracks = 30
      )

      every { dsAttributesMock.datasetInfo } returns dsInfo

      every { viewMock.mySelectedNodesData } returns listOf(NodeData(libraryNodeMock, null, dsAttributesMock))

      allocateDsActionInst.actionPerformed(anActionEventMock)

      assertSoftly {
        isCleanInvalidateOnExpandTriggered shouldBe true
        isShowUntilDoneSucceeded shouldBe true
        isOperationPerformed shouldBe true
        isThrowableReported shouldBe false
        initState.errorMessage shouldBe ""
      }
    }
    should("perform allocate PS dataset with BLOCKS action without creating a new dataset mask, changing BLOCKS to TRACKS") {
      var isBlocksChangedToTracks = false

      val dsInfo = Dataset(spaceUnits = SpaceUnits.BLOCKS)

      val showWarningDialogMock: (String, String) -> Unit = ::showWarningDialog
      mockkStatic(showWarningDialogMock as KFunction<*>)
      every {
        showWarningDialogMock(any<String>(), any<String>())
      } answers {
        isBlocksChangedToTracks = true
      }

      every { dsAttributesMock.datasetInfo } returns dsInfo

      every { viewMock.mySelectedNodesData } returns listOf(NodeData(libraryNodeMock, null, dsAttributesMock))

      allocateDsActionInst.actionPerformed(anActionEventMock)

      assertSoftly {
        isCleanInvalidateOnExpandTriggered shouldBe true
        isShowUntilDoneSucceeded shouldBe true
        isOperationPerformed shouldBe true
        isBlocksChangedToTracks shouldBe true
        isThrowableReported shouldBe false
        initState.errorMessage shouldBe ""
      }
    }
    should("not perform 'allocate like' action as the file explorer view is not found") {
      every { anActionEventMock.getData(EXPLORER_VIEW) } returns null

      allocateDsActionInst.actionPerformed(anActionEventMock)

      assertSoftly {
        isCleanInvalidateOnExpandTriggered shouldBe false
        isOperationPerformed shouldBe false
        isThrowableReported shouldBe false
      }
    }
  }

  context("update") {
    var isPresentationEnabledAndVisible = false

    val presentationMock = mockk<Presentation>(relaxUnitFun = true) {
      every {
        isEnabledAndVisible = any<Boolean>()
      } answers {
        isPresentationEnabledAndVisible = firstArg<Boolean>()
      }
    }
    val viewMock = mockk<FileExplorerView>()
    val anActionEventMock = mockk<AnActionEvent> {
      every { project } returns mockk()
    }

    beforeEach {
      isPresentationEnabledAndVisible = false

      every { viewMock.mySelectedNodesData } returns listOf()
      every { anActionEventMock.presentation } returns presentationMock
      every { anActionEventMock.getData(EXPLORER_VIEW) } returns viewMock
    }

    should("show the 'allocate like' action on update function is triggered for LibraryNode") {
      val dsAttributesMock = mockk<RemoteDatasetAttributes> {
        every { hasDsOrg } returns true
      }
      val selectedNodesData = listOf(NodeData(mockk<LibraryNode>(), null, dsAttributesMock))

      every { viewMock.mySelectedNodesData } returns selectedNodesData

      allocateDsActionInst.update(anActionEventMock)

      assertSoftly { isPresentationEnabledAndVisible shouldBe true }
    }

    should("not show the 'allocate like' action as there are more than one nodes selected") {
      val dsAttributesMock = mockk<RemoteDatasetAttributes>()
      val nodeDataMock = NodeData(mockk<LibraryNode>(), null, dsAttributesMock)
      val selectedNodesData = listOf(nodeDataMock, nodeDataMock)

      every { dsAttributesMock.isMigrated } returns false
      every { viewMock.mySelectedNodesData } returns selectedNodesData

      allocateDsActionInst.update(anActionEventMock)

      assertSoftly { isPresentationEnabledAndVisible shouldBe false }
    }

    should("not show the 'allocate like' action as the selected dataset is migrated") {
      val dsInfo = mockk<Dataset> {
        every { migrated } returns HasMigrated.YES
      }
      val requesters = mockk<MutableList<MaskedRequester>>()
      val dsAttributesMock = spyk(RemoteDatasetAttributes(dsInfo, "test", requesters))

      every {
        viewMock.mySelectedNodesData
      } returns listOf(NodeData(mockk<LibraryNode>(), null, dsAttributesMock))

      allocateDsActionInst.update(anActionEventMock)

      assertSoftly { isPresentationEnabledAndVisible shouldBe false }
    }

    should("not show the 'allocate like' action as the selected dataset does not have dataset organization") {
      val dsInfo = mockk<Dataset> {
        every { migrated } returns HasMigrated.NO
        every { datasetOrganization } returns null
      }
      val requesters = mockk<MutableList<MaskedRequester>>()
      val dsAttributesMock = spyk(RemoteDatasetAttributes(dsInfo, "test", requesters))

      every {
        viewMock.mySelectedNodesData
      } returns listOf(NodeData(mockk<LibraryNode>(), null, dsAttributesMock))

      allocateDsActionInst.update(anActionEventMock)

      assertSoftly { isPresentationEnabledAndVisible shouldBe false }
    }

    should("not show the 'allocate like' action as the selected node is not a dataset") {
      val dsAttributesMock = mockk<RemoteJobAttributes>()

      every { viewMock.mySelectedNodesData } returns listOf(NodeData(mockk<JobNode>(), null, dsAttributesMock))

      allocateDsActionInst.update(anActionEventMock)

      assertSoftly { isPresentationEnabledAndVisible shouldBe false }
    }

    should("not show the 'allocate like' action as the view is not a file explorer view") {
      every { anActionEventMock.getData(EXPLORER_VIEW) } returns null

      allocateDsActionInst.update(anActionEventMock)

      assertSoftly { isPresentationEnabledAndVisible shouldBe false }
    }
  }
})
