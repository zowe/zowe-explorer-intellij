/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import org.zowe.explorer.v3.tree.nodes.Renameable

// TODO: doc
class RenameNodeAction : DumbAwareEDTAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedNodes = ExplorerTreeComponentService.getService()
      .getFilesExplorerComponent(project)
      .selectedNodes
    if (selectedNodes.size == 1) {
      (selectedNodes[0].nodeDescriptor as? Renameable)?.renameNode()
    }
    // TODO: process e.dataContext as well???
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val explorerComponent = ExplorerTreeComponentService.getService()
      .getFilesExplorerComponent(project)
    val selectedNodes = explorerComponent.selectedNodes
    if (selectedNodes.size == 1) {
      e.presentation.isEnabledAndVisible = (selectedNodes[0].nodeDescriptor as? Renameable) != null
    }
    // TODO: process e.dataContext as well???
  }
}