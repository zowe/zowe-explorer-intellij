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
import org.zowe.explorer.v3.impl.formJesFilterBasePath
import org.zowe.explorer.v3.impl.formJobFilterKey
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.NoItemsFoundNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.NodeSyncService
import org.zowe.explorer.v3.tree.nodes.ProfileNodeDescriptor

/**
 * Node descriptor for a `jes_ij` profile from the Zowe Team Config.
 * Represents a top-level profile entry in the JES Explorer tree.
 * Displays gray "(no name)" when the profile key is an empty string.
 * Children are [JobFilterNodeDescriptor] nodes read from the profile's `jobFilters`
 * property, each identified within the profile by a unique `owner.prefix(id)` key
 */
class JesProfileNodeDescriptor(
  displayName: String,
  private val connectionProfile: String? = null,
  private val connectionProfileType: String? = null,
  private val jobFilters: List<ZoweConfigService.JobFilter> = emptyList()
) : ProfileNodeDescriptor(displayName, "JES Profile"), JesExplorerRelated {

  override val genuinePresentationData = PresentationData().also {
    it.setIcon(ZoweExplorerIcons.profileIcon)
    if (displayName.isEmpty()) {
      it.addText("(no name)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    } else {
      it.addText(displayName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }
    if (!connectionProfile.isNullOrBlank()) {
      val typeLabel = if (!connectionProfileType.isNullOrBlank()) " ($connectionProfileType)" else ""
      it.addText("  $connectionProfile$typeLabel", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES)
    } else {
      it.addText("  *no connection profile*", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
    }
    it.tooltip = "JES Profile"
  }

  override fun getNodeChildren(node: ExplorerTreeNode): List<ExplorerTreeNode> {
    val resolvedConnectionProfile = connectionProfile ?: return listOf(
      ExplorerTreeNode(NoItemsFoundNodeDescriptor(), node.project, node)
    )

    val nodeSyncService = NodeSyncService.getService()
    val children = mutableListOf<ExplorerTreeNode>()
    jobFilters.forEach { filter ->
      val filterKey = formJobFilterKey(filter.owner, filter.prefix, filter.id)
      val filterBasePath = formJesFilterBasePath(resolvedConnectionProfile, filter.owner, filter.prefix, filter.id)
      val descriptor = nodeSyncService
        .getOrPutFilterNodeDescriptor(filterBasePath, filterKey) {
          JobFilterNodeDescriptor(filter.prefix, filter.owner, filter.id, resolvedConnectionProfile)
        }
      children.add(ExplorerTreeNode(descriptor, node.project, node))
    }

    return children.ifEmpty {
      listOf(ExplorerTreeNode(NoItemsFoundNodeDescriptor(), node.project, node))
    }
  }
}