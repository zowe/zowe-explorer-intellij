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


import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.JobsFilter
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.UnitRemoteQueryImpl
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.sort.SortQueryKeys
import org.zowe.explorer.dataops.sort.typedSortKeys
import org.zowe.explorer.explorer.JesWorkingSet
import org.zowe.explorer.utils.clearAndMergeWith
import org.zowe.explorer.vfs.MFVirtualFile
import icons.ForMainframeIcons

private val jesFilterIcon = ForMainframeIcons.JclDirectory

/** JES Explorer filter representation */
class JesFilterNode(
  jobsFilter: JobsFilter,
  project: Project,
  parent: ExplorerTreeNode<ConnectionConfig, *>,
  workingSet: JesWorkingSet,
  treeStructure: ExplorerTreeStructureBase,
  override val currentSortQueryKeysList: List<SortQueryKeys> = mutableListOf(
    SortQueryKeys.JOB_CREATION_DATE,
    SortQueryKeys.ASCENDING
  ),
  override val sortedNodes: List<AbstractTreeNode<*>> = mutableListOf()
) : RemoteMFFileFetchNode<ConnectionConfig, JobsFilter, JobsFilter, JesWorkingSet>(
  jobsFilter, project, parent, workingSet, treeStructure
), SortableNode, RefreshableNode {

  override val query: RemoteQuery<ConnectionConfig, JobsFilter, Unit>?
    get() {
      val connectionConfig = unit.connectionConfig
      return if (connectionConfig != null) {
        UnitRemoteQueryImpl(value, connectionConfig)
      } else null
    }

  override fun Collection<MFVirtualFile>.toChildrenNodes(): List<AbstractTreeNode<*>> {
    return map {
      JobNode(it, notNullProject, this@JesFilterNode, unit, treeStructure)
    }.let { sortChildrenNodes(it, currentSortQueryKeysList) }
  }

  override val requestClass = JobsFilter::class.java


  override fun update(presentation: PresentationData) {
    presentation.addText(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
    presentation.setIcon(jesFilterIcon)
    updateRefreshDateAndTime(presentation)
  }


  override fun makeFetchTaskTitle(query: RemoteQuery<ConnectionConfig, JobsFilter, Unit>): String {
    return "Fetching jobs for ${query.request}"
  }

  override fun <Node : AbstractTreeNode<*>> sortChildrenNodes(
    childrenNodes: List<Node>,
    sortKeys: List<SortQueryKeys>
  ): List<Node> {
    val listToReturn: List<Node> = mutableListOf()
    val foundSortKey = sortKeys.firstOrNull { typedSortKeys.contains(it) }
    if (foundSortKey != null) {
      listToReturn.clearAndMergeWith(performJobsSorting(childrenNodes, this@JesFilterNode, foundSortKey))
    } else {
      listToReturn.clearAndMergeWith(childrenNodes)
    }
    return listToReturn
  }

  /**
   * Function sorts the children nodes by specified sorting key
   * @param nodes
   * @param jesFilter
   * @param sortKey
   * @return sorted nodes by specified key
   */
  private fun performJobsSorting(
    nodes: List<AbstractTreeNode<*>>,
    jesFilter: JesFilterNode,
    sortKey: SortQueryKeys
  ): List<AbstractTreeNode<*>> {
    val sortedNodesInternal: List<AbstractTreeNode<*>> =
      if (jesFilter.currentSortQueryKeysList.contains(SortQueryKeys.ASCENDING)) {
        nodes.sortedBy {
          selector(sortKey).invoke(it)
        }
      } else {
        nodes.sortedByDescending {
          selector(sortKey).invoke(it)
        }
      }
    return sortedNodesInternal.also { sortedNodes.clearAndMergeWith(it) }
  }

  /**
   * Selector which extracts the job info by specified sort key
   * @param key - sort key
   * @return String representation of the extracted job info attribute of the virtual file
   */
  private fun selector(key: SortQueryKeys): (AbstractTreeNode<*>) -> String? {
    return {
      val jobInfo =
        (DataOpsManager.getService().tryToGetAttributes((it as JobNode).value) as RemoteJobAttributes).jobInfo
      when (key) {
        SortQueryKeys.JOB_NAME -> jobInfo.jobName
        SortQueryKeys.JOB_OWNER -> jobInfo.owner
        SortQueryKeys.JOB_STATUS -> jobInfo.status?.value
        SortQueryKeys.JOB_ID -> jobInfo.jobId
        SortQueryKeys.JOB_CREATION_DATE -> jobInfo.execStarted
        SortQueryKeys.JOB_COMPLETION_DATE -> jobInfo.execEnded
        else -> null
      }
    }
  }
}
