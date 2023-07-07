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
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.explorer.JesWorkingSet
import org.zowe.explorer.explorer.ui.*

/**
 * Action to manipulate jobs filter from UI
 * @author Uladzislau Kalesnikau
 */
abstract class JobsFilterAction : AnAction() {

  /**
   * Is node conforms to the JesFilterNode type
   * @param node the node to check
   */
  open fun isNodeConformsToType(node: ExplorerTreeNode<ConnectionConfig, *>?): Boolean {
    return node is JesFilterNode
  }

  /** Decides to show action or not */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val node = selected.getOrNull(0)?.node
    e.presentation.isVisible = selected.size == 1 && isNodeConformsToType(node)
  }

  /** Finds JES working set units for selected nodes in explorer */
  protected fun getUnits(view: JesExplorerView): List<JesWorkingSet> {
    return view.mySelectedNodesData.map { it.node }
      .filterIsInstance<ExplorerUnitTreeNodeBase<ConnectionConfig, *, JesWorkingSet>>()
      .map { it.unit }
      .distinct()
  }

}
