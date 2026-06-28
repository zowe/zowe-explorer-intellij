/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.jes.tree.nodes

import org.zowe.explorer.v3.impl.formJesBasePath
import org.zowe.explorer.v3.impl.formJobFilterName
import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.impl.jes.operations.LoadJobFilterNodesOperation
import org.zowe.explorer.v3.impl.jes.operations.LoadJobFilterNodesOperationData
import org.zowe.explorer.v3.impl.jes.operations.RefreshJobFilterNodesOperation
import org.zowe.explorer.v3.impl.jes.operations.RefreshJobFilterNodesOperationData
import org.zowe.explorer.v3.newoperations.RefreshNodesOperation
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.PlainFilterNodeDescriptor

/**
 * JES job filter node descriptor. Carries information about the job filter node
 * @param prefix the job prefix to search jobs by
 * @param owner the job owner to search jobs by
 * @param jobId the job ID to search a job by (is mutually exclusive with job prefix + job owner)
 * @param connectionProfile the connection profile path from the Zowe Team Config
 */
class JobFilterNodeDescriptor(
  prefix: String,
  private val owner: String,
  private val jobId: String,
  connectionProfile: String
) : PlainFilterNodeDescriptor(
  formJobFilterName(prefix, owner, jobId),
  basePath = formJesBasePath(connectionProfile),
  "JES jobs filter",
  ZoweExplorerIcons.jobsFilter,
  connectionProfile
), JesExplorerRelated {
  override val fetchFilter = prefix

  override fun generateLoadNodesOperation(node: ExplorerTreeNode): LoadJobFilterNodesOperation {
    return LoadJobFilterNodesOperation(
      LoadJobFilterNodesOperationData(node, basePath, fetchFilter, owner, jobId)
    )
  }

  override fun generateRefreshNodesOperation(node: ExplorerTreeNode): RefreshNodesOperation {
    return RefreshJobFilterNodesOperation(
      RefreshJobFilterNodesOperationData(node, basePath, formJobFilterName(fetchFilter, owner, jobId))
    )
  }

  /**
   * Check job name and job ID matches with searching pattern.
   * If the job ID for the jobs filter is provided, it is checked as is without considering any other filter parameters.
   * Otherwise, the job name + job ID is checked to match the provided job prefix
   * @param elemName the job name + job ID to check (e.g. "TESTJOB(TESTID)")
   * @return true if the combination matches the filter, false otherwise
   */
  override fun checkMatchesFilter(elemName: String): Boolean {
    val regexPattern = if (jobId != "") "^.*(${jobId})$"
      else "^${fetchFilter.replace("*", ".*").replace("%", ".")}(.*)$"
    return Regex(regexPattern, RegexOption.IGNORE_CASE).matches(elemName)
  }
}
