package org.zowe.explorer.explorer.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.ws.JobsFilter
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.UnitRemoteQueryImpl
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.attributes.JobsRequester
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.log.JobLogFetcher
import org.zowe.explorer.dataops.log.MFLogger
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.JesWorkingSetImpl
import org.zowe.explorer.explorer.ui.ErrorNode
import org.zowe.explorer.explorer.ui.JesExplorerView
import org.zowe.explorer.explorer.ui.JesFilterNode
import org.zowe.explorer.explorer.ui.JobNode
import org.zowe.explorer.explorer.ui.NodeData
import org.zowe.explorer.explorer.ui.getExplorerView
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.testutils.testServiceImpl.TestDataOpsManagerImpl
import org.zowe.explorer.ui.build.jobs.JobBuildTreeView
import org.zowe.explorer.utils.gson
import org.zowe.explorer.utils.service
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import org.zowe.kotlinsdk.CancelJobPurgeOutRequest
import org.zowe.kotlinsdk.JESApi
import org.zowe.kotlinsdk.Job
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

      mockkObject(CredentialService)
      every { CredentialService.instance.getUsernameByKey(any()) } returns "user"
      every { CredentialService.instance.getPasswordByKey(any()) } returns "pas"

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
      mockkObject(ZosmfApi)
      every { ZosmfApi.instance.hint(JESApi::class).getApi<JESApi>(any(), any()) } returns jesApi
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

        dataOpsManager = ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl(explorer.componentManager) {
          override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
            return jobAttr
          }

          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            return mockRequest as R
          }

        }

        var isOperationSucceededForJesEx = false
        every {
          explorer.showNotification(any(), any(), NotificationType.INFORMATION, any())
        } answers {
          isOperationSucceededForJesEx = true
        }

        var isOperationSucceededForJobsLog = false
        every {
          jobsLogView.showNotification(any(), any(), any(), NotificationType.INFORMATION)
        } answers {
          isOperationSucceededForJobsLog = true
        }

        var isVisibleInJes = false
        every { mockActionEventForJesEx.presentation.isVisible = true } answers { isVisibleInJes = true }

        every { job.status } returns mockk()
        var isEnabledInJobsLog = true
        every { mockActionEventForJobsLog.presentation.isEnabled = false } answers { isEnabledInJobsLog = false }

        purgeAction.actionPerformed(mockActionEventForJesEx)
        purgeAction.update(mockActionEventForJesEx)
        purgeAction.actionPerformed(mockActionEventForJobsLog)
        purgeAction.update(mockActionEventForJobsLog)

        assertSoftly {
          isOperationSucceededForJesEx shouldBe true
          isOperationSucceededForJobsLog shouldBe true
          isVisibleInJes shouldBe true
          isEnabledInJobsLog shouldBe true
          purgeAction.isDumbAware shouldBe true
        }

      }
      should("perform purge on job with error") {

        val updateJesAction = mockk<AnActionEvent>()
        val jesViewForUpdate = mockk<JesExplorerView>()
        every { updateJesAction.getExplorerView<JesExplorerView>() } returns jesViewForUpdate
        every { jesViewForUpdate.mySelectedNodesData } returns listOf()

        val updateJesAction2 = mockk<AnActionEvent>()
        val jesViewForUpdate2 = mockk<JesExplorerView>()
        every { updateJesAction2.getExplorerView<JesExplorerView>() } returns jesViewForUpdate2
        val errorNode = mockk<ErrorNode<ConnectionConfig>>()
        every { jesViewForUpdate2.mySelectedNodesData } returns listOf(
          NodeData(
            errorNode,
            virtualFile,
            null
          )
        )

        dataOpsManager = ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl(explorer.componentManager) {
          override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
            return jobAttr
          }

          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            throw IllegalStateException("No operation is expected to be performed.")
          }
        }

        var isOperationFailedForJesEx = false
        every {
          explorer.showNotification(any(), any(), NotificationType.ERROR, any())
        } answers {
          isOperationFailedForJesEx = true
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

        var isVisibleForJes = true
        every { updateJesAction.presentation.isVisible = false } answers { isVisibleForJes = false }
        var isVisibleForJes2 = true
        every { updateJesAction2.presentation.isVisible = false } answers { isVisibleForJes2 = false }

        purgeAction.actionPerformed(mockActionEventForJesEx)
        purgeAction.actionPerformed(mockActionEventForJobsLog)
        purgeAction.actionPerformed(mockActionEventWithoutDataContext)
        purgeAction.update(mockActionEventWithoutDataContext)
        purgeAction.update(mockActionEventForJobsLog)
        purgeAction.update(updateJesAction)
        purgeAction.update(updateJesAction2)

        assertSoftly {
          isOperationFailedForJesEx shouldBe true
          isOperationFailedForJobsLog shouldBe true
          isEnabledInJobsLog shouldBe false
          isVisibleForJes shouldBe false
          isVisibleForJes2 shouldBe false
          isOperationFailedForNoContextAction shouldBe true
        }
      }
    }
  }
})
