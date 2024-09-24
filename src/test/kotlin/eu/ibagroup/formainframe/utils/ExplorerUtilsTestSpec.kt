/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.explorer.*
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import javax.swing.Icon

class ExplorerUtilsTestSpec : WithApplicationShouldSpec({

  val filesWSSlot = slot<FilesWorkingSetImpl>()
  val dsMaskSlot = slot<DSMask>()
  val ussMaskSlot = slot<UssPath>()
  val jobFilterSlot = slot<JobsFilter>()
  val jesWSSlot = slot<JesWorkingSetImpl>()

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  fun <T : ExplorerTreeNode<*,*>> T.doMockStubs() {
    val filesWorkingSet = mockk<FilesWorkingSetImpl>()
    val jesWorkingSet = mockk<JesWorkingSetImpl>()
    when (val node: T = this) {
      is FilesWorkingSetNode -> {
        every { node.unit } returns filesWorkingSet
        every { filesWorkingSet.name } returns "DELETED_WORKING_SET"
      }
      is DSMaskNode -> {
        val dsMask = mockk<DSMask>()
        every { dsMask.mask } returns "DELETED_MASK"
        every { node.unit } returns filesWorkingSet
        every { node.value } returns dsMask
        every { node.cleanCache(any(), any(), any(), any()) } just Runs
        every { node.unit.removeMask(capture(dsMaskSlot)) } just Runs
      }
      is UssDirNode -> {
        val ussMask = mockk<UssPath>()
        every { ussMask.path } returns "DELETED_USS_MASK"
        every { node.isUssMask } returns true
        every { node.unit } returns filesWorkingSet
        every { node.value } returns ussMask
        every { node.cleanCache(any(), any(), any(), any()) } just Runs
        every { node.unit.removeUssPath(capture(ussMaskSlot)) } just Runs
      }
      is JesFilterNode -> {
        val jobsFilter = mockk<JobsFilter>()
        every { jobsFilter.jobId } returns "DELETED_JOB_FILTER"
        every { node.unit } returns jesWorkingSet
        every { node.value } returns jobsFilter
        every { node.unit.removeFilter(capture(jobFilterSlot)) } just Runs
      }
      is JesWsNode -> {
        every { node.unit } returns jesWorkingSet
        every { jesWorkingSet.name } returns "DELETED_JES_WORKING_SET"
      }
    }
  }

  context("utils module: explorerUtils") {

    val fileExplorer = mockk<Explorer<ConnectionConfig, FilesWorkingSet>>()
    val jesExplorer = mockk<Explorer<ConnectionConfig, JesWorkingSetImpl>>()

    // explorers
    val fileExplorerView = mockk<FileExplorerView>()
    every { fileExplorerView.explorer } returns fileExplorer
    every { fileExplorerView.explorer.disposeUnit(capture(filesWSSlot)) } just Runs

    val jesExplorerView = mockk<JesExplorerView>()
    every { jesExplorerView.explorer } returns jesExplorer
    every { jesExplorerView.explorer.disposeUnit(capture(jesWSSlot)) } just Runs

    val project = mockk<Project>()

    // File explorer view
    val fileWS = mockk<FilesWorkingSetNode>()
    val dsMask = mockk<DSMaskNode>()
    val rootDirNode = mockk<UssDirNode>()

    // Jes explorer view
    val jesWS = mockk<JesWsNode>()
    val jobFilter = mockk<JesFilterNode>()

    // dialog stubs
    mockkStatic(Messages::class)
    every {
      Messages.showYesNoDialog(
        any() as Project?, any() as String, any() as String, any() as String, any() as String, any() as Icon?
      )
    } returns Messages.YES

    should("perform working set unit deletion for the file explorer view") {
      // given
      fileWS.doMockStubs()
      val listOfNodesToTest = mutableListOf(fileWS)
      // when
      listOfNodesToTest.performUnitsDeletionBasedOnSelection(project, fileExplorerView, jesExplorerView)
      // then
      assertSoftly {
        filesWSSlot.captured.name shouldBe "DELETED_WORKING_SET"
      }
    }

    should("perform dataset mask deletion for the file explorer view") {
      // given
      dsMask.doMockStubs()
      val listOfNodesToTest = mutableListOf(dsMask)
      // when
      listOfNodesToTest.performUnitsDeletionBasedOnSelection(project, fileExplorerView, jesExplorerView)
      // then
      assertSoftly {
        dsMaskSlot.captured.mask shouldBe "DELETED_MASK"
      }
    }

    should("perform root uss dir deletion for the file explorer view") {
      // given
      rootDirNode.doMockStubs()
      val listOfNodesToTest = mutableListOf(rootDirNode)
      // when
      listOfNodesToTest.performUnitsDeletionBasedOnSelection(project, fileExplorerView, jesExplorerView)
      // then
      assertSoftly {
        ussMaskSlot.captured.path shouldBe "DELETED_USS_MASK"
      }
    }

    should("perform jes working set unit deletion for the jes explorer view") {
      // given
      jesWS.doMockStubs()
      val listOfNodesToTest = mutableListOf(jesWS)
      // when
      listOfNodesToTest.performUnitsDeletionBasedOnSelection(project, fileExplorerView, jesExplorerView)
      // then
      assertSoftly {
        jesWSSlot.captured.name shouldBe "DELETED_JES_WORKING_SET"
      }
    }

    should("perform jes filter deletion for the jes explorer view") {
      // given
      jobFilter.doMockStubs()
      val listOfNodesToTest = mutableListOf(jobFilter)
      // when
      listOfNodesToTest.performUnitsDeletionBasedOnSelection(project, fileExplorerView, jesExplorerView)
      // then
      assertSoftly {
        jobFilterSlot.captured.jobId shouldBe "DELETED_JOB_FILTER"
      }
    }

    should("perform jes WS and jes filter(not included in jes WS to be deleted) deletion for the jes explorer view") {
      // given
      // clear static mock to be able to capture only the last invocation
      mockkStatic(Messages::class)
      every {
        Messages.showYesNoDialog(
          any() as Project?, any() as String, any() as String, any() as String, any() as String, any() as Icon?
        )
      } returns Messages.YES

      jobFilter.doMockStubs()
      jesWS.doMockStubs()
      val messageHeader = slot<String>()
      val listOfNodesToTest = mutableListOf(jesWS, jobFilter)
      // when
      listOfNodesToTest.performUnitsDeletionBasedOnSelection(project, fileExplorerView, jesExplorerView)
      // then
      verify { Messages.showYesNoDialog(any() as Project?, any() as String, capture(messageHeader), any() as String, any() as String, any() as Icon?) }
      assertSoftly {
        jesWSSlot.captured.name shouldBe "DELETED_JES_WORKING_SET"
        jobFilterSlot.captured.jobId shouldBe "DELETED_JOB_FILTER"
        messageHeader.captured shouldBe "Confirm Jes Working Set(s) and Jes Filter(s) Deletion"
      }
    }
  }
})
