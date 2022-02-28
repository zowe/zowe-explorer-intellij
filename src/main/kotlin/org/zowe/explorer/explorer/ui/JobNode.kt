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

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.UnitRemoteQueryImpl
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.fetch.JobQuery
import org.zowe.explorer.explorer.JesWorkingSet
import org.zowe.explorer.vfs.MFVirtualFile

private val jobIcon = AllIcons.Nodes.Folder

class JobNode(
  library: MFVirtualFile,
  project: Project,
  parent: ExplorerTreeNode<*>,
  workingSet: JesWorkingSet,
  treeStructure: ExplorerTreeStructureBase
) : RemoteMFFileFetchNode<MFVirtualFile, JobQuery, JesWorkingSet>(
  library, project, parent, workingSet, treeStructure
), MFNode, RefreshableNode {
  override fun makeFetchTaskTitle(query: RemoteQuery<JobQuery, Unit>): String {
    return "Fetching members for ${query.request.library.name}"
  }

  override val query: RemoteQuery<JobQuery, Unit>?
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
    val attributes = service<DataOpsManager>().tryToGetAttributes(value) as? RemoteJobAttributes
    val job = attributes?.jobInfo
    val jobIdText = if (job == null) "" else "(${job.jobId})"
    presentation.addText("${job?.jobName ?: ""} $jobIdText ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
    presentation.addText(job?.status?.value ?: "", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    presentation.setIcon(jobIcon)
  }

  override fun getVirtualFile(): MFVirtualFile? {
    return value
  }
}
