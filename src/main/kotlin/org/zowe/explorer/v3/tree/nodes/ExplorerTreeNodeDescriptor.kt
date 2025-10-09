/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.tree.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import javax.swing.Icon

// TODO: doc
open class ExplorerTreeNodeDescriptor(
  var displayName: String = "",
  var tooltip: String = "",
  var icon: Icon? = null,
  var isLeaf: Boolean = true,
  var isExpanded: Boolean = false
) {
  open fun updateNode(presentationData: PresentationData) {
    presentationData.addText(displayName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    if (tooltip.isNotEmpty()) {
      presentationData.tooltip = tooltip
    }
  }

  open fun getNodeChildren(node: ExplorerTreeNode): List<ExplorerTreeNode> {
    return listOf()
  }

  fun setNodeIcon(node: ExplorerTreeNode, icon: Icon?) {
    this.icon = icon
    node.presentation.setIcon(icon)
  }
}
