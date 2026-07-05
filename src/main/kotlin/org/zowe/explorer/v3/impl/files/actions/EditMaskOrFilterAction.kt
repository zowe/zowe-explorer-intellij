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
import org.zowe.explorer.v3.actions.DumbAwareEDTAction
import org.zowe.explorer.v3.impl.files.dialogs.AddMaskOrFilterDialog
import org.zowe.explorer.v3.impl.files.dialogs.EditMaskOrFilterDialog
import org.zowe.explorer.v3.impl.files.dialogs.EntryType
import org.zowe.explorer.v3.impl.files.ds.tree.nodes.DatasetMaskNodeDescriptor
import org.zowe.explorer.v3.impl.files.tree.nodes.FilesProfileNodeDescriptor
import org.zowe.explorer.v3.impl.files.uss.tree.nodes.UssFilterNodeDescriptor
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode

/**
 * Action to edit an existing dataset mask or USS filter.
 * Visible when a [DatasetMaskNodeDescriptor] or [UssFilterNodeDescriptor] node is selected
 */
class EditMaskOrFilterAction : DumbAwareEDTAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedNode = ExplorerTreeComponentService.getService()
      .getFilesExplorerComponent(project)
      .selectedNodes
      .singleOrNull() ?: return

    val (entryType, currentValue) = resolveEntryInfo(selectedNode) ?: return
    val profileName = resolveProfileName(selectedNode) ?: return
    val configType = ZoweConfigService.getService().getSelectedConfigType(project)

    EditMaskOrFilterDialog(project, configType, profileName, entryType, currentValue).show()
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
      selectedNodes.size == 1 && resolveEntryInfo(selectedNodes[0]) != null
  }

  /**
   * @return the entry type and current value for the selected node, or null if not a mask/filter node
   */
  private fun resolveEntryInfo(node: ExplorerTreeNode): Pair<EntryType, String>? {
    return when (val descriptor = node.nodeDescriptor) {
      is DatasetMaskNodeDescriptor -> EntryType.DS_MASK to descriptor.displayName
      is UssFilterNodeDescriptor -> EntryType.USS_FILTER to descriptor.displayName
      else -> null
    }
  }

  /**
   * @return the profile name from the parent [FilesProfileNodeDescriptor], or null if not found
   */
  private fun resolveProfileName(node: ExplorerTreeNode): String? {
    val parentDescriptor = (node.parent as? ExplorerTreeNode)?.nodeDescriptor as? FilesProfileNodeDescriptor
    return parentDescriptor?.displayName
  }
}