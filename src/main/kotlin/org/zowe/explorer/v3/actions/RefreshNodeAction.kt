/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import org.zowe.explorer.v3.tree.nodes.Refreshable

/**
 * Refresh a [Refreshable] node action.
 * Will refresh all selected nodes if they could be refreshed and are ready for refresh,
 * meaning there are no children or parent elements that are busy with some other operation
 */
class RefreshNodeAction : DumbAwareEDTAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    ExplorerTreeComponentService.getService()
      .getActiveExplorerComponent(project)
      .selectedNodes
      .forEach {
        if (
          it.nodeDescriptor is Refreshable
          && ((it.nodeDescriptor as? Refreshable)?.isNodeReadyForRefresh(it) ?: false)
        ) {
          (it.nodeDescriptor as? Refreshable)?.refreshNode(it)
        }
      }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val explorerComponent = ExplorerTreeComponentService.getService()
      .getActiveExplorerComponent(project)
    e.presentation.isEnabledAndVisible = explorerComponent
      .selectedNodes
      .any { it.nodeDescriptor is Refreshable }
    e.presentation.isVisible = explorerComponent
      .selectedNodes
      .any {
        it.nodeDescriptor is Refreshable
          && ((it.nodeDescriptor as? Refreshable)?.isNodeReadyForRefresh(it) ?: false)
      }
  }
}
