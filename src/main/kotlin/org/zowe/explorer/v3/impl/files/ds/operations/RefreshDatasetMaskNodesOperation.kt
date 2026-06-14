/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.ds.operations

import org.zowe.explorer.v3.newoperations.RefreshNodesOperation
import org.zowe.explorer.v3.tree.nodes.NodeSyncService

// TODO: doc
class RefreshDatasetMaskNodesOperation(
  override val operationData: RefreshDatasetMaskNodesOperationData
) : RefreshNodesOperation {
  override fun run(): RefreshDatasetMaskNodesOperationResult {
    NodeSyncService.getService().refreshNodesForPlainFilter(this)
    return RefreshDatasetMaskNodesOperationResult()
  }
}
