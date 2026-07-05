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
import com.intellij.openapi.ui.Messages
import org.zowe.explorer.v3.actions.DumbAwareEDTAction
import org.zowe.explorer.v3.impl.jes.dialogs.DeleteJobFilterHandler
import org.zowe.explorer.v3.impl.jes.tree.nodes.JesProfileNodeDescriptor
import org.zowe.explorer.v3.impl.jes.tree.nodes.JobFilterNodeDescriptor
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode

/**
 * Action to delete an existing JES job filter.
 * Visible when a single [JobFilterNodeDescriptor] node is selected.
 * Shows a confirmation dialog before performing the deletion
 */
class DeleteJobFilterAction : DumbAwareEDTAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedNode = ExplorerTreeComponentService.getService()
      .getJesExplorerComponent(project)
      .selectedNodes
      .singleOrNull() ?: return

    val filterDescriptor = selectedNode.nodeDescriptor as? JobFilterNodeDescriptor ?: return
    val parentNode = selectedNode.parent as? ExplorerTreeNode ?: return
    val parentDescriptor = parentNode.nodeDescriptor as? JesProfileNodeDescriptor ?: return

    val result = Messages.showYesNoDialog(
      project,
      "Are you sure you want to delete this job filter (${formatFilterDescription(filterDescriptor)})?",
      "Delete Job Filter",
      Messages.getWarningIcon()
    )
    if (result != Messages.YES) return

    try {
      val configService = ZoweConfigService.getService()
      val configType = configService.getSelectedConfigType(project)
      DeleteJobFilterHandler(project.basePath, project)
        .deleteEntry(
          configType,
          parentDescriptor.displayName,
          filterDescriptor.owner,
          filterDescriptor.prefix,
          filterDescriptor.jobId
        )
    } catch (ex: Exception) {
      Messages.showErrorDialog(project, ex.message ?: "Unknown error", "Failed to Delete Job Filter")
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    if (project == null) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selectedNodes = ExplorerTreeComponentService.getService()
      .getJesExplorerComponent(project)
      .selectedNodes
    e.presentation.isEnabledAndVisible =
      selectedNodes.size == 1 && selectedNodes[0].nodeDescriptor is JobFilterNodeDescriptor
  }

  /**
   * @return a human-readable description of the filter for the confirmation dialog
   */
  private fun formatFilterDescription(descriptor: JobFilterNodeDescriptor): String {
    return if (descriptor.jobId.isNotEmpty()) {
      "Job ID: ${descriptor.jobId}"
    } else {
      "Owner: ${descriptor.owner}, Prefix: ${descriptor.prefix}"
    }
  }
}