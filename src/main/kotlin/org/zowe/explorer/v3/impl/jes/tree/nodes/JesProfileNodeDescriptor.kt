/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.jes.tree.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.state.config.jes.JesWorkingSetConfig
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.NoItemsFoundNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.ProfileNodeDescriptor

/**
 * Node descriptor for a `jes_ij` profile from the Zowe Team Config.
 * Represents a top-level profile entry in the JES Explorer tree.
 * Displays gray "(no name)" when the profile key is an empty string
 */
class JesProfileNodeDescriptor(
  displayName: String,
  config: JesWorkingSetConfig?,
  private val connectionProfile: String? = null
) : ProfileNodeDescriptor(displayName, "JES Profile", config), JesExplorerRelated {

  override val genuinePresentationData = PresentationData().also {
    it.setIcon(ZoweExplorerIcons.profileIcon)
    if (displayName.isEmpty()) {
      it.addText("(no name)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    } else {
      it.addText(displayName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }
    if (!connectionProfile.isNullOrBlank()) {
      it.addText("  $connectionProfile", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES)
    }
    it.tooltip = "JES Profile"
  }

  override fun getNodeChildren(node: ExplorerTreeNode): List<ExplorerTreeNode> {
    return listOf(ExplorerTreeNode(NoItemsFoundNodeDescriptor(), node.project, node))
  }
}