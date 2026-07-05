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

import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.explorer.v3.impl.vault.dialogs.CredentialsDialog
import org.zowe.explorer.v3.impl.vault.tree.nodes.SecureSetNodeDescriptor

/**
 * Context menu action that opens [CredentialsDialog] in create mode for the selected
 * secure profile node. Visible only when a [SecureSetNodeDescriptor] is selected.
 * Disabled with a tooltip when all [known secure fields][CredentialsDialog.KNOWN_SECURE_FIELDS]
 * are already present in the profile
 */
class CreateCredentialsAction : CredentialsAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val descriptor = getSelectedProfileDescriptor(e) ?: return
    val configType = ZoweConfigService.getService().getSelectedConfigType(project)
    CredentialsDialog(project, configType, descriptor.displayName).show()
  }

  override fun update(e: AnActionEvent) {
    val descriptor = getSelectedProfileDescriptor(e)
    val allPresent = descriptor?.hasAllKnownFields() == true
    e.presentation.text = "Create Credential"
    e.presentation.icon = ZoweExplorerIcons.keyIcon
    e.presentation.isVisible = descriptor != null
    e.presentation.isEnabled = descriptor != null && !allPresent
  }

  private fun SecureSetNodeDescriptor.hasAllKnownFields(): Boolean {
    return CredentialsDialog.KNOWN_SECURE_FIELDS.all { it in secureFields }
  }

}
