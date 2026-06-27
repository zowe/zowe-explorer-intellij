/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.vault.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.v3.actions.DumbAwareEDTAction
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.explorer.v3.impl.vault.dialogs.CredentialsDialog
import org.zowe.explorer.v3.impl.vault.tree.nodes.SecureEntryNodeDescriptor
import org.zowe.explorer.v3.impl.vault.tree.nodes.SecureProfileNodeDescriptor
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode

/**
 * Context menu action that opens [CredentialsDialog] in edit mode for the selected
 * secure entry node. Visible only when a [SecureEntryNodeDescriptor] is selected,
 * and reads the parent profile path from the parent [SecureProfileNodeDescriptor]
 */
class EditCredentialsAction : DumbAwareEDTAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val (profilePath, fieldName) = getSelectedEntry(e) ?: return
    val configType = ZoweConfigService.getService().getSelectedConfigType(project)
    CredentialsDialog(project, configType, profilePath, editFieldName = fieldName).show()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.text = "Edit"
    e.presentation.icon = AllIcons.Actions.Edit
    e.presentation.isEnabledAndVisible = getSelectedEntry(e) != null
  }

  private fun getSelectedEntry(e: AnActionEvent): Pair<String, String>? {
    val project = e.project ?: return null
    val selectedNode = ExplorerTreeComponentService.getService()
      .getActiveExplorerComponent(project)
      .selectedNodes
      .firstOrNull { it.nodeDescriptor is SecureEntryNodeDescriptor }
      ?: return null
    val parentNode = selectedNode.parent as? ExplorerTreeNode ?: return null
    if (parentNode.nodeDescriptor !is SecureProfileNodeDescriptor) return null
    val profilePath = parentNode.nodeDescriptor.displayName
    val fieldName = selectedNode.nodeDescriptor.displayName
    return profilePath to fieldName
  }
}
