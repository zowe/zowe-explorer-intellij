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

package org.zowe.explorer.explorer.actions.sort.datasets

import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.dataops.BatchedRemoteQuery
import org.zowe.explorer.dataops.sort.SortQueryKeys
import org.zowe.explorer.explorer.actions.sort.SortAction
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.utils.clearAndMergeWith
import org.zowe.explorer.utils.clearOldKeysAndAddNew

class DatasetsSortAction : SortAction<DSMaskNode>() {
  override fun getSourceView(e: AnActionEvent): FileExplorerView? {
    return e.getExplorerView()
  }

  override fun getSourceNode(view: ExplorerTreeView<*, *, *>): DSMaskNode? {
    return view.mySelectedNodesData[0].node.castOrNull()
  }

  override fun shouldEnableSortKeyForNode(selectedNode: DSMaskNode, sortKey: SortQueryKeys): Boolean {
    return selectedNode.currentSortQueryKeysList.contains(sortKey)
  }

  override fun performQueryUpdateForNode(selectedNode: DSMaskNode, sortKey: SortQueryKeys) {
    val queryToUpdate = selectedNode.query as BatchedRemoteQuery
    selectedNode.currentSortQueryKeysList.clearOldKeysAndAddNew(sortKey)
    queryToUpdate.sortKeys.clearAndMergeWith(selectedNode.currentSortQueryKeysList)
  }
}
