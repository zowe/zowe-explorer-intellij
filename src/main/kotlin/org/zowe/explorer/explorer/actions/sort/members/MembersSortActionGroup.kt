/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.actions.sort.members

import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.explorer.actions.sort.SortActionGroup
import org.zowe.explorer.explorer.ui.*

/**
 * Represents the custom members sort action group in the FileExplorerView context menu
 */
class MembersSortActionGroup : SortActionGroup() {
  override fun getSourceView(e: AnActionEvent): FileExplorerView? {
    return e.getExplorerView()
  }

  override fun checkNode(node: ExplorerTreeNode<*, *>): Boolean {
    return node is LibraryNode
  }

}
