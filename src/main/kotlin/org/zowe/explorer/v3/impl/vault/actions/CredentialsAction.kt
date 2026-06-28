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
import org.zowe.explorer.v3.actions.DumbAwareEDTAction
import org.zowe.explorer.v3.impl.vault.tree.nodes.SecureEntryNodeDescriptor
import org.zowe.explorer.v3.impl.vault.tree.nodes.SecureProfileNodeDescriptor
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode

/**
 * Base class for credential-related context menu actions.
 * Provides shared helpers to resolve the selected [SecureProfileNodeDescriptor]
 * or a profile-path / field-name pair from the explorer tree
 */
abstract class CredentialsAction : DumbAwareEDTAction() {

  /**
   * Resolve the [SecureProfileNodeDescriptor] for the currently selected tree node.
   * Works whether the user selected a profile node directly or one of its entry children
   * @param e the action event providing the project and selection context
   * @return the descriptor, or `null` if the selection does not point to a secure profile
   */
  protected fun getSelectedProfileDescriptor(e: AnActionEvent): SecureProfileNodeDescriptor? {
    val project = e.project ?: return null
    val selectedNode = ExplorerTreeComponentService.getService()
      .getActiveExplorerComponent(project)
      .selectedNodes
      .firstOrNull() ?: return null
    if (selectedNode.nodeDescriptor is SecureProfileNodeDescriptor) {
      return selectedNode.nodeDescriptor as SecureProfileNodeDescriptor
    }
    if (selectedNode.nodeDescriptor is SecureEntryNodeDescriptor) {
      return (selectedNode.parent as? ExplorerTreeNode)
        ?.nodeDescriptor as? SecureProfileNodeDescriptor
    }
    return null
  }

  /**
   * Resolve the profile path and field name for the currently selected [SecureEntryNodeDescriptor].
   * Returns `null` when the selection is not a secure entry node
   * @param e the action event providing the project and selection context
   * @return a pair of (profilePath, fieldName), or `null`
   */
  protected fun getSelectedEntry(e: AnActionEvent): Pair<String, String>? {
    val project = e.project ?: return null
    val fieldName = ExplorerTreeComponentService.getService()
      .getActiveExplorerComponent(project)
      .selectedNodes
      .firstOrNull { it.nodeDescriptor is SecureEntryNodeDescriptor }
      ?.nodeDescriptor?.displayName ?: return null
    val profilePath = getSelectedProfileDescriptor(e)?.displayName ?: return null
    return profilePath to fieldName
  }
}
