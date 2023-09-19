/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.showOkNoDialog
import org.zowe.explorer.common.ui.StatefulDialog
import org.zowe.explorer.common.ui.cleanInvalidateOnExpand
import org.zowe.explorer.common.ui.showUntilDone
import org.zowe.explorer.config.configCrudable
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.operations.DatasetAllocationParams
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.DSMaskNode
import org.zowe.explorer.explorer.ui.ExplorerTreeNode
import org.zowe.explorer.explorer.ui.ExplorerTreeView
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.FileLikeDatasetNode
import org.zowe.explorer.explorer.ui.FilesWorkingSetNode
import org.zowe.explorer.explorer.ui.JobNode
import org.zowe.explorer.explorer.ui.LibraryNode
import org.zowe.explorer.explorer.ui.NodeData
import org.zowe.explorer.explorer.ui.getExplorerView
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.fail
import org.zowe.kotlinsdk.DatasetOrganization
import org.zowe.kotlinsdk.DsnameType
import java.util.*
import javax.swing.Icon
import javax.swing.SwingUtilities
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
      var isThrowableReported = false
      val filesWorkingSetConfigMock = mockk<FilesWorkingSetConfig>()
      val dataOpsManagerMock = mockk<DataOpsManager>()
      val componentManagerMock = mockk<ComponentManager>()
      val explorerMock = mockk<Explorer<ConnectionConfig, *>>()

      beforeEach {
        isCleanInvalidateOnExpandTriggered = false
        isThrowableReported = false

        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock
        every { anActionEventMock.project } returns mockk()

        every { explorerMock.componentManager } returns componentManagerMock

        every {
          explorerMock.reportThrowable(any<Throwable>(), any<Project>())
        } answers {
          isThrowableReported = true
        }

        every {
          componentManagerMock.hint(DataOpsManager::class).getService(DataOpsManager::class.java)
        } returns dataOpsManagerMock

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
        var isCleanCacheTriggered = false
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

        mockkObject(configCrudable)
        every {
          configCrudable.getByUniqueKey<FilesWorkingSetConfig, String>(any(), any())
        } returns Optional.of(filesWorkingSetConfigMock)

        every {
          dsMaskNodeMock.cleanCache(any(), any(), any(), any())
        } answers {
          isCleanCacheTriggered = true
          val isSendTopic = lastArg<Boolean>()
          if (isSendTopic) {
            fail("cleanCache should not send topic in this testcase")
          }
        }
        every { nodeMock.parent } returns dsMaskNodeMock
        every { nodeMock.hint(FilesWorkingSet::class).unit } returns workingSetMock
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { workingSetMock.name } returns "test"
        every { workingSetMock.uuid } returns "test"
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns mockk<ConnectionConfig>()
        every {
          dataOpsManagerMock.hint(Boolean::class).performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } answers {
          isOperationPerformed = true
          true
        }
        every { workingSetMock.explorer } returns explorerMock
        every { configCrudable.update(any(), any()) } answers {
          isUpdateOnConfigCrudableCalled = true
          Optional.of(mockk())
        }

        val showOkNoDialogMock: (
          String,
          String,
          Project?,
          String,
          String,
          Icon?
        ) -> Boolean = ::showOkNoDialog
        mockkStatic(showOkNoDialogMock as KFunction<*>)
        every {
          hint(Boolean::class)
          showOkNoDialogMock(any<String>(), any<String>(), any<Project>(), any<String>(), any<String>(), any())
        } answers {
          true
        }

        allocateDsActionInst.actionPerformed(anActionEventMock)

        // Pause to wait until all EDT events are finished
        SwingUtilities.invokeAndWait {
          assertSoftly { isCleanInvalidateOnExpandTriggered shouldBe true }
          assertSoftly { isShowUntilDoneSucceeded shouldBe true }
          assertSoftly { isOperationPerformed shouldBe true }
          assertSoftly { isCleanCacheTriggered shouldBe true }
          assertSoftly { isUpdateOnConfigCrudableCalled shouldBe true }
          assertSoftly { isThrowableReported shouldBe false }
          assertSoftly { initState.errorMessage shouldBe "" }
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
        var isCleanCacheTriggered = false
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
          initState.allocationParameters.managementClass = "test"
          initState.allocationParameters.storageClass = "test"
          initState.allocationParameters.deviceType = "test"
          initState.allocationParameters.dataClass = "test"
          initState.allocationParameters.volumeSerial = "test"
          initState.allocationParameters.directoryBlocks = 1
          isShowUntilDoneSucceeded = thirdBlockResult(initState)
          initState
        }

        mockkObject(configCrudable)
        every {
          configCrudable.getByUniqueKey<FilesWorkingSetConfig, String>(any(), any())
        } returns Optional.of(filesWorkingSetConfigMock)

        every {
          dsMaskNodeMock.cleanCache(any(), any(), any(), any())
        } answers {
          isCleanCacheTriggered = true
          val isSendTopic = lastArg<Boolean>()
          if (isSendTopic) {
            fail("cleanCache should not send topic in this testcase")
          }
        }
        every { nodeMock.parent } returns dsMaskNodeMock
        every { nodeMock.hint(FilesWorkingSet::class).unit } returns workingSetMock
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { workingSetMock.name } returns "test"
        every { workingSetMock.uuid } returns "test"
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns mockk<ConnectionConfig>()
        every {
          dataOpsManagerMock.hint(Boolean::class).performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } answers {
          isOperationPerformed = true
          true
        }
        every { workingSetMock.explorer } returns explorerMock
        every { configCrudable.update(any(), any()) } answers {
          isUpdateOnConfigCrudableCalled = true
          Optional.of(mockk())
        }

        val showOkNoDialogMock: (
          String,
          String,
          Project?,
          String,
          String,
          Icon?
        ) -> Boolean = ::showOkNoDialog
        mockkStatic(showOkNoDialogMock as KFunction<*>)
        every {
          hint(Boolean::class)
          showOkNoDialogMock(any<String>(), any<String>(), any<Project>(), any<String>(), any<String>(), any())
        } answers {
          true
        }

        allocateDsActionInst.actionPerformed(anActionEventMock)

        // Pause to wait until all EDT events are finished
        SwingUtilities.invokeAndWait {
          assertSoftly { isCleanInvalidateOnExpandTriggered shouldBe true }
          assertSoftly { isShowUntilDoneSucceeded shouldBe true }
          assertSoftly { isOperationPerformed shouldBe true }
          assertSoftly { isCleanCacheTriggered shouldBe true }
          assertSoftly { isUpdateOnConfigCrudableCalled shouldBe true }
          assertSoftly { isThrowableReported shouldBe false }
          assertSoftly { initState.errorMessage shouldBe "" }
          assertSoftly { initState.allocationParameters.directoryBlocks shouldBe null }
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
        var isCleanCacheTriggered = false
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

        mockkObject(configCrudable)
        every {
          configCrudable.getByUniqueKey<FilesWorkingSetConfig, String>(any(), any())
        } returns Optional.of(filesWorkingSetConfigMock)

        every {
          dsMaskNodeMock.cleanCache(any(), any(), any(), any())
        } answers {
          isCleanCacheTriggered = true
          val isSendTopic = lastArg<Boolean>()
          if (isSendTopic) {
            fail("cleanCache should not send topic in this testcase")
          }
        }
        every { nodeMock.parent } returns dsMaskNodeMock
        every { nodeMock.hint(FilesWorkingSet::class).unit } returns workingSetMock
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { workingSetMock.name } returns "test"
        every { workingSetMock.uuid } returns "test"
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns mockk<ConnectionConfig>()
        every {
          dataOpsManagerMock.hint(Boolean::class).performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } answers {
          isOperationPerformed = true
          true
        }
        every { workingSetMock.explorer } returns explorerMock
        every { configCrudable.update(any(), any()) } answers {
          isUpdateOnConfigCrudableCalled = true
          Optional.of(mockk())
        }

        val showOkNoDialogMock: (
          String,
          String,
          Project?,
          String,
          String,
          Icon?
        ) -> Boolean = ::showOkNoDialog
        mockkStatic(showOkNoDialogMock as KFunction<*>)
        every {
          hint(Boolean::class)
          showOkNoDialogMock(any<String>(), any<String>(), any<Project>(), any<String>(), any<String>(), any())
        } answers {
          true
        }

        allocateDsActionInst.actionPerformed(anActionEventMock)

        // Pause to wait until all EDT events are finished
        SwingUtilities.invokeAndWait {
          assertSoftly { isCleanInvalidateOnExpandTriggered shouldBe true }
          assertSoftly { isShowUntilDoneSucceeded shouldBe true }
          assertSoftly { isOperationPerformed shouldBe true }
          assertSoftly { isCleanCacheTriggered shouldBe true }
          assertSoftly { isUpdateOnConfigCrudableCalled shouldBe true }
          assertSoftly { isThrowableReported shouldBe false }
          assertSoftly { initState.errorMessage shouldBe "" }
          assertSoftly { initState.allocationParameters.datasetOrganization shouldBe DatasetOrganization.PO }
          assertSoftly { initState.allocationParameters.dsnType shouldBe DsnameType.LIBRARY }
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
        var isCleanCacheTriggered = false
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

        mockkObject(configCrudable)
        every {
          configCrudable.getByUniqueKey<FilesWorkingSetConfig, String>(any(), any())
        } returns Optional.of(filesWorkingSetConfigMock)

        every {
          dsMaskNodeMock.cleanCache(any(), any(), any(), any())
        } answers {
          isCleanCacheTriggered = true
          val isSendTopic = lastArg<Boolean>()
          if (!isSendTopic) {
            fail("cleanCache should send topic in this testcase")
          }
        }
        every { nodeMock.parent } returns dsMaskNodeMock
        every { nodeMock.hint(FilesWorkingSet::class).unit } returns workingSetMock
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { workingSetMock.name } returns "test"
        every { workingSetMock.uuid } returns "test"
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns mockk<ConnectionConfig>()
        every {
          dataOpsManagerMock.hint(Boolean::class).performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } answers {
          isOperationPerformed = true
          true
        }
        every { workingSetMock.explorer } returns explorerMock
        every { configCrudable.update(any(), any()) } answers {
          isUpdateOnConfigCrudableCalled = true
          Optional.of(mockk())
        }

        val showOkNoDialogMock: (
          String,
          String,
          Project?,
          String,
          String,
          Icon?
        ) -> Boolean = ::showOkNoDialog
        mockkStatic(showOkNoDialogMock as KFunction<*>)
        every {
          hint(Boolean::class)
          showOkNoDialogMock(any<String>(), any<String>(), any<Project>(), any<String>(), any<String>(), any())
        } answers {
          false
        }

        allocateDsActionInst.actionPerformed(anActionEventMock)

        // Pause to wait until all EDT events are finished
        SwingUtilities.invokeAndWait {
          assertSoftly { isCleanInvalidateOnExpandTriggered shouldBe true }
          assertSoftly { isShowUntilDoneSucceeded shouldBe true }
          assertSoftly { isOperationPerformed shouldBe true }
          assertSoftly { isCleanCacheTriggered shouldBe true }
          assertSoftly { isUpdateOnConfigCrudableCalled shouldBe false }
          assertSoftly { isThrowableReported shouldBe false }
          assertSoftly { initState.errorMessage shouldBe "" }
        }
      }
      should("perform allocate dataset action without refreshing dataset mask as the selected node is a working set") {
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

        mockkObject(configCrudable)
        every {
          configCrudable.getByUniqueKey<FilesWorkingSetConfig, String>(any(), any())
        } returns Optional.of(filesWorkingSetConfigMock)

        every { nodeMock.parent } returns null
        every { nodeMock.hint(FilesWorkingSet::class).unit } returns workingSetMock
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { workingSetMock.name } returns "test"
        every { workingSetMock.uuid } returns "test"
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns mockk<ConnectionConfig>()
        every {
          dataOpsManagerMock.hint(Boolean::class).performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } answers {
          isOperationPerformed = true
          true
        }
        every { workingSetMock.explorer } returns explorerMock
        every { configCrudable.update(any(), any()) } answers {
          isUpdateOnConfigCrudableCalled = true
          Optional.of(mockk())
        }

        val showOkNoDialogMock: (
          String,
          String,
          Project?,
          String,
          String,
          Icon?
        ) -> Boolean = ::showOkNoDialog
        mockkStatic(showOkNoDialogMock as KFunction<*>)
        every {
          hint(Boolean::class)
          showOkNoDialogMock(any<String>(), any<String>(), any<Project>(), any<String>(), any<String>(), any())
        } answers {
          true
        }

        allocateDsActionInst.actionPerformed(anActionEventMock)

        // Pause to wait until all EDT events are finished
        SwingUtilities.invokeAndWait {
          assertSoftly { isCleanInvalidateOnExpandTriggered shouldBe false }
          assertSoftly { isShowUntilDoneSucceeded shouldBe true }
          assertSoftly { isOperationPerformed shouldBe true }
          assertSoftly { isUpdateOnConfigCrudableCalled shouldBe true }
          assertSoftly { isThrowableReported shouldBe false }
          assertSoftly { initState.errorMessage shouldBe "" }
        }
      }
      should("perform allocate dataset action creating new dataset mask without refreshing the existing on as the connection config is not found") {
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

        mockkObject(configCrudable)
        every {
          configCrudable.getByUniqueKey<FilesWorkingSetConfig, String>(any(), any())
        } returns Optional.ofNullable(null)

        every { nodeMock.parent } returns null
        every { nodeMock.hint(FilesWorkingSet::class).unit } returns workingSetMock
        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { workingSetMock.name } returns "test"
        every { workingSetMock.uuid } returns "test"
        every { workingSetMock.hint(ConnectionConfig::class).connectionConfig } returns mockk<ConnectionConfig>()
        every {
          dataOpsManagerMock.hint(Boolean::class).performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } answers {
          isOperationPerformed = true
          true
        }
        every { workingSetMock.explorer } returns explorerMock
        every { configCrudable.update(any(), any()) } answers {
          isUpdateOnConfigCrudableCalled = true
          Optional.of(mockk())
        }

        val showOkNoDialogMock: (
          String,
          String,
          Project?,
          String,
          String,
          Icon?
        ) -> Boolean = ::showOkNoDialog
        mockkStatic(showOkNoDialogMock as KFunction<*>)
        every {
          hint(Boolean::class)
          showOkNoDialogMock(any<String>(), any<String>(), any<Project>(), any<String>(), any<String>(), any())
        } answers {
          true
        }

        allocateDsActionInst.actionPerformed(anActionEventMock)

        // Pause to wait until all EDT events are finished
        SwingUtilities.invokeAndWait {
          assertSoftly { isCleanInvalidateOnExpandTriggered shouldBe false }
          assertSoftly { isShowUntilDoneSucceeded shouldBe true }
          assertSoftly { isOperationPerformed shouldBe true }
          assertSoftly { isUpdateOnConfigCrudableCalled shouldBe false }
          assertSoftly { isThrowableReported shouldBe false }
          assertSoftly { initState.errorMessage shouldBe "" }
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
        every {
          dataOpsManagerMock.hint(Boolean::class).performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } answers {
          throw Exception(exceptionMsg)
        }
        every { workingSetMock.explorer } returns explorerMock

        allocateDsActionInst.actionPerformed(anActionEventMock)

        // Pause to wait until all EDT events are finished
        SwingUtilities.invokeAndWait {
          assertSoftly { isCleanInvalidateOnExpandTriggered shouldBe false }
          assertSoftly { isShowUntilDoneSucceeded shouldBe false }
          assertSoftly { isThrowableReported shouldBe true }
          assertSoftly { initState.errorMessage shouldBe exceptionMsg }
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

      should("show the action on update function is triggered for LibraryNode") {
        val nodeMock = mockk<LibraryNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)

        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe true }
      }
      should("show the action on update function is triggered for FilesWorkingSetNode") {
        val nodeMock = mockk<FilesWorkingSetNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)

        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe true }
      }
      should("show the action on update function is triggered for DSMaskNode") {
        val nodeMock = mockk<DSMaskNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)

        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe true }
      }
      should("show the action on update function is triggered for FileLikeDatasetNode") {
        val nodeMock = mockk<FileLikeDatasetNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)

        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe true }
      }
      should("not show the action on update function is triggered for JobNode") {
        val nodeMock = mockk<JobNode>()
        val nodeDataMock = NodeData(nodeMock, null, null)
        val selectedNodesData = listOf(nodeDataMock)

        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe false }
      }
      should("not show the action on update function is triggered without selected node") {
        val selectedNodesData = listOf<NodeData<ConnectionConfig>>()

        every { viewMock.mySelectedNodesData } returns selectedNodesData
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns viewMock

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe false }
      }
      should("not show the action on update function is triggered outside the file explorer view") {
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns null

        allocateDsActionInst.update(anActionEventMock)

        assertSoftly { isPresentationEnabledAndVisible shouldBe false }
      }
    }
  }
})
