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

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Operation
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.JobsRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteSpoolFileAttributes
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.JesWorkingSetImpl
import eu.ibagroup.formainframe.explorer.ui.JesExplorerView
import eu.ibagroup.formainframe.explorer.ui.JobNode
import eu.ibagroup.formainframe.explorer.ui.JobPropertiesDialog
import eu.ibagroup.formainframe.explorer.ui.JobState
import eu.ibagroup.formainframe.explorer.ui.NodeData
import eu.ibagroup.formainframe.explorer.ui.SpoolFileNode
import eu.ibagroup.formainframe.explorer.ui.SpoolFilePropertiesDialog
import eu.ibagroup.formainframe.explorer.ui.SpoolFileState
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.utils.gson
import eu.ibagroup.formainframe.vfs.MFVirtualFile
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

class GetJobPropertiesActionTestSpec : WithApplicationShouldSpec({
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

        val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
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

        val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
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
