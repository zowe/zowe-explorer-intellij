/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer

import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import eu.ibagroup.formainframe.api.ZosmfApi
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.ConfigStateV2
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.makeCrudableWithoutListeners
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Operation
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.JobsRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteSpoolFileAttributes
import eu.ibagroup.formainframe.dataops.log.JobLogFetcher
import eu.ibagroup.formainframe.dataops.log.MFLogger
import eu.ibagroup.formainframe.explorer.actions.GetJobPropertiesAction
import eu.ibagroup.formainframe.explorer.actions.PurgeJobAction
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.ui.build.jobs.JobBuildTreeView
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.kotlinsdk.CancelJobPurgeOutRequest
import org.zowe.kotlinsdk.JESApi
import org.zowe.kotlinsdk.Job
import org.zowe.kotlinsdk.SpoolFile
import retrofit2.Call
import retrofit2.Response
import java.lang.IllegalStateException
import java.util.*
import java.util.stream.Stream

class ExplorerTestSpec : ShouldSpec({
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
  context("explorer module: FilesWorkingSetImpl") {

    context("addUssPath") {
      val mockedCrud = spyk(makeCrudableWithoutListeners(false) { ConfigStateV2() })
      val uuid1 = "uuid1"
      mockkObject(ConfigService)
      every { ConfigService.instance.crudable } returns mockk()
      every { configCrudable } returns mockk()
      mockkObject(gson)

      val mockedFilesWSConfig = mockk<FilesWorkingSetConfig>()
      every { mockedFilesWSConfig.uuid } returns uuid1
      every { mockedFilesWSConfig.name } returns "filesWSuuid1"
      every { mockedFilesWSConfig.connectionConfigUuid } returns "connUuid"
      every { mockedFilesWSConfig.dsMasks } returns mutableListOf(DSMask("ZOSMFAD.*", mutableListOf()))
      every { mockedFilesWSConfig.ussPaths } returns mutableListOf()

      every { gson.toJson(any() as FilesWorkingSetConfig) } returns "mocked_config_to_copy"

      val clonedConfig = FilesWorkingSetConfig(
        uuid1, "filesWSuuid1", "connUuid",
        mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
        mutableListOf()
      )
      every { gson.fromJson(any() as String, FilesWorkingSetConfig::class.java) } returns clonedConfig
      mockkObject(clonedConfig)

      every { mockedCrud.getAll(FilesWorkingSetConfig::class.java) } returns Stream.of(
        FilesWorkingSetConfig(
          uuid1,
          "filesWSuuid1",
          "connUuid",
          mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
          mutableListOf(UssPath("/u/test1"))
        ),
        FilesWorkingSetConfig(
          uuid1,
          "filesWSuuid1",
          "connUuid",
          mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
          mutableListOf()
        )
      )

      fun getMockedFilesWorkingSetConfigNotNull(): FilesWorkingSetConfig {
        return mockedFilesWSConfig
      }

      fun getMockedFilesWorkingSetConfigNull(): FilesWorkingSetConfig? {
        return null
      }

      val mockedFileExplorer =
        mockk<AbstractExplorerBase<ConnectionConfig, FilesWorkingSetImpl, FilesWorkingSetConfig>>()
      val mockedDisposable = mockk<Disposable>()
      val expectedValues = mockedCrud.getAll(FilesWorkingSetConfig::class.java).toMutableList()

      var actual1: Optional<FilesWorkingSetConfig>? = null
      every { configCrudable.update(any() as FilesWorkingSetConfig) } answers {
        actual1 =
          FilesWorkingSetConfig(
            uuid1,
            "filesWSuuid1",
            "connUuid",
            mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
            mutableListOf(UssPath("/u/test1"))
          ).optional
        actual1
      }

      // addUssPath when clone and collection.add succeeds
      should("add USS path to a config") {
        val filesWorkingSetImpl1 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNotNull() },
            mockedDisposable
          )
        )

        every { clonedConfig.ussPaths.add(any() as UssPath) } answers {
          true
        }

        filesWorkingSetImpl1.addUssPath(UssPath("/u/test1"))

        val expected = expectedValues[0].optional

        assertSoftly {
          actual1 shouldBe expected
        }
      }

      // addUssPath when clone succeeds but collection.add is not
      should("add USS path to a config if collection.add is not succeeded") {
        val filesWorkingSetImpl1 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNotNull() },
            mockedDisposable
          )
        )
        every { clonedConfig.ussPaths.add(any() as UssPath) } answers {
          false
        }

        filesWorkingSetImpl1.addUssPath(UssPath("/u/test1"))
        val actual2 = clonedConfig.optional
        val expected = expectedValues[1].optional

        assertSoftly {
          actual2 shouldBe expected
        }
      }

      // addUssPath with null config
      should("not add USS path to a config as working set config is null") {
        val filesWorkingSetImpl2 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNull() },
            mockedDisposable
          )
        )

        filesWorkingSetImpl2.addUssPath(UssPath("/u/test2"))
        val actual3 = clonedConfig.optional
        val expected = expectedValues[1].optional

        assertSoftly {
          actual3 shouldBe expected
        }
      }
    }

    context("removeUssPath") {
      val mockedCrud = spyk(makeCrudableWithoutListeners(false) { ConfigStateV2() })
      val uuid1 = "uuid1"
      mockkObject(ConfigService)
      every { ConfigService.instance.crudable } returns mockk()
      every { configCrudable } returns mockk()
      mockkObject(gson)

      val mockedFilesWSConfig = mockk<FilesWorkingSetConfig>()
      every { mockedFilesWSConfig.uuid } returns uuid1
      every { mockedFilesWSConfig.name } returns "filesWSuuid1"
      every { mockedFilesWSConfig.connectionConfigUuid } returns "connUuid"
      every { mockedFilesWSConfig.dsMasks } returns mutableListOf(DSMask("ZOSMFAD.*", mutableListOf()))
      every { mockedFilesWSConfig.ussPaths } returns mutableListOf(UssPath("/u/uss_path_to_remove"))

      every { gson.toJson(any() as FilesWorkingSetConfig) } returns "mocked_config_to_copy"

      val clonedConfig = FilesWorkingSetConfig(
        uuid1,
        "filesWSuuid1",
        "connUuid",
        mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
        mutableListOf(UssPath("/u/uss_path_to_remove"))
      )
      every { gson.fromJson(any() as String, FilesWorkingSetConfig::class.java) } returns clonedConfig
      mockkObject(clonedConfig)

      every { mockedCrud.getAll(FilesWorkingSetConfig::class.java) } returns Stream.of(
        FilesWorkingSetConfig(
          uuid1, "filesWSuuid1", "connUuid",
          mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
          mutableListOf()
        ),
        FilesWorkingSetConfig(
          uuid1, "filesWSuuid1", "connUuid",
          mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
          mutableListOf(UssPath("/u/uss_path_to_remove"))
        )
      )

      fun getMockedFilesWorkingSetConfigNotNull(): FilesWorkingSetConfig {
        return mockedFilesWSConfig
      }

      fun getMockedFilesWorkingSetConfigNull(): FilesWorkingSetConfig? {
        return null
      }

      val mockedFileExplorer =
        mockk<AbstractExplorerBase<ConnectionConfig, FilesWorkingSetImpl, FilesWorkingSetConfig>>()
      val mockedDisposable = mockk<Disposable>()
      val expectedValues = mockedCrud.getAll(FilesWorkingSetConfig::class.java).toMutableList()

      var actual4: Optional<FilesWorkingSetConfig>? = null
      every { configCrudable.update(any() as FilesWorkingSetConfig) } answers {
        actual4 =
          FilesWorkingSetConfig(
            uuid1,
            "filesWSuuid1",
            "connUuid",
            mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
            mutableListOf()
          ).optional
        actual4
      }

      // removeUssPath when clone and collection.remove succeeds
      should("remove USS path from a config") {
        val filesWorkingSetImpl1 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNotNull() },
            mockedDisposable
          )
        )

        every { clonedConfig.ussPaths.remove(any() as UssPath) } answers {
          true
        }

        filesWorkingSetImpl1.removeUssPath(UssPath("/u/uss_path_to_remove"))

        val expected = expectedValues[0].optional

        assertSoftly {
          actual4 shouldBe expected
        }
      }

      // removeUssPath when clone succeeds but collection.remove is not
      should("remove USS path from a config if collection.remove is not succeeded") {
        val filesWorkingSetImpl1 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNotNull() },
            mockedDisposable
          )
        )
        every { clonedConfig.ussPaths.remove(any() as UssPath) } answers {
          false
        }

        filesWorkingSetImpl1.removeUssPath(UssPath("/u/uss_path_to_remove"))
        val actual5 = clonedConfig.optional
        val expected = expectedValues[1].optional

        assertSoftly {
          actual5 shouldBe expected
        }
      }

      // removeUssPath with null config
      should("not remove USS path from a config as working set config is null") {
        val filesWorkingSetImpl2 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNull() },
            mockedDisposable
          )
        )

        filesWorkingSetImpl2.removeUssPath(UssPath("/u/uss_path_to_remove"))
        val actual6 = clonedConfig.optional
        val expected = expectedValues[1].optional

        assertSoftly {
          actual6 shouldBe expected
        }
      }
    }
  }

  context("explorer module: ui/ExplorerPasteProvider") {
    // performPaste
    should("perform paste without conflicts") {}
    should("perform paste accepting conflicts") {}
    should("perform paste declining conflicts") {}
  }
  context("explorer module: actions/RenameAction") {
    // actionPerformed
    should("perform rename on dataset") {}
    should("perform rename on dataset member") {}
    should("perform rename on USS file") {}
    should("perform rename on USS directory") {}
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
      every { gson.fromJson(any() as String, Job::class.java) } returns job

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
      every { ZosmfApi.instance.getApi<JESApi>(any(), any()) } returns jesApi
      every { jesApi.getFilteredJobs(any(), any(), any(), any(), any(), any(), any(), any()) } returns call

      val response = mockk<Response<List<Job>>>()
      val jobList = mutableListOf(job, job)
      every { call.execute() } returns response
      every { response.body() } answers {
        if (jobList.isNotEmpty()) {
          jobList.removeFirst()
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
        every { mockActionEventForJesEx.presentation.setVisible(true) } answers { isVisibleInJes = true }

        every { job.status } returns mockk()
        var isEnabledInJobsLog = true
        every { mockActionEventForJobsLog.presentation.setEnabled(false) } answers { isEnabledInJobsLog = false }

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
        every { mockActionEventWithoutDataContext.presentation.setEnabledAndVisible(false) } answers {
          isOperationFailedForNoContextAction = true
        }

        every { job.status } returns null
        var isEnabledInJobsLog = true
        every { mockActionEventForJobsLog.presentation.setEnabled(false) } answers { isEnabledInJobsLog = false }

        var isVisibleForJes = true
        every { updateJesAction.presentation.setVisible(false) } answers { isVisibleForJes = false }
        var isVisibleForJes2 = true
        every { updateJesAction2.presentation.setVisible(false) } answers { isVisibleForJes2 = false }

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
        mockkObject(JobPropertiesDialog.Companion)

        mockkStatic(SpoolFilePropertiesDialog::class)
        mockkObject(SpoolFilePropertiesDialog.Companion)

        every { JobPropertiesDialog.Companion.create(any() as Project?, any() as JobState) } returns dialogMock
        every { SpoolFilePropertiesDialog.Companion.create(any() as Project?, any() as SpoolFileState) } answers {
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
        mockkObject(JobPropertiesDialog.Companion)

        mockkStatic(SpoolFilePropertiesDialog::class)
        mockkObject(SpoolFilePropertiesDialog.Companion)

        every { JobPropertiesDialog.Companion.create(any() as Project?, any() as JobState) } answers {
          throw IllegalStateException("Job properties dialog should not be used.")
        }
        every {
          SpoolFilePropertiesDialog.Companion.create(any() as Project?, any() as SpoolFileState)
        } returns dialogMock

        GetJobPropertiesAction().actionPerformed(getPropertiesEvent)

        verify { dialogMock.showAndGet() }
        unmockkObject(gson)
      }
    }
  }
  context("explorer module: actions/GetFilePropertiesAction") {
    // actionPerformed
    should("get dataset properties") {}
    should("get dataset member properties") {}
    should("get USS file properties") {}
  }
  context("explorer module: actions/ForceRenameAction") {
    // actionPerformed
    should("perform force rename on USS file") {}
    should("not rename dataset") {}
  }
  context("explorer module: actions/ChangeContentModeAction") {
    // isSelected
    should("check if the 'Use binary mode' selected for a file") {}
    // setSelected
    should("select 'Use binary mode' for a file") {}
    should("unselect 'Use binary mode' for a file") {}
  }
  context("explorer module: actions/AllocateDatasetAction") {
    // actionPerformed
    should("perform dataset allocate with default parameters") {}
  }
  context("explorer module: actions/AddMemberAction") {
    // actionPerformed
    should("perform dataset member allocate with default parameters") {}
  }
})
