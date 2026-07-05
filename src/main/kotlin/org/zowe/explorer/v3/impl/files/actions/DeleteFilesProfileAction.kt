/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import org.zowe.explorer.v3.actions.DumbAwareEDTAction
import org.zowe.explorer.v3.impl.files.tree.nodes.FilesProfileNodeDescriptor
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.explorer.v3.profiles.ProfileService
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService

/**
 * Action to delete an existing files profile.
 * Visible when a single [FilesProfileNodeDescriptor] node is selected.
 * Shows a confirmation dialog before performing the deletion
 */
class DeleteFilesProfileAction : DumbAwareEDTAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedNode = ExplorerTreeComponentService.getService()
      .getFilesExplorerComponent(project)
      .selectedNodes
      .singleOrNull() ?: return
    val descriptor = selectedNode.nodeDescriptor as? FilesProfileNodeDescriptor ?: return

    val result = Messages.showYesNoDialog(
      project,
      "Are you sure you want to delete files profile '${descriptor.displayName}'?",
      "Delete Files Profile",
      Messages.getWarningIcon()
    )
    if (result != Messages.YES) return

    try {
      val configType = ZoweConfigService.getService().getSelectedConfigType(project)
      ProfileService.getService().deleteProfile(configType, descriptor.displayName, project.basePath, project)
    } catch (ex: Exception) {
      Messages.showErrorDialog(project, ex.message ?: "Unknown error", "Failed to Delete Files Profile")
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    if (project == null) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selectedNodes = ExplorerTreeComponentService.getService()
      .getFilesExplorerComponent(project)
      .selectedNodes
    e.presentation.isEnabledAndVisible =
      selectedNodes.size == 1 && selectedNodes[0].nodeDescriptor is FilesProfileNodeDescriptor
  }
}