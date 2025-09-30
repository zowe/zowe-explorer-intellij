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
import org.zowe.explorer.v3.components.ExplorerTreeComponentService
import org.zowe.explorer.v3.tree.nodes.RefreshableNode

// TODO: doc
class RefreshNodeAction : DumbAwareEDTAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    ExplorerTreeComponentService.getService()
      .getFilesExplorerComponent(project)
      .selectedNodes
      .forEach {
        if (it is RefreshableNode) {
          it.refreshNode()
        }
      }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val explorerComponent = ExplorerTreeComponentService.getService()
      .getFilesExplorerComponent(project)
    e.presentation.isEnabledAndVisible = explorerComponent
      .selectedNodes
      .any { it is RefreshableNode }
  }
}