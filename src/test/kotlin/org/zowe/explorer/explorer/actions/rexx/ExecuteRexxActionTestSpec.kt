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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.explorer.actions.rexx

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.content.synchronizer.checkFileForSync
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.tso.SESSION_EXECUTE_REXX_TOPIC
import org.zowe.explorer.tso.config.TSOSessionConfig
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.getAll
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.dataops.operations.TsoOperation
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.optional
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.TsoResponse
import retrofit2.Response
import java.util.*
import java.util.stream.Stream

class ExecuteRexxActionTestSpec : AppInitShouldSpec("explorer/actions/rexx/ExecuteRexxAction", {
  context("all functions") {
    val classUnderTest = spyk(ExecuteRexxAction())

    val connectionConfigMock = mockk<ConnectionConfig> {
      every { uuid } returns "test_uuid"
      every { url } returns "TEST_URL"
    }
    val presentationMock = mockk<Presentation>(relaxUnitFun = true)
    val explorerViewMock = mockk<FileExplorerView>()
    val actionEventMock = mockk<AnActionEvent> {
      every { project } returns ProjectManager.getInstance().defaultProject
      every { presentation } returns presentationMock
      every { getData(EXPLORER_VIEW) } returns explorerViewMock
    }

    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "test"
    every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()

    val configService = ConfigService.getService()
    every { configService.crudable } returns mockk {
      every {
        getByUniqueKey(ConnectionConfig::class.java, any<ConnectionConfig>())
      } returns connectionConfigMock.optional
    }

    context("actionPerformed") {
      var notificationShowedForTsoSessionError = false
      var notificationShowedForConnConfigNotFound = false
      var notificationShowedForMemberIsNotRexx = false
      var notificationShowedForMemberContentFetchError = false

      val node = mockk<FileLikeDatasetNode> {
        every { unit } returns mockk {
          every { connectionConfig } returns connectionConfigMock
        }
        every { parent } returns mockk {
          every { virtualFile } returns mockk {
            every { filenameInternal } returns "ARST.REXX"
          }
        }
        every { virtualFile } returns mockk {
          every { name } returns "SAMPLE"
        }
      }
      val nodeDataForTest = NodeData(
        node = node,
        file = mockk<MFVirtualFile>(),
        attributes = mockk<RemoteMemberAttributes>()
      )

      every { explorerViewMock.mySelectedNodesData } returns mutableListOf(nodeDataForTest)

      // data api mockk behavior
      val responseBody = "/* SOME CONTENT WITH REXX COMMENT FIRST */\n" + "TEST PGM\n" + "END"
      val dataApiResponse = mockk<Response<String>> {
        every { isSuccessful } returns true
        every { body() } returns responseBody
      }
      val mockedCall = mockk<retrofit2.Call<String>> {
        every { execute() } returns dataApiResponse
      }
      val dataApi = mockk<DataAPI> {
        every {
          retrieveMemberContent(any<String>(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns mockedCall
      }

      val zosmfApi = ZosmfApi.getService()
      every { zosmfApi.getApi(DataAPI::class.java, any<ConnectionConfig>()) } returns dataApi

      // notificationService mockk behavior
      val notificationsService = NotificationsService.getService()
      every {
        notificationsService
          .notifyError(any<Throwable>(), any<Project>(), any<String>(), any<String>(), any<String>())
      } answers {
        val throwable = firstArg<Throwable>()
        val custDetailsShort = args[3] as String?
        if (throwable.message == "FAILED TO ESTABLISH RUNTIME SESSION" ) {
          notificationShowedForTsoSessionError = true
        } else if (custDetailsShort == "Connection config was not found") {
          notificationShowedForConnConfigNotFound = true
        } else if (custDetailsShort == "Member is not REXX") {
          notificationShowedForMemberIsNotRexx = true
        } else {
          notificationShowedForMemberContentFetchError = true
        }
      }

      // dialog mockk behavior
      val tsoSessionConfig = mockk<TSOSessionConfig> {
        every { connectionConfigUuid } returns "conn_uuid"
        every { name } returns "TSO_Config_Name"
      }
      val dialogState = ExecuteRexxDialogState(tsoSessionConfig)
      val dialogMock = mockk<ExecuteRexxDialog>()

      mockkStatic(ExecuteRexxDialog::class)
      mockkObject(ExecuteRexxDialog)
      every {
        ExecuteRexxDialog
          .create(any<Project>(), any<Crudable>(), any<ConnectionConfig>(), any<ExecuteRexxDialogState>())
      } returns dialogMock

      // checkFileForSync mockk behavior
      mockkStatic(::checkFileForSync)
      every { checkFileForSync(any(), any(), any()) } returns false

      val dataOpsManager = DataOpsManager.getService()

      beforeEach {
        clearMocks(dialogMock, verificationMarks = true, recordedCalls = true)

        every { dialogMock.showAndGet() } returns true
        every { dialogMock.state } returns dialogState

        every {
          dataOpsManager.performOperation(any<TsoOperation>(), any<ProgressIndicator>())
        } returns TsoResponse(servletKey = "TEST KEY")
      }

      should("execute actionPerformed and send execution topic to sync publisher") {
        classUnderTest.actionPerformed(actionEventMock)

        verify { dialogMock.showAndGet() }
        verify { classUnderTest.logMessage("About to send $SESSION_EXECUTE_REXX_TOPIC topic to sync publisher") }
      }

      should("execute actionPerformed and call notifyError if start TSO session throws exception") {
        every {
          dataOpsManager.performOperation(any<TsoOperation>(), any<ProgressIndicator>())
        } throws IllegalArgumentException("FAILED TO ESTABLISH RUNTIME SESSION")
        every { dialogMock.showAndGet() } returns true
        every { dialogMock.state } returns dialogState

        classUnderTest.actionPerformed(actionEventMock)

        verify { dialogMock.showAndGet() }
        assertSoftly { notificationShowedForTsoSessionError shouldBe true }
      }

      should("execute actionPerformed and call notifyError if connection config was not found for TSO session") {
        every {
          configService.crudable.getByUniqueKey(ConnectionConfig::class.java, any<ConnectionConfig>())
        } returns Optional.ofNullable(null)
        every { dialogMock.showAndGet() } returns true
        every { dialogMock.state } returns dialogState

        classUnderTest.actionPerformed(actionEventMock)

        verify { dialogMock.showAndGet() }
        assertSoftly { notificationShowedForConnConfigNotFound shouldBe true }
      }

      should("execute actionPerformed and call notifyError if member is not REXX") {
        every {
          dataApi
            .retrieveMemberContent(any<String>(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns mockk {
          every { execute() } returns mockk {
            every { isSuccessful } returns true
            every { body() } returns "/* SOME CONTENT WITH NO PGM COMMENT FIRST */\n" + "TEST PGM\n" + "END"
          }
        }

        classUnderTest.actionPerformed(actionEventMock)

        verify { dialogMock wasNot Called }
        assertSoftly { notificationShowedForMemberIsNotRexx shouldBe true }
      }

      should("execute actionPerformed and call notifyError if API call failed for member content fetching") {
        every { dataApiResponse.isSuccessful } returns false
        every {
          dataApi
            .retrieveMemberContent(any<String>(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns mockedCall

        classUnderTest.actionPerformed(actionEventMock)

        verify { dialogMock wasNot Called }
        assertSoftly { notificationShowedForMemberContentFetchError shouldBe true }
      }

      should("execute actionPerformed and do nothing if vFile is currently under synchronization") {
        var isFileUnderSync = false

        every {
          checkFileForSync(any(), any(), any())
        } answers {
          isFileUnderSync = true
          true
        }

        classUnderTest.actionPerformed(actionEventMock)

        verify { dialogMock wasNot Called }
        assertSoftly { isFileUnderSync shouldBe true }
      }
    }

    context("update") {
      var isVisibleNewValue: Boolean? = null
      var isEnabledNewValue: Boolean? = null
      val tsoSessionConfig = mockk<TSOSessionConfig>()
      val nodeDataForPositiveTest = NodeData(
        node = mockk<FileLikeDatasetNode>(),
        file = mockk<MFVirtualFile>(),
        attributes = mockk<RemoteMemberAttributes>()
      )

      every {
        presentationMock.isVisible = any()
      } answers {
        isVisibleNewValue = firstArg<Boolean>()
      }
      every {
        presentationMock.isEnabled = any()
      } answers {
        isEnabledNewValue = firstArg<Boolean>()
      }
      every {
        presentationMock.isEnabledAndVisible = any()
      } answers {
        presentationMock.isVisible = firstArg<Boolean>()
        presentationMock.isEnabled = firstArg<Boolean>()
      }

      beforeEach {
        isVisibleNewValue = null
        isEnabledNewValue = null
      }

      should("action should be enabled and visible if all conditions met") {
        every { explorerViewMock.mySelectedNodesData } returns mutableListOf(nodeDataForPositiveTest)
        every { configService.crudable.getAll<TSOSessionConfig>() } returns Stream.of(tsoSessionConfig)

        classUnderTest.update(actionEventMock)

        assertSoftly {
          isVisibleNewValue shouldBe true
          isEnabledNewValue shouldBe true
        }
      }

      should("action should be visible, but not enabled if no TSO sessions defined") {
        every { explorerViewMock.mySelectedNodesData } returns mutableListOf(nodeDataForPositiveTest)
        every { configService.crudable.getAll(TSOSessionConfig::class.java) } returns Stream.of()

        classUnderTest.update(actionEventMock)

        assertSoftly {
          isVisibleNewValue shouldBe true
          isEnabledNewValue shouldBe false
        }
      }

      should("action should not be visible if attributes of selected node is not RemoteMemberAttributes") {
        val nodeDataForBadAttributesTest = NodeData(
          node = mockk<FileLikeDatasetNode>(),
          file = mockk<MFVirtualFile>(),
          attributes = mockk<RemoteUssAttributes>()
        )

        every { explorerViewMock.mySelectedNodesData } returns mutableListOf(nodeDataForBadAttributesTest)
        every { configService.crudable.getAll<TSOSessionConfig>() } returns Stream.of()

        classUnderTest.update(actionEventMock)

        assertSoftly {
          isVisibleNewValue shouldBe false
          isEnabledNewValue shouldBe false
        }
      }

      should("action should not be visible and not be enabled if selectedNodesData contains 2 nodes") {
        val nodeData1ForMoreThan1NodeTest = NodeData(
          node = mockk<FileLikeDatasetNode>(),
          file = mockk<MFVirtualFile>(),
          attributes = mockk<RemoteMemberAttributes>()
        )
        val nodeData2ForMoreThan1NodeTest = NodeData(
          node = mockk<FileLikeDatasetNode>(),
          file = mockk<MFVirtualFile>(),
          attributes = mockk<RemoteMemberAttributes>()
        )

        every { explorerViewMock.mySelectedNodesData } returns mutableListOf(nodeData1ForMoreThan1NodeTest, nodeData2ForMoreThan1NodeTest)

        classUnderTest.update(actionEventMock)

        assertSoftly {
          isVisibleNewValue shouldBe false
          isEnabledNewValue shouldBe false
        }
      }

      should("action should not be visible and not be enabled if explorer view is not FileExplorerView") {
        every { actionEventMock.getData(EXPLORER_VIEW) } returns null

        classUnderTest.update(actionEventMock)

        assertSoftly {
          isVisibleNewValue shouldBe false
          isEnabledNewValue shouldBe false
        }
      }
    }
  }
})
