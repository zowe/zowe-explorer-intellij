/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.tree.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import javax.swing.Icon
import kotlin.collections.fold

// TODO: doc
open class ExplorerTreeNodeDescriptor(
  var displayName: String = "",
  var tooltip: String = "",
  var icon: Icon? = null,
  var isLeaf: Boolean = true,
  var hasExpandChevron: Boolean = false
) {
  private val associatedNodes: MutableList<ExplorerTreeNode> = mutableListOf()

  protected open val genuinePresentationData = PresentationData()
    .also {
      it.setIcon(icon)
      it.addText(displayName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
      it.tooltip = tooltip
    }

  private val busyNodePresentationData = PresentationData()
    .also {
      it.setIcon(AnimatedIcon.Default())
      it.addText(displayName, SimpleTextAttributes.GRAYED_ATTRIBUTES)
      it.tooltip = "Node is busy with some action..."
    }

  var isBusy: Boolean = false

  fun updateNode(presentationData: PresentationData) {
    presentationData.copyFrom(if (isBusy) busyNodePresentationData else genuinePresentationData)
  }

  open fun getNodeChildren(node: ExplorerTreeNode): List<ExplorerTreeNode> {
    return listOf()
  }

  fun setNodeIcon(icon: Icon?) {
    this.icon = icon
    genuinePresentationData.setIcon(icon)
  }

  fun associateNode(node: ExplorerTreeNode) {
    associatedNodes.add(node)
  }

  fun invalidateAssociatedNodes() {
    associatedNodes
      .fold(mutableMapOf<Project, MutableList<ExplorerTreeNode>>()) { projectsToNodes, node ->
        projectsToNodes.getOrPut(node.project) { mutableListOf() }.add(node)
        projectsToNodes
      }
      .forEach { (project, nodes) ->
        val filesExplorerComponent = ExplorerTreeComponentService.getService()
          .getFilesExplorerComponent(project)
        nodes.forEach { node ->
          filesExplorerComponent.invalidateNode(node, true)
        }
      }
  }

  fun invalidateAssociatedParents() {
    associatedNodes
      .mapNotNull { it.parent }
      .distinct()
      .filterIsInstance<ExplorerTreeNode>()
      .fold(mutableMapOf<Project, MutableList<ExplorerTreeNode>>()) { projectsToNodes, node ->
        projectsToNodes.getOrPut(node.project) { mutableListOf() }.add(node)
        projectsToNodes
      }
      .forEach { (project, nodes) ->
        val filesExplorerComponent = ExplorerTreeComponentService.getService()
          .getFilesExplorerComponent(project)
        nodes.forEach { node ->
          filesExplorerComponent.invalidateNode(node, true)
        }
      }
  }
}
