/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.vault.tree.nodes

import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNodeDescriptor

// TODO: doc
class SecureSetNodeDescriptor(
  profilePath: String,
  secureFields: List<String>
) : ExplorerTreeNodeDescriptor(
  profilePath,
  profileTooltip(profilePath, secureFields),
  ZoweExplorerIcons.keyChainIcon,
  isLeaf = secureFields.isEmpty(),
  hasExpandChevron = secureFields.isNotEmpty()
), SecureVaultRelated {

  companion object {
    private fun profileTooltip(profilePath: String, secureFields: List<String>): String {
      return if (secureFields.isEmpty()) {
        "Storing profile (without credentials): $profilePath"
      } else {
        "Storing profile: $profilePath"
      }
    }
  }

  var secureFields = secureFields
    private set

  /**
   * Updates the secure fields list in place, preserving node identity
   * so the tree keeps its expansion state
   */
  fun updateSecureFields(newFields: List<String>) {
    secureFields = newFields
    isLeaf = newFields.isEmpty()
    hasExpandChevron = newFields.isNotEmpty()
    tooltip = profileTooltip(displayName, newFields)
  }

  override fun getNodeChildren(node: ExplorerTreeNode): List<ExplorerTreeNode> {
    return secureFields.map { field ->
      ExplorerTreeNode(SecureEntryNodeDescriptor(field), node.project, node)
    }
  }
}
