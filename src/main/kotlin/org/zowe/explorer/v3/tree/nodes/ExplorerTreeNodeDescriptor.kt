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
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.v3.impl.splitToColoredParts
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import javax.swing.Icon

/**
 * Base class of the tree node descriptors. Node descriptors contain all the necessary data and functionalities
 * to initialize and work with explorer tree nodes
 * @property displayName the name of the node descriptor
 * @property tooltip the tooltip of the node descriptor
 * @property icon the icon associated with the node descriptor
 * @property isLeaf if "true", then the node does not carry any children
 * @property hasExpandChevron if "true", then the node should show the expand chevron (means it has children)
 */
open class ExplorerTreeNodeDescriptor(
  var displayName: String = "",
  var tooltip: String = "",
  icon: Icon? = null,
  var isLeaf: Boolean = true,
  var hasExpandChevron: Boolean = false
) {
  /**
   * A mutable list of [ExplorerTreeNode]'s, associated with the descriptor.
   * Is used to operate on nodes representation from a single endpoint
   */
  val associatedNodes: MutableList<ExplorerTreeNode> = mutableListOf()

  /**
   * A genuine presentation data of the node descriptor.
   * Identifies the data to associate with the node as the stable representation in normal conditions
   */
  protected open val genuinePresentationData = PresentationData()
    .also { presentationData ->
      presentationData.setIcon(icon)
      splitToColoredParts(displayName)
        .forEach {
          presentationData.addText(it)
        }
      presentationData.tooltip = tooltip
    }

  /** Set the node descriptor icon, updating the [genuinePresentationData]'s icon */
  var icon: Icon? = icon
    set(value) {
      field = value
      genuinePresentationData.setIcon(value)
    }

  /**
   * A busy node presentation data.
   * Identifies the data to show on the associated nodes when the node descriptor is marked as busy
   */
  private val busyNodePresentationData = PresentationData()
    .also {
      it.setIcon(AnimatedIcon.Default())
      it.addText(displayName, SimpleTextAttributes.GRAYED_ATTRIBUTES)
      it.tooltip = "Node is busy with some action..."
    }

  /** An indicator of the node descriptor to show the respective data on each of the associated nodes */
  var isBusy: Boolean = false

  /**
   * Update the node descriptor presentation.
   * Puts the respective node presentation data depending on whether the node descriptor is busy or not
   * @param presentationData the presentation data of the node descriptor to update
   */
  fun updatePresentation(presentationData: PresentationData) {
    presentationData.copyFrom(if (isBusy) busyNodePresentationData else genuinePresentationData)
  }

  /**
   * Get children nodes of the descriptor. For leaf nodes, it returns an empty list.
   * For nodes that will expand, the respective node descriptor class should define its own functionality
   * @param node the node to get children for
   * @return the respective list of [ExplorerTreeNode]'s
   */
  open fun getNodeChildren(node: ExplorerTreeNode): List<ExplorerTreeNode> {
    return listOf()
  }

  /** Associate the [node] with the node descriptor */
  fun associateNode(node: ExplorerTreeNode) {
    associatedNodes.add(node)
  }

  /** Trigger [ExplorerTreeNode]'s invalidation on the nodes, associated with this descriptor */
  open fun invalidateAssociatedNodes() {
    associatedNodes
      .forEach { node ->
        ExplorerTreeComponentService.getService()
          .invalidateNode(node)
      }
  }
}
