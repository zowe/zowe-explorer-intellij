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

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.common.ui.promisePath
import org.zowe.explorer.explorer.ui.*

class RefreshNodeAction : AnAction() {

  private fun cleanInvalidateOnExpand(
    node: ExplorerTreeNode<*>,
    view: ExplorerTreeView<*,*>
  ) {
    view.myStructure.promisePath(node, view.myTree).onSuccess {
      val lastNode = it.lastPathComponent
      if (view.myNodesToInvalidateOnExpand.contains(lastNode)) {
        synchronized(view.myNodesToInvalidateOnExpand) {
          view.myNodesToInvalidateOnExpand.remove(lastNode)
        }
      }
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: e.getData(JES_EXPLORER_VIEW)
    view ?: return

    val selected = view.mySelectedNodesData

    selected.parallelStream().forEach { data ->
      when (val node = data.node) {
        is FetchNode -> {
          cleanInvalidateOnExpand(node, view)
          node.cleanCache()
          val query = node.query ?: return@forEach
          view.getNodesByQueryAndInvalidate(query)
        }
        is WorkingSetNode<*> -> {
          node.cachedChildren.filterIsInstance<FetchNode>()
            .forEach {
              it.cleanCache()
              cleanInvalidateOnExpand(it, view)
            }
          view.myFsTreeStructure.findByValue(node.value).forEach {
            view.myStructure.invalidate(it, true)
          }
        }
      }
    }

  }

  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: e.getData(JES_EXPLORER_VIEW)

    view ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    e.presentation.isEnabledAndVisible = selected.any {
      it.node is RefreshableNode
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}
