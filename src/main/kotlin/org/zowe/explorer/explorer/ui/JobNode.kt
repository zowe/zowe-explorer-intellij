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

package org.zowe.explorer.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.SimpleTextAttributes.STYLE_BOLD
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.UnitRemoteQueryImpl
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.fetch.JobQuery
import org.zowe.explorer.explorer.JesWorkingSet
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.Job
import org.zowe.kotlinsdk.annotations.ZVersion
import kotlin.math.roundToInt

private val jobIcon = AllIcons.Nodes.Folder

/** JES Explorer job node representation */
class JobNode(
  library: MFVirtualFile,
  project: Project,
  parent: ExplorerTreeNode<ConnectionConfig, *>,
  workingSet: JesWorkingSet,
  treeStructure: ExplorerTreeStructureBase
) : RemoteMFFileFetchNode<ConnectionConfig, MFVirtualFile, JobQuery, JesWorkingSet>(
  library, project, parent, workingSet, treeStructure
), RefreshableNode {
  override fun makeFetchTaskTitle(query: RemoteQuery<ConnectionConfig, JobQuery, Unit>): String {
    return "Fetching members for ${query.request.library.name}"
  }

  private val jobJclNotAvailable = "JCL NOT AVAILABLE"

  override val query: RemoteQuery<ConnectionConfig, JobQuery, Unit>?
    get() {
      val connectionConfig = unit.connectionConfig
      return if (connectionConfig != null) {
        UnitRemoteQueryImpl(JobQuery(value), connectionConfig)
      } else null
    }

  override fun Collection<MFVirtualFile>.toChildrenNodes(): List<AbstractTreeNode<*>> {
    return map { SpoolFileNode(it, notNullProject, this@JobNode, unit, treeStructure) }
  }

  override val requestClass = JobQuery::class.java

  override fun update(presentation: PresentationData) {
    val attributes = DataOpsManager.getService().tryToGetAttributes(value) as? RemoteJobAttributes
    val job = attributes?.jobInfo
    val jobIdText = if (job == null) "" else "(${job.jobId})"
    presentation.addText("${job?.jobName ?: ""} $jobIdText ", SimpleTextAttributes.REGULAR_ATTRIBUTES)

    if ((job?.execEnded == null || job.execEnded?.trim()
        .equals("")) && job?.returnedCode == null && job?.execStarted != null
    ) {
      if (job.status != Job.Status.INPUT) {
        if (job.execStarted?.trim().equals("")) {
          presentation.addText(
            "JOB IN PROGRESS", SimpleTextAttributes(STYLE_BOLD, JBColor.BLUE)
          )
        } else {
          presentation.addText(
            "STARTED AT: " +
              if (job.execStarted == null) {
                parseJobTimestampValueToDisplay(job.execSubmitted)
              } else {
                parseJobTimestampValueToDisplay(job.execStarted)
              },
            SimpleTextAttributes(STYLE_BOLD, JBColor.BLUE)
          )
        }
      } else {
        presentation.addText(
          "PENDING INPUT: " +
            if (job.execStarted == null) {
              parseJobTimestampValueToDisplay(job.execSubmitted)
            } else {
              parseJobTimestampValueToDisplay(job.execStarted)
            },
          SimpleTextAttributes(STYLE_BOLD, JBColor.YELLOW)
        )
      }
    } else {
      if (job?.returnedCode == null && (job?.execEnded == null || job.execEnded?.trim().equals(jobJclNotAvailable))) {
        presentation.addText(
          jobJclNotAvailable, SimpleTextAttributes(STYLE_BOLD, JBColor.RED)
        )
      } else if ((job.execEnded == null || job.execEnded?.trim()
          .equals("")) && (job.execStarted == null || job.execStarted?.trim().equals(""))
      ) {
        presentation.addText(
          if (job.execSubmitted == null) {
            "JOB ENDED. RC = ${job.returnedCode}"
          } else {
            "ENDED AT: " + parseJobTimestampValueToDisplay(job.execSubmitted) + ". RC = ${job.returnedCode}"
          },
          when (getReturnCode(job.returnedCode)) {
            ReturnCode.SUCCESS -> SimpleTextAttributes(STYLE_BOLD, JBColor.GREEN)
            ReturnCode.WARNING -> SimpleTextAttributes(STYLE_BOLD, JBColor.ORANGE)
            else -> SimpleTextAttributes(STYLE_BOLD, JBColor.RED)
          }
        )
      } else {
        presentation.addText(
          "ENDED AT: " +
            if (job.execEnded == null || job.execEnded?.trim().equals("")) {
              parseJobTimestampValueToDisplay(job.execSubmitted) ?: parseJobTimestampValueToDisplay(job.execStarted)
            } else {
              parseJobTimestampValueToDisplay(job.execEnded)
            } + ". RC = ${job.returnedCode}",
          when (getReturnCode(job.returnedCode)) {
            ReturnCode.SUCCESS -> SimpleTextAttributes(STYLE_BOLD, JBColor.GREEN)
            ReturnCode.WARNING -> SimpleTextAttributes(STYLE_BOLD, JBColor.ORANGE)
            else -> SimpleTextAttributes(STYLE_BOLD, JBColor.RED)
          }
        )
      }
    }
    presentation.setIcon(jobIcon)
  }

  override fun getVirtualFile(): MFVirtualFile? {
    return value
  }

  /**
   * Formatter method for job executions timestamps to be displayed in JES Explorer tree
   * @param timestamp - timestamp to be formatted
   * @return Formatted timestamp value to be displayed
   */
  private fun parseJobTimestampValueToDisplay(timestamp: String?): String? {
    // Means we got timestamps from FetchHelper. We do not need to parse it
    if (query!!.connectionConfig.zVersion < ZVersion.ZOS_2_4) {
      return timestamp
    } else {
      // Means we got timestamps from fetch query already. Need to parse it before return to caller
      return if (timestamp != null) {
        val date = timestamp.substringBefore("T")
        val time = (timestamp.substringAfter("T")).replace("Z", "", true)
        val timeWithoutRoundSeconds = time.substringBeforeLast(":")
        val roundSeconds = time.substringAfterLast(":").toDouble().roundToInt()
        val formattedRoundSeconds: String = if (roundSeconds < 10) {
          "0$roundSeconds"
        } else {
          roundSeconds.toString()
        }
        "$date $timeWithoutRoundSeconds:$formattedRoundSeconds"
      } else {
        ""
      }
    }
  }

  /**
   * Function to parse return code after job completion
   * @param returnCode - return code to parse
   * @return ReturnCode.ERR if return code is kind of Error.
   * ReturnCode.WARN if return code in [1..8] range.
   * ReturnCode.SUCCESS otherwise
   */
  private fun getReturnCode(returnCode: String?): ReturnCode {
    if (returnCode != null) {
      return if (!returnCode.contains(Regex("ERR|ABEND|CANCEL|FAIL"))) {
        val numberedRC = returnCode.split(" ").getOrNull(1)?.toIntOrNull()
        if (numberedRC != null) {
          when (numberedRC) {
            0 -> ReturnCode.SUCCESS
            in 1..7 -> ReturnCode.WARNING
            else -> ReturnCode.ERROR
          }
        } else {
          ReturnCode.ERROR
        }
      } else ReturnCode.ERROR
    }
    return ReturnCode.SUCCESS
  }

  enum class ReturnCode {
    SUCCESS, WARNING, ERROR
  }
}
