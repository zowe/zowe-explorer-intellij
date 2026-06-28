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
import com.intellij.openapi.ui.Messages
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService

/**
 * Context menu action that deletes a secure credential entry from the profile.
 * Shows a confirmation dialog before removing the field from both the `secure` array
 * in the config file and the OS secure store.
 * Visible only when a [SecureEntryNodeDescriptor] is selected
 */
class DeleteCredentialAction : CredentialsAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val (profilePath, fieldName) = getSelectedEntry(e) ?: return

    val result = Messages.showYesNoDialog(
      project,
      "Are you sure you want to delete '$fieldName' secure credential?",
      "Delete Credential",
      Messages.getWarningIcon()
    )
    if (result != Messages.YES) return

    val configService = ZoweConfigService.getService()
    val configType = configService.getSelectedConfigType(project)
    configService.removeSecureFields(configType, project.basePath, project, profilePath, listOf(fieldName))
  }

  override fun update(e: AnActionEvent) {
    e.presentation.text = "Delete"
    e.presentation.icon = AllIcons.Actions.GC
    e.presentation.isEnabledAndVisible = getSelectedEntry(e) != null
  }

}
