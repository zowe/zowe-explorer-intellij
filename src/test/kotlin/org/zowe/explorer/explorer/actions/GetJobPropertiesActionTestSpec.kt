package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.JobsFilter
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.attributes.JobsRequester
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.attributes.RemoteSpoolFileAttributes
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.JesWorkingSetImpl
import org.zowe.explorer.explorer.ui.JesExplorerView
import org.zowe.explorer.explorer.ui.JobNode
import org.zowe.explorer.explorer.ui.JobPropertiesDialog
import org.zowe.explorer.explorer.ui.JobState
import org.zowe.explorer.explorer.ui.NodeData
import org.zowe.explorer.explorer.ui.SpoolFileNode
import org.zowe.explorer.explorer.ui.SpoolFilePropertiesDialog
import org.zowe.explorer.explorer.ui.SpoolFileState
import org.zowe.explorer.explorer.ui.getExplorerView
import org.zowe.explorer.testServiceImpl.TestDataOpsManagerImpl
import org.zowe.explorer.utils.gson
import org.zowe.explorer.utils.service
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import org.zowe.kotlinsdk.Job
import org.zowe.kotlinsdk.SpoolFile

class GetJobPropertiesActionTestSpec : ShouldSpec({
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
  context("explorer module: actions/GetJobPropertiesAction") {
    context("actionPerformed") {
      val getPropertiesEvent = mockk<AnActionEvent>()
      val project = mockk<Project>()
      every { getPropertiesEvent.project } returns project

      val virtualFile = mockk<MFVirtualFile>()

      val jesView = mockk<JesExplorerView>()
      val explorer = mockk<Explorer<ConnectionConfig, JesWorkingSetImpl>>()
      every { getPropertiesEvent.getExplorerView<JesExplorerView>() } returns jesView

      val connectionConfig = mockk<ConnectionConfig>()
      every { connectionConfig.uuid } returns "uuid"

      should("get job properties") {
        val jobNode = mockk<JobNode>()
        every { jobNode.virtualFile } returns virtualFile
        val nodeData = spyk(NodeData(jobNode, virtualFile, null))

        every { jesView.mySelectedNodesData } returns listOf(nodeData)

        every { jobNode.explorer } returns explorer
        every { explorer.componentManager } returns ApplicationManager.getApplication()

        val job = mockk<Job>()
        every { job.jobName } returns "name"
        every { job.jobId } returns "id"
        val jobsFilter = spyk(JobsFilter("owner", "prefix", "id"))
        val jobAttr = spyk(RemoteJobAttributes(job, "test", mutableListOf(JobsRequester(connectionConfig, jobsFilter))))

        val dataOpsManager = ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl(explorer.componentManager) {
          override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
            return jobAttr
          }

          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            throw IllegalStateException("No operation is expected to be performed.")
          }
        }

        val dialogMock = mockk<JobPropertiesDialog>()
        every { dialogMock.showAndGet() } returns true

        mockkStatic(JobPropertiesDialog::class)
        mockkObject(JobPropertiesDialog)

        mockkStatic(SpoolFilePropertiesDialog::class)
        mockkObject(SpoolFilePropertiesDialog)

        every { JobPropertiesDialog.create(any() as Project?, any() as JobState) } returns dialogMock
        every { SpoolFilePropertiesDialog.create(any() as Project?, any() as SpoolFileState) } answers {
          throw IllegalStateException("Spool file properties dialog should not be used.")
        }

        GetJobPropertiesAction().actionPerformed(getPropertiesEvent)

        verify { dialogMock.showAndGet() }

      }
      should("get spool file properties") {
        val spoolFileNode = mockk<SpoolFileNode>()
        every { spoolFileNode.virtualFile } returns virtualFile
        val nodeData = spyk(NodeData(spoolFileNode, virtualFile, null))

        every { jesView.mySelectedNodesData } returns listOf(nodeData)

        every { spoolFileNode.explorer } returns explorer
        every { explorer.componentManager } returns ApplicationManager.getApplication()

        val spoolFile = mockk<SpoolFile>()

        every { spoolFile.ddName } returns "ddname"
        every { spoolFile.jobId } returns "jobid"
        every { spoolFile.id } returns 1

        mockkObject(gson)
        every { gson.fromJson(any() as String, SpoolFile::class.java) } returns spoolFile

        val parentFile = mockk<MFVirtualFile>()
        val spoolFileAttr = spyk(RemoteSpoolFileAttributes(spoolFile, parentFile))

        val dataOpsManager = ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl(explorer.componentManager) {
          override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
            return spoolFileAttr
          }

          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            throw IllegalStateException("No operation is expected to be performed.")
          }
        }

        val dialogMock = mockk<SpoolFilePropertiesDialog>()
        every { dialogMock.showAndGet() } returns true

        mockkStatic(JobPropertiesDialog::class)
        mockkObject(JobPropertiesDialog)

        mockkStatic(SpoolFilePropertiesDialog::class)
        mockkObject(SpoolFilePropertiesDialog)

        every { JobPropertiesDialog.create(any() as Project?, any() as JobState) } answers {
          throw IllegalStateException("Job properties dialog should not be used.")
        }
        every {
          SpoolFilePropertiesDialog.create(any() as Project?, any() as SpoolFileState)
        } returns dialogMock

        GetJobPropertiesAction().actionPerformed(getPropertiesEvent)

        verify { dialogMock.showAndGet() }
        unmockkObject(gson)
      }
    }
  }
})
