/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.config.ws.JobsFilter
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.sort.SortQueryKeys
import org.zowe.explorer.explorer.JesExplorer
import org.zowe.explorer.explorer.JesWorkingSetImpl
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.testutils.testServiceImpl.TestDataOpsManagerImpl
import org.zowe.explorer.utils.clearOldKeysAndAddNew
import org.zowe.explorer.utils.service
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import org.zowe.kotlinsdk.Job

class JesFilterNodeTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("explorer module: ui/JesFilterNode") {

    val mockedJobsFilter = mockk<JobsFilter>()
    val mockedProject = mockk<Project>()
    val mockedParent = mockk<JesWsNode>()
    val mockedWorkingSet = mockk<JesWorkingSetImpl>()
    val mockedExplorer = mockk<JesExplorer>()
    val mockedExplorerTreeStructure = mockk<ExplorerTreeStructureBase>()

    every { mockedWorkingSet.explorer } returns mockedExplorer
    every { mockedExplorerTreeStructure.registerNode(any()) } just Runs

    val classUnderTest =
      spyk(JesFilterNode(mockedJobsFilter, mockedProject, mockedParent, mockedWorkingSet, mockedExplorerTreeStructure))

    context("sort children nodes") {

      val virtualFile1 = mockk<MFVirtualFile>()
      val virtualFile2 = mockk<MFVirtualFile>()
      val virtualFile3 = mockk<MFVirtualFile>()
      val jobAttributes1 = mockk<RemoteJobAttributes>()
      val jobAttributes2 = mockk<RemoteJobAttributes>()
      val jobAttributes3 = mockk<RemoteJobAttributes>()
      val jobInfo1 = Job(
        "id1",
        "name1",
        null,
        "ARST",
        Job.Status.OUTPUT,
        Job.JobType.JOB,
        null,
        null,
        "url1",
        "filesUrl1",
        null,
        1,
        "phase1",
        listOf(),
        null,
        null,
        null,
        null,
        "2024-01-02",
        "2024-01-02"
      )
      val jobInfo2 = Job(
        "id2",
        "name2",
        null,
        "DLIS",
        Job.Status.ACTIVE,
        Job.JobType.JOB,
        null,
        null,
        "url2",
        "filesUrl2",
        null,
        2,
        "phase2",
        listOf(),
        null,
        null,
        null,
        null,
        "2024-01-04",
        "2024-01-04"
      )
      val jobInfo3 = Job(
        "id3",
        "name3",
        null,
        "ZOSMFAD",
        Job.Status.INPUT,
        Job.JobType.JOB,
        null,
        null,
        "url3",
        "filesUrl3",
        null,
        3,
        "phase3",
        listOf(),
        null,
        null,
        null,
        null,
        "2024-01-01",
        "2024-01-06"
      )
      val jobNode1 = mockk<JobNode>()
      val jobNode2 = mockk<JobNode>()
      val jobNode3 = mockk<JobNode>()
      every { jobNode1.value } returns virtualFile1
      every { jobNode2.value } returns virtualFile2
      every { jobNode3.value } returns virtualFile3
      every { jobAttributes1.jobInfo } returns jobInfo1
      every { jobAttributes2.jobInfo } returns jobInfo2
      every { jobAttributes3.jobInfo } returns jobInfo3

      val nodeToAttributesMap = mutableMapOf(
        Pair(virtualFile1, jobAttributes1),
        Pair(virtualFile2, jobAttributes2),
        Pair(virtualFile3, jobAttributes3)
      )

      val listToSort = listOf(jobNode1, jobNode2, jobNode3)

      val dataOpsManagerService =
        ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
        override fun tryToGetAttributes(file: VirtualFile): FileAttributes? {
          return nodeToAttributesMap[file]
        }
      }

      should("return children nodes sorted by JOB NAME in ascending order") {
        // given
        val sortKeys = listOf(SortQueryKeys.JOB_NAME, SortQueryKeys.ASCENDING)
        val expected = listOf(jobNode1, jobNode2, jobNode3)

        // when
        val actual = classUnderTest.sortChildrenNodes(listToSort, sortKeys)

        //then
        assertSoftly {
          actual shouldContainExactly expected
        }
      }

      should("return children nodes sorted by JOB OWNER in ascending order") {
        // given
        val sortKeys = listOf(SortQueryKeys.JOB_OWNER, SortQueryKeys.ASCENDING)
        val expected = listOf(jobNode1, jobNode2, jobNode3)

        // when
        val actual = classUnderTest.sortChildrenNodes(listToSort, sortKeys)

        //then
        assertSoftly {
          actual shouldContainExactly expected
        }
      }

      should("return children nodes sorted by JOB STATUS in ascending order") {
        // given
        val sortKeys = listOf(SortQueryKeys.JOB_STATUS, SortQueryKeys.ASCENDING)
        val expected = listOf(jobNode2, jobNode3, jobNode1)

        // when
        val actual = classUnderTest.sortChildrenNodes(listToSort, sortKeys)

        //then
        assertSoftly {
          actual shouldContainExactly expected
        }
      }

      should("return children nodes sorted by JOB ID in ascending order") {
        // given
        val sortKeys = listOf(SortQueryKeys.JOB_ID, SortQueryKeys.ASCENDING)
        val expected = listOf(jobNode1, jobNode2, jobNode3)

        // when
        val actual = classUnderTest.sortChildrenNodes(listToSort, sortKeys)

        //then
        assertSoftly {
          actual shouldContainExactly expected
        }
      }

      should("return children nodes sorted by JOB CREATION DATE in ascending order") {
        // given
        val sortKeys = listOf(SortQueryKeys.JOB_CREATION_DATE, SortQueryKeys.ASCENDING)
        val expected = listOf(jobNode3, jobNode1, jobNode2)

        // when
        val actual = classUnderTest.sortChildrenNodes(listToSort, sortKeys)

        //then
        assertSoftly {
          actual shouldContainExactly expected
        }
      }

      should("return children nodes sorted by JOB COMPLETION DATE in ascending order") {
        // given
        val sortKeys = listOf(SortQueryKeys.JOB_COMPLETION_DATE, SortQueryKeys.ASCENDING)
        val expected = listOf(jobNode1, jobNode2, jobNode3)

        // when
        val actual = classUnderTest.sortChildrenNodes(listToSort, sortKeys)

        //then
        assertSoftly {
          actual shouldContainExactly expected
        }
      }

      should("return children nodes sorted by invalid sort key") {
        // given
        val sortKeys = listOf(SortQueryKeys.FILE_NAME, SortQueryKeys.ASCENDING)
        val expected = listOf(jobNode1, jobNode2, jobNode3)

        // when
        val actual = classUnderTest.sortChildrenNodes(listToSort, sortKeys)

        //then
        assertSoftly {
          actual shouldContainExactly expected
        }
      }

      should("return children nodes without sorting if sort keys are null") {
        // given
        val sortKeys = listOf<SortQueryKeys>()
        val expected = listOf(jobNode1, jobNode2, jobNode3)

        // when
        val actual = classUnderTest.sortChildrenNodes(listToSort, sortKeys)

        //then
        assertSoftly {
          actual shouldContainExactly expected
        }
      }

      should("return children nodes sorted by JOB NAME in descending order") {
        // given
        classUnderTest.currentSortQueryKeysList.clearOldKeysAndAddNew(SortQueryKeys.DESCENDING)
        val sortKeys = listOf(SortQueryKeys.JOB_NAME)
        val expected = listOf(jobNode3, jobNode2, jobNode1)

        // when
        val actual = classUnderTest.sortChildrenNodes(listToSort, sortKeys)

        //then
        assertSoftly {
          actual shouldContainExactly expected
        }
      }
    }
  }
})
