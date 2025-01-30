/*
 * Copyright (c) 2024 IBA Group.
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

package eu.ibagroup.formainframe.explorer.actions.rexx

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Key
import eu.ibagroup.formainframe.api.ZosmfApi
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Operation
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.service.SyncProcessService
import eu.ibagroup.formainframe.dataops.content.synchronizer.checkFileForSync
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.telemetry.NotificationsService
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.*
import eu.ibagroup.formainframe.tso.SESSION_EXECUTE_REXX_TOPIC
import eu.ibagroup.formainframe.tso.config.TSOSessionConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.TsoResponse
import retrofit2.Response
import java.util.stream.Stream

class ExecuteRexxActionTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("explorer module: actions/rexx/ExecuteRexxAction") {
    val classUnderTest = spyk(ExecuteRexxAction())
    val project = ProjectManager.getInstance().defaultProject
    val presentationMock = mockk<Presentation>()
    val actionEventMock = mockk<AnActionEvent>()
    val explorerViewMock = mockk<FileExplorerView>()
    val connectionConfigMock = mockk<ConnectionConfig>()

    val configService = ConfigService.getService() as TestConfigServiceImpl
    every { actionEventMock.project } returns project
    every { actionEventMock.presentation } returns presentationMock
    every { presentationMock.putClientProperty(any() as Key<String>, any() as String) } just Runs
    every { actionEventMock.getExplorerView<FileExplorerView>() } returns explorerViewMock
    every { configService.crudable.getByUniqueKey<ConnectionConfig>(any()) } returns connectionConfigMock
    every { connectionConfigMock.authToken } returns "AUTH_TOKEN"
    every { connectionConfigMock.url } returns "TEST_URL"

    context("actionPerformed") {
      var notificationShowedForTsoSessionError = false
      var notificationShowedForConnConfigNotFound = false
      var notificationShowedForMemberIsNotRexx = false
      var notificationShowedForMemberContentFetchError = false


      val node = mockk<FileLikeDatasetNode>()
      val virtualFile = mockk<MFVirtualFile>()
      val attributes = mockk<RemoteMemberAttributes>()
      val nodeDataForTest = NodeData(node = node, file = virtualFile, attributes = attributes)

      every { explorerViewMock.mySelectedNodesData } returns mutableListOf(nodeDataForTest)

      // node mockk behavior
      every { node.unit } returns mockk()
      every { node.unit.connectionConfig } returns connectionConfigMock
      every { node.parent } returns mockk()
      every { node.parent?.virtualFile } returns mockk()
      every { node.parent?.virtualFile?.filenameInternal } returns "ARST.REXX"
      every { node.virtualFile } returns mockk()
      every { node.virtualFile.name } returns "SAMPLE"

      // data api mockk behavior
      val dataApi = mockk<DataAPI>()
      val zosmfApi = ZosmfApi.getService() as TestZosmfApiImpl
      zosmfApi.testInstance = object : TestZosmfApiImpl() {
        override fun <Api : Any> getApi(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api {
          @Suppress("UNCHECKED_CAST")
          return dataApi as Api
        }
      }

      // notificationService mockk behavior
      val notificationsService = NotificationsService.getService() as TestNotificationsServiceImpl
      notificationsService.testInstance = object : TestNotificationsServiceImpl() {
        override fun notifyError(
          t: Throwable,
          project: Project?,
          custTitle: String?,
          custDetailsShort: String?,
          custDetailsLong: String?
        ) {
          if (t.message == "FAILED TO ESTABLISH RUNTIME SESSION" ) {
            notificationShowedForTsoSessionError = true
          } else if (custDetailsShort == "Connection config was not found") {
            notificationShowedForConnConfigNotFound = true
          } else if (custDetailsShort == "Member is not REXX") {
            notificationShowedForMemberIsNotRexx = true
          } else {
            notificationShowedForMemberContentFetchError = true
          }
        }
      }

      // dataOps manager config mockk behavior
      val tsoStartResponse = TsoResponse(servletKey = "TEST KEY")
      val dataOpsManager = ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
      dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
        override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
          @Suppress("UNCHECKED_CAST")
          return tsoStartResponse as R
        }
      }

      // response from data api mockk behavior
      val responseBody = "/* SOME CONTENT WITH REXX COMMENT FIRST */\n" + "TEST PGM\n" + "END"
      val mockedCall = mockk<retrofit2.Call<String>>()
      val dataApiResponse = mockk<Response<String>>()
      every { mockedCall.execute() } returns dataApiResponse
      every { dataApiResponse.isSuccessful } returns true
      every { dataApiResponse.body() } returns responseBody
      every { dataApi.retrieveMemberContent(any<String>(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())} returns mockedCall

      // dialog mockk behavior
      val tsoSessionConfig = mockk<TSOSessionConfig>()
      val dialogState = ExecuteRexxDialogState(tsoSessionConfig)
      val dialogMock = mockk<ExecuteRexxDialog>()
      mockkStatic(ExecuteRexxDialog::class)
      mockkObject(ExecuteRexxDialog)
      every { dialogMock.showAndGet() } returns true
      every { ExecuteRexxDialog.create(any() as Project, any() as Crudable, any() as ConnectionConfig,any() as ExecuteRexxDialogState ) } returns dialogMock
      every { dialogMock.state } returns dialogState
      every { tsoSessionConfig.connectionConfigUuid } returns "conn_uuid"
      every { tsoSessionConfig.name } returns "TSO_Config_Name"

      // checkFileForSync mockk behavior
      mockkStatic(::checkFileForSync)
      every { checkFileForSync(any(), any(), any()) } returns false

      should("execute actionPerformed and send execution topic to sync publisher") {
        classUnderTest.actionPerformed(actionEventMock)
        verify { dialogMock.showAndGet() }
        verify { classUnderTest.logMessage("About to send $SESSION_EXECUTE_REXX_TOPIC topic to sync publisher") }
      }

      should("execute actionPerformed and call notifyError if start TSO session throws exception") {
        clearMocks(dialogMock, verificationMarks = true, recordedCalls = true)
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            throw IllegalArgumentException("FAILED TO ESTABLISH RUNTIME SESSION")
          }
        }
        every { dialogMock.showAndGet() } returns true
        every { dialogMock.state } returns dialogState

        classUnderTest.actionPerformed(actionEventMock)
        verify { dialogMock.showAndGet() }
        assertSoftly {
          notificationShowedForTsoSessionError shouldBe true
        }
      }

      should("execute actionPerformed and call notifyError if connection config was not found for TSO session") {
        clearMocks(dialogMock, verificationMarks = true, recordedCalls = true)
        every { configService.crudable.getByUniqueKey<ConnectionConfig>(any()) } returns null
        every { dialogMock.showAndGet() } returns true
        every { dialogMock.state } returns dialogState

        classUnderTest.actionPerformed(actionEventMock)
        verify { dialogMock.showAndGet() }
        assertSoftly {
          notificationShowedForConnConfigNotFound shouldBe true
        }
      }

      should("execute actionPerformed and call notifyError if member is not REXX") {
        clearMocks(dialogMock, verificationMarks = true, recordedCalls = true)
        val responseBodyBad = "/* SOME CONTENT WITH NO PGM COMMENT FIRST */\n" + "TEST PGM\n" + "END"
        val mockedCallBad = mockk<retrofit2.Call<String>>()
        val dataApiResponseBad = mockk<Response<String>>()
        every { mockedCallBad.execute() } returns dataApiResponseBad
        every { dataApiResponseBad.isSuccessful } returns true
        every { dataApiResponseBad.body() } returns responseBodyBad
        every { dataApi.retrieveMemberContent(any<String>(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())} returns mockedCallBad

        classUnderTest.actionPerformed(actionEventMock)
        verify { dialogMock wasNot Called }
        assertSoftly {
          notificationShowedForMemberIsNotRexx shouldBe true
        }
      }

      should("execute actionPerformed and call notifyError if API call failed for member content fetching") {
        clearMocks(dialogMock, verificationMarks = true, recordedCalls = true)
        every { dataApiResponse.isSuccessful } returns false
        every { dataApi.retrieveMemberContent(any<String>(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())} returns mockedCall

        classUnderTest.actionPerformed(actionEventMock)
        verify { dialogMock wasNot Called }
        assertSoftly {
          notificationShowedForMemberContentFetchError shouldBe true
        }
      }

      should("execute actionPerformed and do nothing if vFile is currently under synchronization") {
        var isFileUnderSync = false
        clearMocks(dialogMock, verificationMarks = true, recordedCalls = true)
        every { checkFileForSync(any(), any(), any()) } answers {
          isFileUnderSync = true
          true
        }

        classUnderTest.actionPerformed(actionEventMock)
        verify { dialogMock wasNot Called }
        assertSoftly {
          isFileUnderSync shouldBe true
        }
      }
    }

    context("update") {
      var isVisible: Boolean
      var isEnabled: Boolean
      val tsoSessionConfig = mockk<TSOSessionConfig>()
      val node = mockk<FileLikeDatasetNode>()
      val virtualFile = mockk<MFVirtualFile>()
      val attributes = mockk<RemoteMemberAttributes>()
      val nodeDataForPositiveTest = NodeData(node = node, file = virtualFile, attributes = attributes)

      every { presentationMock.setVisible(true) } answers { isVisible = true }
      every { presentationMock.setEnabled(true) } answers { isEnabled = true }
      every { presentationMock.setVisible(false) } answers { isVisible = false }
      every { presentationMock.setEnabled(false) } answers { isEnabled = false }
      every { presentationMock.setEnabledAndVisible(false) } answers {
        isVisible = false
        isEnabled = false
      }
      every { presentationMock.setEnabledAndVisible(true) } answers {
        isVisible = true
        isEnabled = true
      }

      should("action should be enabled and visible if all conditions met") {
        isVisible = false
        isEnabled = false

        every { presentationMock.isEnabled } returns true
        every { explorerViewMock.mySelectedNodesData } returns mutableListOf(nodeDataForPositiveTest)
        every { configService.crudable.getAll<TSOSessionConfig>() } returns Stream.of(tsoSessionConfig)

        classUnderTest.update(actionEventMock)

        assertSoftly {
          isVisible shouldBe true
          isEnabled shouldBe true
        }
      }

      should("action should be visible, but not enabled if no TSO sessions defined") {
        isVisible = false
        isEnabled = true

        every { presentationMock.isEnabled } returns false
        every { explorerViewMock.mySelectedNodesData } returns mutableListOf(nodeDataForPositiveTest)
        every { configService.crudable.getAll<TSOSessionConfig>() } returns Stream.of()

        classUnderTest.update(actionEventMock)

        assertSoftly {
          isVisible shouldBe true
          isEnabled shouldBe false
        }
      }

      should("action should not be visible if attributes of selected node is not RemoteMemberAttributes") {
        isVisible = true
        isEnabled = true

        val nodeBad = mockk<FileLikeDatasetNode>()
        val virtualFileBad = mockk<MFVirtualFile>()
        val attributesBad = mockk<RemoteUssAttributes>()
        val nodeDataForBadAttributesTest = NodeData(node = nodeBad, file = virtualFileBad, attributes = attributesBad)

        every { presentationMock.isEnabled } returns false
        every { explorerViewMock.mySelectedNodesData } returns mutableListOf(nodeDataForBadAttributesTest)
        every { configService.crudable.getAll<TSOSessionConfig>() } returns Stream.of()

        classUnderTest.update(actionEventMock)

        assertSoftly {
          isVisible shouldBe false
          isEnabled shouldBe false
        }
      }

      should("action should not be visible and not be enabled if selectedNodesData contains 2 nodes") {
        isVisible = true
        isEnabled = true

        val node1 = mockk<FileLikeDatasetNode>()
        val node2 = mockk<FileLikeDatasetNode>()
        val virtualFile1 = mockk<MFVirtualFile>()
        val virtualFile2 = mockk<MFVirtualFile>()
        val attributes1 = mockk<RemoteMemberAttributes>()
        val attributes2 = mockk<RemoteMemberAttributes>()
        val nodeData1ForMoreThan1NodeTest = NodeData(node = node1, file = virtualFile1, attributes = attributes1)
        val nodeData2ForMoreThan1NodeTest = NodeData(node = node2, file = virtualFile2, attributes = attributes2)

        every { explorerViewMock.mySelectedNodesData } returns mutableListOf(nodeData1ForMoreThan1NodeTest, nodeData2ForMoreThan1NodeTest)

        classUnderTest.update(actionEventMock)

        assertSoftly {
          isVisible shouldBe false
          isEnabled shouldBe false
        }
      }

      should("action should not be visible and not be enabled if explorer view is not FileExplorerView") {
        isVisible = true
        isEnabled = true

        every { actionEventMock.getExplorerView<FileExplorerView>() } returns null

        classUnderTest.update(actionEventMock)

        assertSoftly {
          isVisible shouldBe false
          isEnabled shouldBe false
        }
      }
    }

    context("misc") {
      should("isDumbAware should be true") {
        val isDumbAware = classUnderTest.isDumbAware
        assertSoftly {
          isDumbAware shouldBe true
        }
      }

      should("getActionUpdateThread should be EDT") {
        val actionUpdateThread = classUnderTest.actionUpdateThread
        assertSoftly {
          actionUpdateThread shouldBe ActionUpdateThread.EDT
        }
      }
    }
  }
})