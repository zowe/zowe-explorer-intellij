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
import org.zowe.explorer.v3.impl.jes.dialogs.AddJobFilterDialog
import org.zowe.explorer.v3.impl.jes.tree.nodes.JesProfileNodeDescriptor
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService

/**
 * Action to add a job filter to an existing JES profile.
 * Visible only when a single [JesProfileNodeDescriptor] node is selected
 */
class AddJobFilterAction : JesProfileAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedNode = ExplorerTreeComponentService.getService()
      .getJesExplorerComponent(project)
      .selectedNodes
      .singleOrNull() ?: return
    val profileDescriptor = selectedNode.nodeDescriptor as? JesProfileNodeDescriptor ?: return
    val configType = ZoweConfigService.getService().getSelectedConfigType(project)
    AddJobFilterDialog(project, configType, profileDescriptor.displayName).show()
  }
}
