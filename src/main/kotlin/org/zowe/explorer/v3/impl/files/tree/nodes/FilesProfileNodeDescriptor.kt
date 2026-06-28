/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.tree.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.impl.files.ds.tree.nodes.DatasetMaskNodeDescriptor
import org.zowe.explorer.v3.impl.files.uss.tree.nodes.UssFilterNodeDescriptor
import org.zowe.explorer.v3.state.config.files.FilesWorkingSetConfig
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.NoItemsFoundNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.ProfileNodeDescriptor

/**
 * Node descriptor for a `files_ij` profile from the Zowe Team Config.
 * Represents a top-level profile entry in the Files Explorer tree.
 * Displays gray "(no name)" when the profile key is an empty string.
 * Children are [DatasetMaskNodeDescriptor] and [UssFilterNodeDescriptor] nodes
 * read from the profile's `dsMasks` and `ussFilters` properties
 */
class FilesProfileNodeDescriptor(
  displayName: String,
  config: FilesWorkingSetConfig?,
  private val connectionProfile: String? = null,
  private val dsMasks: List<String> = emptyList(),
  private val ussFilters: List<String> = emptyList()
) : ProfileNodeDescriptor(displayName, "Files Profile", config), FilesExplorerRelated {

  override val genuinePresentationData = PresentationData().also {
    it.setIcon(ZoweExplorerIcons.profileIcon)
    if (displayName.isEmpty()) {
      it.addText("(no name)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    } else {
      it.addText(displayName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }
    if (!connectionProfile.isNullOrBlank()) {
      it.addText("  $connectionProfile", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES)
    } else {
      it.addText("  *no connection profile*", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
    }
    it.tooltip = "Files Profile"
  }

  override fun getNodeChildren(node: ExplorerTreeNode): List<ExplorerTreeNode> {
    val resolvedConnectionProfile = connectionProfile ?: return listOf(
      ExplorerTreeNode(NoItemsFoundNodeDescriptor(), node.project, node)
    )

    val children = mutableListOf<ExplorerTreeNode>()
    dsMasks.forEach { mask ->
      children.add(ExplorerTreeNode(DatasetMaskNodeDescriptor(mask, resolvedConnectionProfile), node.project, node))
    }
    ussFilters.forEach { path ->
      children.add(ExplorerTreeNode(UssFilterNodeDescriptor(path, resolvedConnectionProfile), node.project, node))
    }

    return children.ifEmpty {
      listOf(ExplorerTreeNode(NoItemsFoundNodeDescriptor(), node.project, node))
    }
  }
}