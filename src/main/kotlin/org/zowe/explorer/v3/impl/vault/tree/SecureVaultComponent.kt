/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.vault.tree

import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.tree.ExplorerTreeComponent

/**
 * Secure Vault component for viewing credentials stored in the Zowe Team Config.
 * Displays profiles that have non-empty `secure` arrays and their credential entries.
 * Automatically syncs with config file edits and programmatic writes
 */
class SecureVaultComponent(project: Project) : ExplorerTreeComponent(project) {
  companion object {
    const val SECURE_VAULT_COMPONENT_NAME = "Secure Vault"
  }

  override val explorerName = SECURE_VAULT_COMPONENT_NAME
  override val explorerTreeStructure = SecureVaultTreeStructure(project)
  override val explorerTreeView = SecureVaultTreeView(explorerName, explorerAsyncTreeModel)
}
