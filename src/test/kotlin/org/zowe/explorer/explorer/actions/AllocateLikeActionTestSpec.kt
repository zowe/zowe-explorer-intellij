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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages.showWarningDialog
import com.intellij.openapi.ui.showOkNoDialog
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import org.zowe.explorer.common.ui.StatefulDialog
import org.zowe.explorer.common.ui.cleanInvalidateOnExpand
import org.zowe.explorer.common.ui.showUntilDone
import org.zowe.explorer.config.configCrudable
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.operations.DatasetAllocationParams
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.DSMaskNode
import org.zowe.explorer.explorer.ui.ExplorerTreeNode
import org.zowe.explorer.explorer.ui.ExplorerTreeView
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.JobNode
import org.zowe.explorer.explorer.ui.LibraryNode
import org.zowe.explorer.explorer.ui.NodeData
import org.zowe.explorer.explorer.ui.getExplorerView
import org.zowe.explorer.utils.service
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
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
import org.zowe.kotlinsdk.Dataset
import org.zowe.kotlinsdk.DatasetOrganization
import org.zowe.kotlinsdk.RecordFormat
import org.zowe.kotlinsdk.SpaceUnits
import java.util.*
import javax.swing.Icon
import javax.swing.SwingUtilities
import kotlin.reflect.KFunction

class AllocateLikeActionTestSpec : ShouldSpec({
  beforeSpec {
    // FIXTURE SETUP TO HAVE ACCESS TO APPLICATION INSTANCE
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
    val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor, "for-mainframe")
    val fixture = fixtureBuilder.fixture
    val myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
      fixture,
      LightTempDirTestFixtureImpl(true)
    )
    myFixture.setUp()
  }
  afterSpec {
    clearAllMocks()
  }
  context("explorer module: actions/AllocateLikeAction") {
    val anActionEventMock = mockk<AnActionEvent>()
    val allocateDsActionInst = AllocateLikeAction()

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
        var isCleanCacheTriggered = false
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
          assertSoftly { isThrowableReported shouldBe false }
          assertSoftly { initState.errorMessage shouldBe "" }
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
        var isCleanCacheTriggered = false
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
          assertSoftly { isThrowableReported shouldBe false }
          assertSoftly { initState.errorMessage shouldBe "" }
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
        var isCleanCacheTriggered = false
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
          assertSoftly { isThrowableReported shouldBe false }
          assertSoftly { initState.errorMessage shouldBe "" }
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
        var isCleanCacheTriggered = false
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
          assertSoftly { isBlocksChangedToTracks shouldBe true }
          assertSoftly { isThrowableReported shouldBe false }
          assertSoftly { initState.errorMessage shouldBe "" }
        }
      }
      should("not perform 'allocate like' action as the file explorer view is not found") {
        var isOperationPerformed = false

        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns null

        every {
          dataOpsManagerMock.hint(Boolean::class).performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } answers {
          isOperationPerformed = true
          true
        }

        allocateDsActionInst.actionPerformed(anActionEventMock)

        // Pause to wait until all EDT events are finished
        SwingUtilities.invokeAndWait {
          assertSoftly { isCleanInvalidateOnExpandTriggered shouldBe false }
          assertSoftly { isOperationPerformed shouldBe false }
          assertSoftly { isThrowableReported shouldBe false }
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

        every { dsAttributesMock.isMigrated } returns false
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
        val viewMock = mockk<FileExplorerView>()
        val nodeMock = mockk<LibraryNode>()
        val dsAttributesMock = mockk<RemoteDatasetAttributes>()
        val nodeDataMock = NodeData(nodeMock, null, dsAttributesMock)
        val selectedNodesData = listOf(nodeDataMock)

        every { dsAttributesMock.isMigrated } returns true
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
