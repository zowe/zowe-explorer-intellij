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

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.api.ZosmfApi
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Operation
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.JobsRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.dataops.log.JobLogFetcher
import eu.ibagroup.formainframe.dataops.log.MFLogger
import eu.ibagroup.formainframe.dataops.operations.jobs.PurgeJobOperation
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.JesWorkingSetImpl
import eu.ibagroup.formainframe.explorer.ui.EXPLORER_VIEW
import eu.ibagroup.formainframe.explorer.ui.JesExplorerView
import eu.ibagroup.formainframe.explorer.ui.JesFilterNode
import eu.ibagroup.formainframe.explorer.ui.JesWsNode
import eu.ibagroup.formainframe.explorer.ui.JobNode
import eu.ibagroup.formainframe.explorer.ui.NodeData
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestCredentialsServiceImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestZosmfApiImpl
import eu.ibagroup.formainframe.ui.build.jobs.JobBuildTreeView
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import org.junit.jupiter.api.Assertions.assertThrows
import org.zowe.kotlinsdk.CancelJobPurgeOutRequest
import org.zowe.kotlinsdk.JESApi
import org.zowe.kotlinsdk.Job
import org.zowe.kotlinsdk.gson
import retrofit2.Call
import retrofit2.Response

class PurgeJobActionTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }
  context("explorer module: actions/PurgeJobAction") {
    context("actionPerformed") {
      val purgeAction = PurgeJobAction()
      val mockActionEventForJesEx = mockk<AnActionEvent>()
      val jesExplorerView = mockk<JesExplorerView>()
      val job = mockk<Job>()

      every { job.jobName } returns "name"
      every { job.jobId } returns "id"
      val connectionConfig = mockk<ConnectionConfig>()
      every { connectionConfig.uuid } returns "uuid"
      val jobsFilter = spyk(
        JobsFilter(
          "owner",
          "prefix",
          "id"
        )
      )
      mockkObject(gson)
      every { gson.hint(Job::class).fromJson(any<String>(), Job::class.java) } returns job

      val credentialService = CredentialService.getService() as TestCredentialsServiceImpl
      credentialService.testInstance = object : TestCredentialsServiceImpl() {
        override fun getUsernameByKey(connectionConfigUuid: String): String {
          return "user"
        }

        override fun getPasswordByKey(connectionConfigUuid: String): String {
          return "pas"
        }
      }

      every { mockActionEventForJesEx.getExplorerView<JesExplorerView>() } returns jesExplorerView

      val jobNode = mockk<JobNode>()
      val virtualFile = mockk<MFVirtualFile>()

      val nodeData = spyk(
        NodeData(
          jobNode,
          virtualFile,
          null
        )
      )
      every { jesExplorerView.mySelectedNodesData } returns listOf(nodeData)

      val parentNode = mockk<JesFilterNode>()
      val query = spyk(
        UnitRemoteQueryImpl(
          jobsFilter,
          connectionConfig
        )
      )
      every { parentNode.query } returns query
      justRun { parentNode.cleanCache() }

      val jesApi = mockk<JESApi>()
      val call = mockk<Call<List<Job>>>()
      val zosmfApi = ZosmfApi.getService() as TestZosmfApiImpl
      zosmfApi.testInstance = object : TestZosmfApiImpl() {
        override fun <Api : Any> getApi(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api {
          return if (apiClass == JESApi::class.java) {
            jesApi as Api
          } else {
            super.getApi(apiClass, connectionConfig)
          }
        }
      }
      every { jesApi.getFilteredJobs(any(), any(), any(), any(), any(), any(), any(), any()) } returns call

      val response = mockk<Response<List<Job>>>()
      val jobList = mutableListOf(job, job)
      every { call.execute() } returns response
      every {
        hint(List::class)
        response.body()
      } answers {
        if (jobList.isNotEmpty()) {
          jobList.removeAt(0)
          jobList
        } else {
          null
        }
      }
      every { response.isSuccessful } returns true

      every { jobNode.virtualFile } returns virtualFile
      every { jobNode.parent } returns parentNode

      val explorer = mockk<Explorer<ConnectionConfig, JesWorkingSetImpl>>()
      every { jobNode.explorer } returns explorer
      every { explorer.componentManager } returns ApplicationManager.getApplication()

      lateinit var dataOpsManager: TestDataOpsManagerImpl

      val project = mockk<Project>()
      every {
        mockActionEventForJesEx.project
      } returns project

      every {
        jesExplorerView.explorer
      } returns explorer

      val mockRequest = mockk<CancelJobPurgeOutRequest>()
      val jobAttr = spyk(
        RemoteJobAttributes(
          job,
          "test",
          mutableListOf(JobsRequester(connectionConfig, jobsFilter))
        )
      )
      every { jobAttr.clone() } returns jobAttr

      val mockActionEventForJobsLog = mockk<AnActionEvent>()
      val jobsLogView = mockk<JobBuildTreeView>()

      every { mockActionEventForJobsLog.getData(any() as DataKey<Any>) } returns jobsLogView
      every { mockActionEventForJobsLog.project } returns project

      val mockkLogger = mockk<MFLogger<JobLogFetcher>>()
      val mockkFetcher = mockk<JobLogFetcher>()

      every { jobsLogView.getJobLogger() } returns mockkLogger
      every {
        hint(JobLogFetcher::class)
        mockkLogger.logFetcher
      } returns mockkFetcher
      every { mockkFetcher.getCachedJobStatus() } returns job
      every { jobsLogView.getConnectionConfig() } returns connectionConfig

      should("perform purge on job successfully") {

        dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
          override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
            return jobAttr
          }

          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            return mockRequest as R
          }

        }

        var isOperationSucceededForJobsLog = false
        every {
          jobsLogView.showNotification(any(), any(), any(), NotificationType.INFORMATION)
        } answers {
          isOperationSucceededForJobsLog = true
        }

        every { job.status } returns mockk()
        var isEnabledInJobsLog = true
        every { mockActionEventForJobsLog.presentation.isEnabled = false } answers { isEnabledInJobsLog = false }

        purgeAction.actionPerformed(mockActionEventForJobsLog)
        purgeAction.update(mockActionEventForJobsLog)

        assertSoftly {
          isOperationSucceededForJobsLog shouldBe true
          isEnabledInJobsLog shouldBe true
          purgeAction.isDumbAware shouldBe true
        }

      }
      should("perform purge on job with error") {

        dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
          override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
            return jobAttr
          }

          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            throw IllegalStateException("No operation is expected to be performed.")
          }
        }

        var isOperationFailedForJobsLog = false
        every {
          jobsLogView.showNotification(any(), any(), any(), NotificationType.ERROR)
        } answers {
          isOperationFailedForJobsLog = true
        }

        var isOperationFailedForNoContextAction = false
        val mockActionEventWithoutDataContext = mockk<AnActionEvent>()
        every { mockActionEventWithoutDataContext.getData(any() as DataKey<Any>) } returns null
        every { mockActionEventWithoutDataContext.presentation.isEnabledAndVisible = false } answers {
          isOperationFailedForNoContextAction = true
        }

        every { job.status } returns null
        var isEnabledInJobsLog = true
        every { mockActionEventForJobsLog.presentation.isEnabled = false } answers { isEnabledInJobsLog = false }

        purgeAction.actionPerformed(mockActionEventForJobsLog)
        purgeAction.actionPerformed(mockActionEventWithoutDataContext)
        purgeAction.update(mockActionEventWithoutDataContext)
        purgeAction.update(mockActionEventForJobsLog)

        assertSoftly {
          isOperationFailedForJobsLog shouldBe true
          isEnabledInJobsLog shouldBe false
          isOperationFailedForNoContextAction shouldBe true
        }
      }
      unmockkAll()
    }
    context("api spec") {

      val zosmfApi = ZosmfApi.getService() as TestZosmfApiImpl
      zosmfApi.testInstance = mockk()

      val responseMockk = mockk<Response<List<Job>>>()
      every {
        zosmfApi.testInstance.getApi(JESApi::class.java, any())
          .getFilteredJobs(any(), any(), any(), any(), any(), any(), any(), any())
          .execute()
      } returns responseMockk

      val purgeAction = spyk(PurgeJobAction())
      val projectMock = mockk<Project>()
      val mockActionEventForJesEx = mockk<AnActionEvent>()
      every { mockActionEventForJesEx.project } returns projectMock
      every { mockActionEventForJesEx.presentation.text = any() } just Runs
      val jesExplorerView = mockk<JesExplorerView>()
      val explorerMock = mockk<Explorer<ConnectionConfig, JesWorkingSetImpl>>()
      every { mockActionEventForJesEx.getData(EXPLORER_VIEW) } returns jesExplorerView
      every { jesExplorerView.explorer } returns explorerMock
      val connectionConfig = mockk<ConnectionConfig>()
      every { connectionConfig.uuid } returns "uuid"
      every { connectionConfig.authToken } returns "auth_token"
      val diffConnectionConfig = mockk<ConnectionConfig>()
      every { diffConnectionConfig.uuid } returns "diffUuid"

      context("common test spec") {

        val jobParent1 = mockk<JesFilterNode>()
        val jobParentParent1 = mockk<JesWsNode>()
        every { jobParent1.parent } returns jobParentParent1
        every { jobParent1.query } returns mockk()
        every { jobParent1.query?.connectionConfig } returns connectionConfig
        every { jobParent1.query?.request } returns mockk()
        every { jobParent1.query?.request?.prefix } returns "prefix_1"
        every { jobParent1.query?.request?.owner } returns "owner"
        every { jobParent1.query?.request?.userCorrelatorFilter } returns "filter_1"

        every { jobParentParent1.unit } returns mockk()
        every { jobParentParent1.unit.name } returns "firstWS"

        val jobParent2 = mockk<JesFilterNode>()
        val jobParentParent2 = mockk<JesWsNode>()
        every { jobParent2.parent } returns jobParentParent2
        every { jobParent2.query } returns mockk()
        every { jobParent2.query?.connectionConfig } returns connectionConfig
        every { jobParent2.query?.request } returns mockk()
        every { jobParent2.query?.request?.prefix } returns "prefix_2"
        every { jobParent2.query?.request?.owner } returns "owner"
        every { jobParent2.query?.request?.userCorrelatorFilter } returns "filter_2"
        every { jobParentParent2.unit } returns mockk()
        every { jobParentParent2.unit.name } returns "secondWS"

        context("isSelectedJobNodesFromSameWS") {

          val jesWorkingSet1 = mockk<JesWorkingSetImpl>()
          every { jesWorkingSet1.connectionConfig } returns connectionConfig
          val jesWorkingSet2 = mockk<JesWorkingSetImpl>()
          every { jesWorkingSet2.connectionConfig } returns connectionConfig
          val jesWorkingSet3 = mockk<JesWorkingSetImpl>()
          every { jesWorkingSet3.connectionConfig } returns diffConnectionConfig

          val jobNode1 = mockk<JobNode>()
          every { jobNode1.unit } returns jesWorkingSet1
          every { jobNode1.query } returns mockk()
          every { jobNode1.query?.connectionConfig } returns connectionConfig
          every { jobNode1.parent } returns jobParent1
          val jobNode2 = mockk<JobNode>()
          every { jobNode2.unit } returns jesWorkingSet2
          every { jobNode2.query } returns mockk()
          every { jobNode2.query?.connectionConfig } returns connectionConfig
          every { jobNode2.parent } returns jobParent2
          val jobNode3 = mockk<JobNode>()
          every { jobNode3.unit } returns jesWorkingSet3
          every { jobNode3.query } returns mockk()
          every { jobNode3.query?.connectionConfig } returns diffConnectionConfig
          val wrongJobNode4 = mockk<JesFilterNode>()
          every { wrongJobNode4.unit } returns jesWorkingSet3

          val virtualFileMock = mockk<MFVirtualFile>()
          val jobInfo1 = mockk<Job>()
          val jobInfo2 = mockk<Job>()
          every { jobInfo1.jobId } returns "TSU01"
          every { jobInfo2.jobId } returns "TSU02"
          every { jobInfo1.jobName } returns "test1"
          every { jobInfo2.jobName } returns "test2"
          val requester1 = mockk<JobsRequester>()
          val requester2 = mockk<JobsRequester>()
          every { requester1.connectionConfig } returns connectionConfig
          every { requester2.connectionConfig } returns connectionConfig

          val attributes1 = mockk<RemoteJobAttributes>()
          every { attributes1.jobInfo } returns jobInfo1
          every { attributes1.requesters[0] } returns requester1
          val attributes2 = mockk<RemoteJobAttributes>()
          every { attributes2.jobInfo } returns jobInfo2
          every { attributes2.requesters[0] } returns requester2
          val attributes3 = mockk<RemoteJobAttributes>()
          val attributes4 = mockk<RemoteJobAttributes>()

          var mySelectedData: List<NodeData<ConnectionConfig>>
          var isEnableAndVisibleAction: Boolean

          every { mockActionEventForJesEx.presentation.isEnabledAndVisible = false } answers {
            isEnableAndVisibleAction = false
          }
          every { mockActionEventForJesEx.presentation.isEnabledAndVisible = true } answers {
            isEnableAndVisibleAction = true
          }

          should("action is not visible when selected job nodes contains wrong node") {
            isEnableAndVisibleAction = true
            val nodeData1 = NodeData(jobNode1, virtualFileMock, attributes1)
            mockkObject(nodeData1)
            val nodeData2 = NodeData(jobNode2, virtualFileMock, attributes2)
            mockkObject(nodeData2)
            val nodeData3 = NodeData(jobNode3, virtualFileMock, attributes3)
            mockkObject(nodeData3)
            val nodeData4 = NodeData(wrongJobNode4, virtualFileMock, attributes4)
            mockkObject(nodeData4)
            mySelectedData = listOf(nodeData1, nodeData2, nodeData3, nodeData4)
            every { jesExplorerView.mySelectedNodesData } returns mySelectedData
            every { mockActionEventForJesEx.presentation.isEnabledAndVisible } returns false
            purgeAction.update(mockActionEventForJesEx)

            assertSoftly {
              isEnableAndVisibleAction shouldBe false
            }
            unmockkObject(nodeData1, nodeData2, nodeData3, nodeData4)
          }
          should("action is not visible when selected job nodes from different connections") {
            isEnableAndVisibleAction = true
            val nodeData1 = NodeData(jobNode1, virtualFileMock, attributes1)
            mockkObject(nodeData1)
            val nodeData2 = NodeData(jobNode2, virtualFileMock, attributes2)
            mockkObject(nodeData2)
            val nodeData3 = NodeData(jobNode3, virtualFileMock, attributes3)
            mockkObject(nodeData3)
            mySelectedData = listOf(nodeData1, nodeData2, nodeData3)
            every { jesExplorerView.mySelectedNodesData } returns mySelectedData
            purgeAction.update(mockActionEventForJesEx)

            assertSoftly {
              isEnableAndVisibleAction shouldBe false
            }
            unmockkObject(nodeData1, nodeData2, nodeData3)
          }
          should("action is not visible when selected job nodes from different working sets") {
            isEnableAndVisibleAction = true
            val nodeData1 = NodeData(jobNode1, virtualFileMock, attributes1)
            mockkObject(nodeData1)
            val nodeData2 = NodeData(jobNode2, virtualFileMock, attributes2)
            mockkObject(nodeData2)
            mySelectedData = listOf(nodeData1, nodeData2)
            every { jesExplorerView.mySelectedNodesData } returns mySelectedData
            purgeAction.update(mockActionEventForJesEx)

            assertSoftly {
              isEnableAndVisibleAction shouldBe false
            }
            unmockkObject(nodeData1, nodeData2)
          }
          should("action is visible when selected job nodes from the same connections and same working set") {
            isEnableAndVisibleAction = false
            every { jobParentParent1.unit.name } returns "sameWS"
            every { jobParentParent2.unit.name } returns "sameWS"
            val nodeData1 = NodeData(jobNode1, virtualFileMock, attributes1)
            mockkObject(nodeData1)
            val nodeData2 = NodeData(jobNode2, virtualFileMock, attributes2)
            mockkObject(nodeData2)
            mySelectedData = listOf(nodeData1, nodeData2)
            every { jesExplorerView.mySelectedNodesData } returns mySelectedData
            purgeAction.update(mockActionEventForJesEx)

            assertSoftly {
              isEnableAndVisibleAction shouldBe true
            }
            unmockkObject(nodeData1, nodeData2)
          }
          should("purge actionPerformed when jobs haven't been purged due to error") {
            var isJobsPurged = true
            val nodeData1 = NodeData(jobNode1, virtualFileMock, attributes1)
            mockkObject(nodeData1)
            val nodeData2 = NodeData(jobNode2, virtualFileMock, attributes2)
            mockkObject(nodeData2)
            mySelectedData = listOf(nodeData1, nodeData2)
            every { jesExplorerView.mySelectedNodesData } returns mySelectedData
            every {
              explorerMock.showNotification(
                any() as String,
                any() as String,
                any() as NotificationType,
                any() as Project
              )
            } just Runs
            every { responseMockk.isSuccessful } returns true
            every { responseMockk.body() } returns listOf(jobInfo1, jobInfo2)
            every { jobParent1.cleanCache() } answers {
              isJobsPurged = false
            }
            every { jobParent2.cleanCache() } answers {
              isJobsPurged = false
            }
            purgeAction.actionPerformed(mockActionEventForJesEx)

            assertSoftly {
              isJobsPurged shouldBe false
            }
          }
          should("purge actionPerformed when jobs have been purged successfully") {
            var isJobsPurged = false
            val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
            dataOpsManager.testInstance = mockk()
            every {
              dataOpsManager.testInstance.performOperation(
                any() as PurgeJobOperation,
                any() as ProgressIndicator
              )
            } answers {
              isJobsPurged = true
              CancelJobPurgeOutRequest()
            }
            every { responseMockk.isSuccessful } returns true
            every { responseMockk.body() } returns listOf(jobInfo1, jobInfo2)

            purgeAction.actionPerformed(mockActionEventForJesEx)

            assertSoftly {
              isJobsPurged shouldBe true
            }
          }
          should("purge actionPerformed when jobs have been purged successfully, but refresh by filter fails") {
            every { responseMockk.isSuccessful } returns false
            every { responseMockk.body() } returns listOf()
            every { jobParent1.unit } returns mockk()
            every { jobParent1.unit.name } returns "firstFilter"
            every { jobParent2.unit } returns mockk()
            every { jobParent2.unit.name } returns "secondFilter"

            assertThrows(RuntimeException::class.java) {
              purgeAction.actionPerformed(mockActionEventForJesEx)
            }
          }
        }
      }
      unmockkAll()
    }
  }
})
