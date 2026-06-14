/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.jes.operations

import org.zowe.explorer.v3.newoperations.LoadNodesOperationData
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode

/**
 * Operation data for load job filter operation
 * @param node the node the operation is triggered against
 * @param path the path of the node for children nodes to be stored under
 * @param filter the job filter prefix to search by
 * @param owner the job owner to search by (optional)
 * @param jobId the job ID to search by (optional)
 */
data class LoadJobFilterNodesOperationData(
  override val node: ExplorerTreeNode,
  override val path: List<String>,
  override val filter: String,
  val owner: String = "",
  val jobId: String = ""
) : LoadNodesOperationData
