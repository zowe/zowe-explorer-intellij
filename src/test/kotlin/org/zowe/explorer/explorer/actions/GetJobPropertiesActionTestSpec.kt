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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.*
import io.mockk.clearAllMocks
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
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.testutils.testServiceImpl.TestDataOpsManagerImpl
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.explorer.explorer.ui.*
import org.zowe.kotlinsdk.Job
import org.zowe.kotlinsdk.SpoolFile

class GetJobPropertiesActionTestSpec : WithApplicationShouldSpec({
  afterSpec {
    unmockkAll()
    clearAllMocks()
  }
  context("explorer module: actions/GetJobPropertiesAction") {
    context("actionPerformed") {
      val mockProject = mockk<Project>()
      var jesView = mockk<JesExplorerView>()
      val getPropertiesEvent = mockk<AnActionEvent> {
        every { project } returns mockProject
        every { getData(EXPLORER_VIEW) } returns jesView
      }
      val mockVirtualFile = mockk<MFVirtualFile>()
      val mockExplorer = mockk<Explorer<ConnectionConfig, JesWorkingSetImpl>> {
        every { componentManager } returns ApplicationManager.getApplication()
      }
      val connectionConfig = mockk<ConnectionConfig> {
        every { uuid } returns "uuid"
      }

      beforeEach {
        jesView = mockk<JesExplorerView>()
        every { getPropertiesEvent.getData(EXPLORER_VIEW) } returns jesView
      }

      should("get job properties") {
        val jobNode = mockk<JobNode> {
          every { virtualFile } returns mockVirtualFile
          every { explorer } returns mockExplorer
        }
        val nodeData = spyk(NodeData(jobNode, mockVirtualFile, null))

        every { jesView.mySelectedNodesData } returns listOf(nodeData)

        val job = mockk<Job> {
          every { jobName } returns "name"
          every { jobId } returns "id"
        }
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

        val dialogMock = mockk<JobPropertiesDialog> {
          every { showAndGet() } returns true
        }

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
        val spoolFileNode = mockk<SpoolFileNode> {
          every { virtualFile } returns mockVirtualFile
          every { explorer } returns mockExplorer
        }
        val nodeData = spyk(NodeData(spoolFileNode, mockVirtualFile, null))

        every { jesView.mySelectedNodesData } returns listOf(nodeData)

        val spoolFile = mockk<SpoolFile> {
          every { ddName } returns "ddname"
          every { jobId } returns "jobid"
          every { id } returns 1
        }

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

        val dialogMock = mockk<SpoolFilePropertiesDialog> {
          every { showAndGet() } returns true
        }

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
      }
    }
  }
})
