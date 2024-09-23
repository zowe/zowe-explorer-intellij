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

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.common.ui.cleanInvalidateOnExpand
import eu.ibagroup.formainframe.dataops.content.synchronizer.checkFileForSync
import eu.ibagroup.formainframe.explorer.ui.*

/**
 * Class which represents a refresh node action
 */
class RefreshNodeAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Check if the file related with the node or files related with child nodes are currently synchronized
   * @return true if synchronization is not running anf false otherwise
   */
  private fun checkNodeForSync(project: Project?, node: ExplorerTreeNode<*, *>): Boolean {
    val file = node.virtualFile
    return if (file != null) {
      !checkFileForSync(project, file, checkDependentFiles = true)
    } else if (node is DSMaskNode) {
      node.children.none {
        val vFile = it.value as? VirtualFile
        vFile != null && checkFileForSync(project, vFile, checkDependentFiles = true)
      }
    } else true
  }

  /**
   * Filter out nodes data that cannot be refreshed due to synchronization
   */
  private fun filterNodesData(project: Project?, nodesData: List<NodeData<*>>): List<NodeData<*>> {
    return nodesData.filter { data ->
      when (val node = data.node) {
        is FetchNode -> {
          checkNodeForSync(project, node)
        }

        is WorkingSetNode<*, *> -> {
          node.cachedChildren.filterIsInstance<FetchNode>().none {
            !checkNodeForSync(project, it)
          }
        }

        else -> true
      }
    }
  }

  /**
   * Overloaded method of AnAction abstract class. Tells what to do if an action was submitted
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(EXPLORER_VIEW) ?: return

    val filteredNodesData = filterNodesData(e.project, view.mySelectedNodesData)

    filteredNodesData.parallelStream().forEach { data ->
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
