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
import org.zowe.explorer.v3.impl.jes.dialogs.EditJobFilterDialog
import org.zowe.explorer.v3.impl.jes.tree.nodes.JesProfileNodeDescriptor
import org.zowe.explorer.v3.impl.jes.tree.nodes.JobFilterNodeDescriptor
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode

/**
 * Action to edit an existing JES job filter.
 * Visible only when a single [JobFilterNodeDescriptor] node is selected
 */
class EditJobFilterAction : DumbAwareEDTAction() {

  override fun update(e: AnActionEvent) {
    val project = e.project ?: run {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selectedNodes = ExplorerTreeComponentService.getService()
      .getJesExplorerComponent(project)
      .selectedNodes
    e.presentation.isEnabledAndVisible =
      selectedNodes.size == 1 && selectedNodes[0].nodeDescriptor is JobFilterNodeDescriptor
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedNode = ExplorerTreeComponentService.getService()
      .getJesExplorerComponent(project)
      .selectedNodes
      .singleOrNull() ?: return
    val filterDescriptor = selectedNode.nodeDescriptor as? JobFilterNodeDescriptor ?: return
    val parentNode = selectedNode.parent as? ExplorerTreeNode ?: return
    val parentDescriptor = parentNode.nodeDescriptor as? JesProfileNodeDescriptor ?: return
    val configType = ZoweConfigService.getService().getSelectedConfigType(project)
    EditJobFilterDialog(
      project,
      configType,
      parentDescriptor.displayName,
      filterDescriptor.owner,
      filterDescriptor.prefix,
      filterDescriptor.jobId
    ).showAndGet()
  }
}