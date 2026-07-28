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
import org.zowe.explorer.v3.tree.nodes.Navigable

/**
 * Action to open the selected navigable node in the editor.
 * Visible when a single node with a [Navigable] descriptor is selected
 */
class OpenFileAction : DumbAwareEDTAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedNode = ExplorerTreeComponentService.getService()
      .getActiveExplorerComponent(project)
      .selectedNodes
      .singleOrNull() ?: return
    if (selectedNode.canNavigate()) {
      selectedNode.navigate(true)
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    if (project == null) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selectedNodes = ExplorerTreeComponentService.getService()
      .getActiveExplorerComponent(project)
      .selectedNodes
    e.presentation.isEnabledAndVisible =
      selectedNodes.size == 1 && selectedNodes[0].nodeDescriptor is Navigable
  }
}