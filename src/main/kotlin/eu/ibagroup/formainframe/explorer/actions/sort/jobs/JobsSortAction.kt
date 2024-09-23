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

package eu.ibagroup.formainframe.explorer.actions.sort.jobs

import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.sort.SortQueryKeys
import eu.ibagroup.formainframe.explorer.actions.sort.SortAction
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeView
import eu.ibagroup.formainframe.explorer.ui.JesExplorerView
import eu.ibagroup.formainframe.explorer.ui.JesFilterNode
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.clearAndMergeWith
import eu.ibagroup.formainframe.utils.clearOldKeysAndAddNew

class JobsSortAction : SortAction<JesFilterNode>() {
  override fun getSourceView(e: AnActionEvent): JesExplorerView? {
    return e.getExplorerView()
  }

  override fun getSourceNode(view: ExplorerTreeView<*, *, *>): JesFilterNode? {
    return view.mySelectedNodesData[0].node.castOrNull()
  }

  override fun shouldEnableSortKeyForNode(selectedNode: JesFilterNode, sortKey: SortQueryKeys): Boolean {
    return selectedNode.currentSortQueryKeysList.contains(sortKey)
  }

  override fun performQueryUpdateForNode(selectedNode: JesFilterNode, sortKey: SortQueryKeys) {
    val queryToUpdate = selectedNode.query as UnitRemoteQueryImpl
    selectedNode.currentSortQueryKeysList.clearOldKeysAndAddNew(sortKey)
    queryToUpdate.sortKeys.clearAndMergeWith(selectedNode.currentSortQueryKeysList)
  }

}
