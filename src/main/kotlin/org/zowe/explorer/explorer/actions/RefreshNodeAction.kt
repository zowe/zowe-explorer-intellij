/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.common.ui.cleanInvalidateOnExpand
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.FetchNode
import org.zowe.explorer.explorer.ui.RefreshableNode
import org.zowe.explorer.explorer.ui.WorkingSetNode

/**
 * Class which represents a refresh node action
 */
class RefreshNodeAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Overloaded method of AnAction abstract class. Tells what to do if an action was submitted
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(EXPLORER_VIEW) ?: return

    val selected = view.mySelectedNodesData

    selected.parallelStream().forEach { data ->
      when (val node = data.node) {
        is FetchNode -> {
          cleanInvalidateOnExpand(node, view)
          node.cleanCache(cleanBatchedQuery = true)
          val query = node.query ?: return@forEach
          val nodes = view.getNodesByQuery(query)
          view.invalidateNodes(nodes)
        }

        is WorkingSetNode<*, *> -> {
          node.cachedChildren.filterIsInstance<FetchNode>()
            .forEach {
              it.cleanCache(cleanBatchedQuery = true)
              cleanInvalidateOnExpand(it, view)
            }
          view.myFsTreeStructure.findByValue(node.value).forEach {
            view.myStructure.invalidate(it, true)
          }
        }
      }
    }

  }

  /**
   * Method determines if an action is visible for particular virtual file in VFS
   */
  override fun update(e: AnActionEvent) {
    val view = e.getData(EXPLORER_VIEW)

    view ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    e.presentation.isEnabledAndVisible = selected.any {
      it.node is RefreshableNode
    }
  }

  /**
   * Determines if an action is dumb aware
   */
  override fun isDumbAware(): Boolean {
    return true
  }
}
