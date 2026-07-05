/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.jes.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.v3.actions.DumbAwareEDTAction
import org.zowe.explorer.v3.impl.jes.tree.nodes.JesProfileNodeDescriptor
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService

abstract class JesProfileAction : DumbAwareEDTAction() {
  override fun update(e: AnActionEvent) {
    val project = e.project ?: run {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selectedNodes = ExplorerTreeComponentService.getService()
      .getJesExplorerComponent(project)
      .selectedNodes
    e.presentation.isEnabledAndVisible =
      selectedNodes.size == 1 && selectedNodes[0].nodeDescriptor is JesProfileNodeDescriptor
  }
}
