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
 *   Dzianis Lisiankou
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.explorer.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.ws.JobsFilter
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.attributes.JobsRequester
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.log.JobLogFetcher
import org.zowe.explorer.dataops.log.MFLogger
import org.zowe.explorer.dataops.operations.jobs.PurgeJobOperation
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.JesWorkingSetImpl
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.JesExplorerView
import org.zowe.explorer.explorer.ui.JesFilterNode
import org.zowe.explorer.explorer.ui.JesWsNode
import org.zowe.explorer.explorer.ui.JobNode
import org.zowe.explorer.explorer.ui.NodeData
import org.zowe.explorer.ui.build.jobs.JobBuildTreeView
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertThrows
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.kotlinsdk.CancelJobPurgeOutRequest
import org.zowe.kotlinsdk.JESApi
import org.zowe.kotlinsdk.Job
import org.zowe.kotlinsdk.gson
import retrofit2.Response

class PurgeJobActionTestSpec : AppInitShouldSpec("explorer/actions/PurgeJobAction", {
  context("all functions") {
    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "user"
    every { credentialService.getPasswordByKey(any<String>()) } returns "pas".toCharArray()

    val zosmfApi = ZosmfApi.getService()

    val dataOpsManager = DataOpsManager.getService()

    beforeEach {
      every { zosmfApi.getApi(JESApi::class.java, any<ConnectionConfig>()) } returns mockk()

      every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns mockk()
      every { dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>()) } returns mockk()
    }

    context("actionPerformed") {
      val purgeAction = PurgeJobAction()
      val job = mockk<Job> {
        every { jobName } returns "name"
        every { jobId } returns "id"
      }
      val connectionConfig = mockk<ConnectionConfig> {
        every { uuid } returns "uuid"
      }
      val jobsFilter = spyk(
        JobsFilter(
          "owner",
          "prefix",
          "id"
        )
      )

      mockkObject(gson)
      every { gson.hint(Job::class).fromJson(any<String>(), Job::class.java) } returns job

      val mockProject = mockk<Project>()

      val jobList = mutableListOf(job, job)
      val mockRequest = mockk<CancelJobPurgeOutRequest>()
      val jobAttr = spyk(
        RemoteJobAttributes(
          job,
          "test",
          mutableListOf(JobsRequester(connectionConfig, jobsFilter))
        )
      ) {
        every { clone() } returns this@spyk
      }

      val mockkLogger = mockk<MFLogger<JobLogFetcher>>()
      val jobsLogView = mockk<JobBuildTreeView> {
        every { getJobLogger() } returns mockkLogger
        every { getConnectionConfig() } returns connectionConfig
      }
      val mockActionEventForJobsLog = mockk<AnActionEvent> {
        every { getData(any<DataKey<Any>>()) } returns jobsLogView
        every { project } returns mockProject
      }

      val mockkFetcher = mockk<JobLogFetcher> {
        every { getCachedJobStatus() } returns job
      }
      every {
        hint(JobLogFetcher::class)
        mockkLogger.logFetcher
      } returns mockkFetcher

      beforeEach {
        every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns jobAttr

        every { zosmfApi.getApi(JESApi::class.java, any<ConnectionConfig>()) } returns mockk {
          every { getFilteredJobs(any(), any(), any(), any(), any(), any(), any(), any()) } returns mockk {
            every { execute() } returns mockk {
              every { isSuccessful } returns true
              every {
                body()
              } answers {
                if (jobList.isNotEmpty()) {
                  jobList.removeAt(0)
                  jobList
                } else {
                  null
                }
              }
            }
          }
        }
      }

      should("perform purge on job successfully") {
        var isOperationSucceededForJobsLog = false
        var isEnabledInJobsLog = true

        every { dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>()) } returns mockRequest

        every {
          jobsLogView.showNotification(any(), any(), any(), NotificationType.INFORMATION)
        } answers {
          isOperationSucceededForJobsLog = true
        }

        every { job.status } returns mockk()

        every { mockActionEventForJobsLog.presentation } returns mockk {
          every {
            isEnabled = false
          } answers {
            isEnabledInJobsLog = false
          }
        }

        purgeAction.actionPerformed(mockActionEventForJobsLog)
        purgeAction.update(mockActionEventForJobsLog)

        assertSoftly {
          isOperationSucceededForJobsLog shouldBe true
          isEnabledInJobsLog shouldBe true
          purgeAction.isDumbAware shouldBe true
        }
      }

      should("perform purge on job with error") {
        var isOperationFailedForJobsLog = false
        var isOperationFailedForNoContextAction = false
        var isEnabledInJobsLog = true

        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } throws IllegalStateException("No operation is expected to be performed.")

        every {
          jobsLogView.showNotification(any(), any(), any(), NotificationType.ERROR)
        } answers {
          isOperationFailedForJobsLog = true
        }

        val mockActionEventWithoutDataContext = mockk<AnActionEvent> {
          every { getData(any<DataKey<Any>>()) } returns null
          every { presentation } returns mockk {
            every {
              isEnabledAndVisible = false
            } answers {
              isOperationFailedForNoContextAction = true
            }
          }
        }

        every { job.status } returns null
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
    }

    context("api spec") {
      val responseMockk = mockk<Response<List<Job>>>()
      val purgeAction = spyk(PurgeJobAction())
      val projectMock = mockk<Project>()
      val explorerMock = mockk<Explorer<ConnectionConfig, JesWorkingSetImpl>>(relaxUnitFun = true)
      val jesExplorerView = mockk<JesExplorerView> {
        every { explorer } returns explorerMock
      }
      val presentationMock = mockk<Presentation>(relaxUnitFun = true)
      val mockActionEventForJesEx = mockk<AnActionEvent> {
        every { project } returns projectMock
        every { presentation } returns presentationMock
        every { getData(EXPLORER_VIEW) } returns jesExplorerView
      }
      val connectionConfigMock = mockk<ConnectionConfig> {
        every { uuid } returns "uuid"
      }
      val diffConnectionConfig = mockk<ConnectionConfig> {
        every { uuid } returns "diffUuid"
      }

      beforeEach {
        every { zosmfApi.getApi(JESApi::class.java, any<ConnectionConfig>()) } returns mockk {
          every { getFilteredJobs(any(), any(), any(), any(), any(), any(), any(), any()) } returns mockk {
            every { execute() } returns responseMockk
          }
        }
      }

      context("common test spec") {
        val jobParentParent1 = mockk<JesWsNode> {
          every { unit } returns mockk {
            every { name } returns "firstWS"
          }
        }
        val jobParent1 = mockk<JesFilterNode> {
          every { parent } returns jobParentParent1
          every { query } returns mockk {
            every { connectionConfig } returns connectionConfigMock
            every { request } returns mockk {
              every { prefix } returns "prefix_1"
              every { owner } returns "owner"
              every { jobId } returns ""
              every { userCorrelatorFilter } returns "filter_1"
            }
          }
        }

        val jobParentParent2 = mockk<JesWsNode> {
          every { unit } returns mockk {
            every { name } returns "secondWS"
          }
        }
        val jobParent2 = mockk<JesFilterNode> {
          every { parent } returns jobParentParent2
          every { query } returns mockk {
            every { connectionConfig } returns connectionConfigMock
            every { request } returns mockk {
              every { prefix } returns "prefix_2"
              every { owner } returns "owner"
              every { jobId } returns ""
              every { userCorrelatorFilter } returns "filter_2"
            }
          }
        }

        context("isSelectedJobNodesFromSameWS") {
          val jesWorkingSet3 = mockk<JesWorkingSetImpl> {
            every { connectionConfig } returns diffConnectionConfig
          }

          val jobNode1 = mockk<JobNode> {
            every { parent } returns jobParent1
            every { unit } returns mockk {
              every { connectionConfig } returns connectionConfigMock
            }
            every { query } returns mockk {
              every { connectionConfig } returns connectionConfigMock
            }
          }

          val jobNode2 = mockk<JobNode> {
            every { parent } returns jobParent2
            every { unit } returns mockk {
              every { connectionConfig } returns connectionConfigMock
            }
            every { query } returns mockk {
              every { connectionConfig } returns connectionConfigMock
            }
          }

          val jobNode3 = mockk<JobNode> {
            every { unit } returns jesWorkingSet3
            every { query } returns mockk {
              every { connectionConfig } returns diffConnectionConfig
            }
          }

          val wrongJobNode4 = mockk<JesFilterNode> {
            every { unit } returns jesWorkingSet3
          }

          val virtualFileMock = mockk<MFVirtualFile>()
          val jobInfo1 = mockk<Job> {
            every { jobId } returns "TSU01"
            every { jobName } returns "test1"
          }
          val jobInfo2 = mockk<Job> {
            every { jobId } returns "TSU02"
            every { jobName } returns "test2"
          }

          val requester1 = mockk<JobsRequester> {
            every { connectionConfig } returns connectionConfigMock
          }
          val requester2 = mockk<JobsRequester> {
            every { connectionConfig } returns connectionConfigMock
          }

          val attributes1 = mockk<RemoteJobAttributes> {
            every { jobInfo } returns jobInfo1
            every { requesters[0] } returns requester1
          }
          val attributes2 = mockk<RemoteJobAttributes> {
            every { jobInfo } returns jobInfo2
            every { requesters[0] } returns requester2
          }
          val attributes3 = mockk<RemoteJobAttributes>()
          val attributes4 = mockk<RemoteJobAttributes>()

          var isEnableAndVisibleAction: Boolean

          every {
            presentationMock.isEnabledAndVisible = false
          } answers {
            isEnableAndVisibleAction = false
          }
          every {
            presentationMock.isEnabledAndVisible = true
          } answers {
            isEnableAndVisibleAction = true
          }

          should("action is not visible when no selected nodes") {
            isEnableAndVisibleAction = true

            every { jesExplorerView.mySelectedNodesData } returns mutableListOf()
            every { presentationMock.isEnabledAndVisible } returns false

            purgeAction.update(mockActionEventForJesEx)

            assertSoftly { isEnableAndVisibleAction shouldBe false }
          }

          should("action is not visible when selected job nodes contains wrong node") {
            isEnableAndVisibleAction = true

            val nodeData1 = NodeData(jobNode1, virtualFileMock, attributes1)
            val nodeData2 = NodeData(jobNode2, virtualFileMock, attributes2)
            val nodeData3 = NodeData(jobNode3, virtualFileMock, attributes3)
            val nodeData4 = NodeData(wrongJobNode4, virtualFileMock, attributes4)

            every { jesExplorerView.mySelectedNodesData } returns listOf(nodeData1, nodeData2, nodeData3, nodeData4)
            every { presentationMock.isEnabledAndVisible } returns false

            purgeAction.update(mockActionEventForJesEx)

            assertSoftly { isEnableAndVisibleAction shouldBe false }
          }
          should("action is not visible when selected job nodes from different connections") {
            isEnableAndVisibleAction = true

            val nodeData1 = NodeData(jobNode1, virtualFileMock, attributes1)
            val nodeData2 = NodeData(jobNode2, virtualFileMock, attributes2)
            val nodeData3 = NodeData(jobNode3, virtualFileMock, attributes3)

            every { jesExplorerView.mySelectedNodesData } returns listOf(nodeData1, nodeData2, nodeData3)

            purgeAction.update(mockActionEventForJesEx)

            assertSoftly { isEnableAndVisibleAction shouldBe false }
          }
          should("action is not visible when selected job nodes from different working sets") {
            isEnableAndVisibleAction = true

            val nodeData1 = NodeData(jobNode1, virtualFileMock, attributes1)
            val nodeData2 = NodeData(jobNode2, virtualFileMock, attributes2)
            every { jesExplorerView.mySelectedNodesData } returns listOf(nodeData1, nodeData2)

            purgeAction.update(mockActionEventForJesEx)

            assertSoftly { isEnableAndVisibleAction shouldBe false }
          }
          should("action is visible when selected job nodes from the same connections and same working set") {
            isEnableAndVisibleAction = false

            every { jobParentParent1.unit.name } returns "sameWS"
            every { jobParentParent2.unit.name } returns "sameWS"
            val nodeData1 = NodeData(jobNode1, virtualFileMock, attributes1)
            val nodeData2 = NodeData(jobNode2, virtualFileMock, attributes2)
            every { jesExplorerView.mySelectedNodesData } returns listOf(nodeData1, nodeData2)

            purgeAction.update(mockActionEventForJesEx)

            assertSoftly { isEnableAndVisibleAction shouldBe true }
          }
          should("purge actionPerformed when jobs haven't been purged due to error") {
            var isJobsPurged = true

            val nodeData1 = NodeData(jobNode1, virtualFileMock, attributes1)
            val nodeData2 = NodeData(jobNode2, virtualFileMock, attributes2)
            every { jesExplorerView.mySelectedNodesData } returns listOf(nodeData1, nodeData2)
            every { responseMockk.isSuccessful } returns true
            every { responseMockk.body() } returns listOf(jobInfo1, jobInfo2)
            every {
              jobParent1.cleanCache()
            } answers {
              isJobsPurged = false
            }
            every {
              jobParent2.cleanCache()
            } answers {
              isJobsPurged = false
            }

            purgeAction.actionPerformed(mockActionEventForJesEx)

            assertSoftly { isJobsPurged shouldBe false }
          }
          should("purge actionPerformed when jobs have been purged successfully") {
            var isJobsPurged = false

            every {
              dataOpsManager.performOperation(any<PurgeJobOperation>(), any<ProgressIndicator>())
            } answers {
              isJobsPurged = true
              CancelJobPurgeOutRequest()
            }
            every { responseMockk.isSuccessful } returns true
            every { responseMockk.body() } returns listOf(jobInfo1, jobInfo2)

            purgeAction.actionPerformed(mockActionEventForJesEx)

            assertSoftly { isJobsPurged shouldBe true }
          }
          should("purge actionPerformed when jobs have been purged successfully, but refresh by filter fails") {
            every { responseMockk.isSuccessful } returns false
            every { responseMockk.body() } returns listOf()
            every { jobParent1.name } returns "firstFilter"
            every { jobParent2.name } returns "secondFilter"

            assertThrows(RuntimeException::class.java) {
              purgeAction.actionPerformed(mockActionEventForJesEx)
            }
          }
        }
      }
    }
  }
})
